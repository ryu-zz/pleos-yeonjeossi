package com.example.test.yeonsu.polling

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener

/**
 * Pleos Vehicle SDK getter 를 주기 조회하는 실제 폴링 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 대상 getter(상태 변경 리스너가 없어 폴링으로만 판정 가능):
 * - 송풍 방향: HVAC.getHvacFanDirection(SeatArea) → FanDirectionType
 * - 와이퍼 단: Wiper.getCurrentWiperSwitch(WindowArea) → WiperSwitch
 * 반환 enum 의 이름(name)을 PollQuest.isTarget 으로 넘겨 목표 여부를 판정한다.
 *
 * 안전 규칙(요구사항):
 * 1) 주차(GEAR_PARK)에서만 조회한다. 시작 시 현재 기어를 확인하고, 주차가 아니면 조회를 멈춘다.
 * 2) registerGearSelection 으로 기어 변화를 구독한다. 주행 등 '주차 아님'으로 바뀌면 즉시 조회를 멈춘다
 *    (주행 제한 진입). 다시 주차로 돌아오면 재개한다.
 * 3) 직렬 조회: 앞 조회의 콜백(성공/실패)이 끝나기 전에는 다음 조회를 보내지 않는다(inFlight 가드).
 * 4) stop() 이후(또는 세대 번호가 바뀐 뒤) 도착하는 모든 응답은 무시한다(generation 가드).
 * 5) 조회는 퀘스트 수행 중에만 돈다. 컨트롤러가 stop() 하면 즉시 멈춘다.
 *
 * 폴링 주기: 목표값 조작을 사람이 수행하는 동안 반응이 느리지 않도록 600ms 간격.
 */
class VehiclePolledSource(
    private val appContext: Context,
    private val quest: PollQuest,
) : PollSource {

    private val main = Handler(Looper.getMainLooper())
    private var vehicle: Vehicle? = null
    private var observer: PollSource.Observer? = null

    private val frontPair = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT)
    private val frontWindshield = WindowArea(WindowValue.FRONT_WINDSHIELD)

    /** 조회 세대. start/stop 마다 올려 늦은 응답을 식별·무시한다. */
    private var generation = 0
    /** 현재 수행 중(폴링 루프가 돌아야 하는지). */
    private var running = false
    /** 주차 여부(게이트). */
    private var parked = false
    /** 앞 조회의 응답이 아직 안 온 상태. 직렬 조회 보장. */
    private var inFlight = false
    /** 초기(onInitial) 1회 보고 완료 여부. */
    private var initialReported = false

    private var gearListener: GearSelectionListener? = null

    override fun start(observer: PollSource.Observer) {
        this.observer = observer
        val gen = ++generation
        running = true
        inFlight = false
        initialReported = false
        parked = false
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v

            // 기어 변화 구독: 주차↔주행 전환에 반응.
            val gl = object : GearSelectionListener {
                override fun onSelected(gear: VehicleGear?) {
                    main.post { if (gen == generation) onGear(gear, gen) }
                }
                override fun onFailed(e: Exception) {
                    Log.e(TAG, "[${quest.questId}] 기어 구독 실패: ${e.javaClass.simpleName} ${e.message}")
                }
            }
            gearListener = gl
            v.getGear().registerGearSelection(gl)

            // 현재 기어 1회 확인 → 주차면 폴링 시작, 아니면 대기(주행 제한 안내).
            v.getGear().getCurrentGearSelectionState(
                { gear -> main.post { if (gen == generation) onGear(gear, gen) } },
                { e -> main.post { if (gen == generation) fail("기어 상태를 확인하지 못했어요. (${e.javaClass.simpleName})") } }
            )
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { if (gen == generation) fail("차량 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    private fun onGear(gear: VehicleGear?, gen: Int) {
        if (gen != generation || !running) return
        val nowParked = gear == VehicleGear.GEAR_PARK
        if (nowParked == parked) {
            // 상태 동일: 주차인데 아직 루프가 안 돌고 있으면(최초 확인) 시작.
            if (nowParked && !inFlight) pollOnce(gen)
            return
        }
        parked = nowParked
        if (nowParked) {
            Log.d(TAG, "[${quest.questId}] 주차 확인 → 조회 시작")
            pollOnce(gen)
        } else {
            // 주행 등으로 전환: 조회 멈춤(주행 제한 진입).
            Log.d(TAG, "[${quest.questId}] 주차 해제 → 조회 멈춤")
            fail("주차(P) 상태에서만 확인할 수 있어요. 기어를 P 로 두고 다시 시도해줘.")
        }
    }

    /** 한 번만 조회한다. 응답 콜백 안에서 다음 조회를 예약(직렬). */
    private fun pollOnce(gen: Int) {
        if (gen != generation || !running || !parked) return
        if (inFlight) return          // 직렬 보장: 앞 조회가 안 끝났으면 건너뜀.
        val v = vehicle ?: return
        inFlight = true
        val onToken: (String?) -> Unit = { token ->
            main.post {
                if (gen != generation) return@post   // 늦은 응답 무시.
                inFlight = false
                deliver(quest.isTarget(token))
                scheduleNext(gen)
            }
        }
        val onErr: (Exception) -> Unit = { e ->
            main.post {
                if (gen != generation) return@post   // 늦은 응답 무시.
                inFlight = false
                Log.e(TAG, "[${quest.questId}] 조회 실패: ${e.javaClass.simpleName} ${e.message}")
                // 일시 실패는 완료로 치지 않고 다음 주기에 재시도(사유만 알림).
                observer?.onUpdate(null)
                scheduleNext(gen)
            }
        }
        try {
            when (quest.kind) {
                PollKind.FAN_DIRECTION ->
                    v.getHVAC().getHvacFanDirection(frontPair, { onToken(it?.name) }, onErr)
                PollKind.WIPER_SWITCH ->
                    v.getWiper().getCurrentWiperSwitch(frontWindshield, { onToken(it?.name) }, onErr)
            }
        } catch (e: Exception) {
            inFlight = false
            fail("조회 중 오류가 났어요. (${e.javaClass.simpleName})")
        }
    }

    private fun deliver(isTarget: Boolean?) {
        if (!initialReported) {
            initialReported = true
            observer?.onInitial(isTarget)
        } else {
            observer?.onUpdate(isTarget)
        }
    }

    private fun scheduleNext(gen: Int) {
        if (gen != generation || !running || !parked) return
        main.postDelayed({ pollOnce(gen) }, POLL_INTERVAL_MS)
    }

    private fun fail(reason: String) {
        observer?.onError(reason)
    }

    override fun stop() {
        running = false
        parked = false
        inFlight = false
        generation++               // 진행 중/예약된 조회를 모두 무효화.
        observer = null
        main.removeCallbacksAndMessages(null)
        try {
            gearListener?.let { vehicle?.getGear()?.unregisterGearSelection(it) }
        } catch (e: Exception) {
            Log.e(TAG, "기어 구독 해제 실패: ${e.message}")
        }
        gearListener = null
        try {
            vehicle?.release()
        } catch (e: Exception) {
            Log.e(TAG, "release 실패: ${e.message}")
        }
        vehicle = null
    }

    companion object {
        private const val TAG = "PollSource"
        private const val POLL_INTERVAL_MS = 600L
    }
}
