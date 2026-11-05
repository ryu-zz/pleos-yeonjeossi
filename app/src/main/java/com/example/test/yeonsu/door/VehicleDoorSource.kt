package com.example.test.yeonsu.door

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '운전석 문' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 운전석 문 = DoorArea(DoorValue.ROW_1_LEFT).
 * 문 위치 position 매핑: 0 = 닫힘(CLOSED), 1 = 열림(OPEN), 그 외/null = 불명.
 * (SDK 연결 확인 화면과 기존 CoachVehicleSource 에서 검증한 경로를 재사용한다.)
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleDoorSource(private val appContext: Context) : DoorSource {

    private val area = DoorArea(DoorValue.ROW_1_LEFT)
    private var vehicle: Vehicle? = null
    private var observer: DoorSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : DoorPositionListener {
        override fun onPositionUpdated(pos: Int?) {
            val s = map(pos)
            main.post { observer?.onUpdate(s) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "문 상태 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("문 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    private fun map(pos: Int?): DoorState? = when (pos) {
        0 -> DoorState.CLOSED
        1 -> DoorState.OPEN
        else -> null
    }

    override fun start(observer: DoorSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val door = v.getDoor()
            door.getCurrentDoorPosition(
                area,
                { current -> main.post { this.observer?.onInitial(map(current)) } },
                { e ->
                    Log.e(TAG, "현재 문 상태 읽기 실패: ${e.message}")
                    // 현재값을 못 읽어도 구독으로 이어가도록 초기값 null 통지.
                    main.post { this.observer?.onInitial(null) }
                }
            )
            door.registerDoorPosition(area, listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("문 확인 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
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
        private const val TAG = "DoorSource"
    }
}
