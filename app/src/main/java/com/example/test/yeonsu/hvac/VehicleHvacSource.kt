package com.example.test.yeonsu.hvac

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.MirrorArea
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.MirrorValue
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.HvacAcStateListener
import ai.pleos.playground.vehicle.listener.HvacDefrosterStateListener
import ai.pleos.playground.vehicle.listener.HvacFanSpeedListener
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.HvacSeatVentilationListener
import ai.pleos.playground.vehicle.listener.HvacSideMirrorHeatStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureValueListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '공조·시야' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 각 HvacQuest 에 맞는 getCurrent / register 계열 API 를 호출하고, 받은 값을
 * "목표 조건 충족 여부(isTarget)"로 바꿔 Observer 로 올린다.
 *
 * 영역(Area)은 이 에뮬레이터(AVD 5556) VHAL 이 지원하는 areaId 에 맞춘다
 * (DOOR_LOCK 처럼 미지원 영역으로 조회하면 예외가 나므로, dumpsys 로 확인한 영역을 쓴다):
 * - HVAC_AC_ON / HVAC_RECIRC_ON: areaId 0x75 = 앞2 + 뒤3 좌석 묶음.
 * - HVAC_TEMPERATURE_SET: areaId 0x1 = 운전석. (config: 15~30℃)
 * - HVAC_FAN_SPEED: areaId 0x5 = 앞 두 좌석. (1~8)
 * - HVAC_DEFROSTER: areaId 0x1 = 앞유리(FRONT_WINDSHIELD).
 * - HVAC_SIDE_MIRROR_HEAT: areaId 0x3 = 좌·우 사이드미러. (0~3)
 * - HVAC_SEAT_VENTILATION: areaId 0x1 = 운전석. (0~3)
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleHvacSource(
    private val appContext: Context,
    private val quest: HvacQuest,
) : HvacSource {

    private var vehicle: Vehicle? = null
    private var observer: HvacSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    // 공통 좌석 영역들.
    private val allSeats = SeatArea(
        SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT,
        SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT,
    )
    private val frontPair = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT)
    private val driverSeat = SeatArea(SeatValue.ROW_1_LEFT)
    private val frontWindshield = WindowArea(WindowValue.FRONT_WINDSHIELD)
    private val bothMirrors = MirrorArea(MirrorValue.DRIVER_LEFT, MirrorValue.DRIVER_RIGHT)

    // 보관해 둔 리스너(해제용). 퀘스트별로 하나만 만든다.
    private var acL: HvacAcStateListener? = null
    private var recircL: HvacRecirculationStateListener? = null
    private var defrostL: HvacDefrosterStateListener? = null
    private var tempL: HvacTemperatureValueListener? = null
    private var fanL: HvacFanSpeedListener? = null
    private var mirrorL: HvacSideMirrorHeatStateListener? = null
    private var seatVentL: HvacSeatVentilationListener? = null

    private fun emitInitial(isTarget: Boolean?) {
        Log.d(TAG, "[${quest.questId}] 현재 isTarget=$isTarget")
        main.post { observer?.onInitial(isTarget) }
    }

    private fun emitUpdate(isTarget: Boolean?) {
        Log.d(TAG, "[${quest.questId}] 수신 isTarget=$isTarget")
        main.post { observer?.onUpdate(isTarget) }
    }

    private fun fail(e: Exception) {
        Log.e(TAG, "[${quest.questId}] 공조 신호 실패: ${e.javaClass.simpleName} ${e.message}")
        main.post { observer?.onError("공조 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
    }

    // Boolean 목표: true 면 isTarget=true. (외기 모드는 recirc==false 가 목표)
    private fun boolTarget(v: Boolean?, want: Boolean): Boolean? = if (v == null) null else (v == want)
    // Int 레벨 목표: 레벨 > 0.
    private fun levelOn(v: Int?): Boolean? = if (v == null) null else (v > 0)
    // Int 정확값 목표.
    private fun intEquals(v: Int?, target: Int): Boolean? = if (v == null) null else (v == target)
    // Float 목표(근사 비교).
    private fun floatEquals(v: Float?, target: Float): Boolean? =
        if (v == null) null else (kotlin.math.abs(v - target) < 0.5f)

    override fun start(observer: HvacSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val hvac = v.getHVAC()
            val seat = v.getSeat()
            val mirror = v.getSideMirror()
            when (quest) {
                HvacQuest.AC_ON -> {
                    hvac.getHvacAcState(allSeats, { emitInitial(boolTarget(it, true)) }, { emitInitial(null) })
                    acL = object : HvacAcStateListener {
                        override fun onHvacAcStateUpdated(state: Boolean?) = emitUpdate(boolTarget(state, true))
                        override fun onFailed(e: Exception) = fail(e)
                    }
                    hvac.registerHvacAcState(allSeats, acL!!)
                }
                HvacQuest.RECIRC_ON, HvacQuest.RECIRC_OFF -> {
                    val want = quest == HvacQuest.RECIRC_ON
                    hvac.getHvacRecirculationState(allSeats, { emitInitial(boolTarget(it, want)) }, { emitInitial(null) })
                    recircL = object : HvacRecirculationStateListener {
                        override fun onHvacAutoRecirculationStateUpdated(state: Boolean?) = emitUpdate(boolTarget(state, want))
                        override fun onFailed(e: Exception) = fail(e)
                    }
                    hvac.registerHvacRecirculationState(allSeats, recircL!!)
                }
                HvacQuest.DEFROST_ON -> {
                    hvac.getHvacDefrosterState(frontWindshield, { emitInitial(boolTarget(it, true)) }, { emitInitial(null) })
                    defrostL = object : HvacDefrosterStateListener {
                        override fun onHvacDefrosterStateUpdated(state: Boolean?) = emitUpdate(boolTarget(state, true))
                        override fun onFailed(e: Exception) = fail(e)
                    }
                    hvac.registerHvacDefrosterState(frontWindshield, defrostL!!)
                }
                HvacQuest.TEMP_22 -> {
                    hvac.getHvacTemperature(driverSeat, TemperatureUnit.CELSIUS, { emitInitial(floatEquals(it, 22f)) }, { emitInitial(null) })
                    tempL = object : HvacTemperatureValueListener {
                        override fun onHvacTemperatureUpdated(value: Float?, suggestion: Float?) = emitUpdate(floatEquals(value, 22f))
                        override fun onFailed(e: Exception) = fail(e)
                    }
                    hvac.registerHvacTemperature(driverSeat, tempL!!)
                }
                HvacQuest.FAN_3 -> {
                    hvac.getHvacFanSpeed(frontPair, { emitInitial(intEquals(it, 3)) }, { emitInitial(null) })
                    fanL = object : HvacFanSpeedListener {
                        override fun onHvacFanSpeedUpdated(speed: Int?) = emitUpdate(intEquals(speed, 3))
                        override fun onFailed(e: Exception) = fail(e)
                    }
                    hvac.registerHvacFanSpeed(frontPair, fanL!!)
                }
                HvacQuest.MIRROR_HEAT_ON -> {
                    mirror.getHvacSideMirrorHeatState(bothMirrors, { emitInitial(levelOn(it)) }, { emitInitial(null) })
                    mirrorL = object : HvacSideMirrorHeatStateListener {
                        override fun onHvacSideMirrorHeatStateUpdated(level: Int?) = emitUpdate(levelOn(level))
                        override fun onFailed(e: Exception) = fail(e)
                    }
                    mirror.registerHvacSideMirrorHeatState(bothMirrors, mirrorL!!)
                }
                HvacQuest.SEAT_VENT_ON -> {
                    seat.getHvacSeatVentilationState(driverSeat, { emitInitial(levelOn(it)) }, { emitInitial(null) })
                    seatVentL = object : HvacSeatVentilationListener {
                        override fun onHvacSeatVentilationUpdated(level: Int?) = emitUpdate(levelOn(level))
                        override fun onFailed(e: Exception) = fail(e)
                    }
                    seat.registerHvacSeatVentilation(driverSeat, seatVentL!!)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("공조 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            val v = vehicle
            val hvac = v?.getHVAC()
            acL?.let { hvac?.unregisterHvacAcState(allSeats, it) }
            recircL?.let { hvac?.unregisterHvacRecirculationState(allSeats, it) }
            defrostL?.let { hvac?.unregisterHvacDefrosterState(frontWindshield, it) }
            tempL?.let { hvac?.unregisterHvacTemperature(driverSeat, it) }
            fanL?.let { hvac?.unregisterHvacFanSpeed(frontPair, it) }
            mirrorL?.let { v?.getSideMirror()?.unregisterHvacSideMirrorHeatState(bothMirrors, it) }
            seatVentL?.let { v?.getSeat()?.unregisterHvacSeatVentilation(driverSeat, it) }
        } catch (e: Exception) {
            Log.e(TAG, "구독 해제 실패: ${e.message}")
        }
        try {
            vehicle?.release()
        } catch (e: Exception) {
            Log.e(TAG, "release 실패: ${e.message}")
        }
        vehicle = null
    }

    companion object {
        private const val TAG = "HvacSource"
    }
}
