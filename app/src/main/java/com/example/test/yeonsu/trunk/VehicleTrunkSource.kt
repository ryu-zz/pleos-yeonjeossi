package com.example.test.yeonsu.trunk

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '트렁크' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 트렁크 = DoorArea(DoorValue.REAR). 운전석 문(VehicleDoorSource)과 같은 Door 위치 API 를 쓴다.
 * 위치 position: 0=닫힘, >0=열림(부분 포함), null=불명.
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleTrunkSource(private val appContext: Context) : TrunkSource {

    private val area = DoorArea(DoorValue.REAR)
    private var vehicle: Vehicle? = null
    private var observer: TrunkSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : DoorPositionListener {
        override fun onPositionUpdated(pos: Int?) {
            val s = map(pos)
            Log.d(TAG, "트렁크 위치=$pos → $s")
            main.post { observer?.onUpdate(s) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "트렁크 상태 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("트렁크 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    private fun map(pos: Int?): TrunkState? = when {
        pos == null -> null
        pos == 0 -> TrunkState.CLOSED
        pos > 0 -> TrunkState.OPEN
        else -> null
    }

    override fun start(observer: TrunkSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val door = v.getDoor()
            door.getCurrentDoorPosition(
                area,
                { current -> Log.d(TAG, "현재 트렁크 위치=$current"); main.post { this.observer?.onInitial(map(current)) } },
                { e ->
                    Log.e(TAG, "현재 트렁크 위치 읽기 실패: ${e.message}")
                    main.post { this.observer?.onInitial(null) }
                }
            )
            door.registerDoorPosition(area, listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("트렁크 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getDoor()?.unregisterDoorPosition(area, listener)
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
        private const val TAG = "TrunkSource"
    }
}
