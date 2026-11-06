package com.example.test.yeonsu.brake

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '브레이크 페달' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 브레이크 = Brake.getCurrentBrakePedalStatus / registerBrakePedalStatus (BrakePedalStatusListener).
 * 값: true=밟음, false=뗌, null=불명.
 * (VHAL vendor 프로퍼티 BRAKE_PEDAL_STATUS 0x21200409: 밟으면 1, 떼면 0)
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleBrakeSource(private val appContext: Context) : BrakeSource {

    private var vehicle: Vehicle? = null
    private var observer: BrakeSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(pressed: Boolean?) {
            Log.d(TAG, "브레이크 눌림=$pressed")
            main.post { observer?.onUpdate(pressed) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "브레이크 상태 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("브레이크 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun start(observer: BrakeSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val brake = v.getBrake()
            brake.getCurrentBrakePedalStatus(
                { current -> Log.d(TAG, "현재 브레이크 눌림=$current"); main.post { this.observer?.onInitial(current) } },
                { e ->
                    Log.e(TAG, "현재 브레이크 상태 읽기 실패: ${e.message}")
                    main.post { this.observer?.onInitial(null) }
                }
            )
            brake.registerBrakePedalStatus(listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("브레이크 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getBrake()?.unregisterBrakePedalStatus(listener)
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
        private const val TAG = "BrakeSource"
    }
}
