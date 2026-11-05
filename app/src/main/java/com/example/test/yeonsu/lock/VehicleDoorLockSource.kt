package com.example.test.yeonsu.lock

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorLockStateListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '전체 문 잠금' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 이 에뮬레이터(AVD 5556)의 VHAL DOOR_LOCK 프로퍼티는 문 네 짝을 하나로 묶은
 * 단일 영역(areaId 0x55 = ROW_1_LEFT|ROW_1_RIGHT|ROW_2_LEFT|ROW_2_RIGHT)만 지원한다.
 * (per-door 영역 0x1/0x4/0x10/0x40 로 읽으면 "area ID not supported" 예외가 난다.)
 * 그래서 DoorArea 가변인자 생성자에 네 값을 함께 넘겨 하나의 영역으로 구독/조회하고,
 * 그 Boolean(전체 잠김 여부)을 그대로 상위로 올린다.
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleDoorLockSource(private val appContext: Context) : DoorLockSource {

    // 문 네 짝을 하나로 묶은 영역(VHAL DOOR_LOCK areaId 0x55 에 대응).
    private val area = DoorArea(
        DoorValue.ROW_1_LEFT,
        DoorValue.ROW_1_RIGHT,
        DoorValue.ROW_2_LEFT,
        DoorValue.ROW_2_RIGHT,
    )
    private var vehicle: Vehicle? = null
    private var observer: DoorLockSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : DoorLockStateListener {
        override fun onDoorLockStateUpdated(state: Boolean?) {
            Log.d(TAG, "전체 문 잠김=$state")
            main.post { observer?.onUpdate(state) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "문 잠금 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("문 잠금 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun start(observer: DoorLockSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val door = v.getDoor()
            door.getDoorLockState(
                area,
                { cur -> Log.d(TAG, "현재 전체 문 잠김=$cur"); main.post { this.observer?.onInitial(cur) } },
                { e ->
                    Log.e(TAG, "현재 문 잠금 읽기 실패: ${e.message}")
                    main.post { this.observer?.onInitial(null) }
                }
            )
            door.registerDoorLockState(area, listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("문 잠금 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getDoor()?.unregisterDoorLockState(area, listener)
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
        private const val TAG = "DoorLockSource"
    }
}
