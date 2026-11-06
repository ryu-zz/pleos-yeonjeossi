package com.example.test.yeonsu.steering

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '운전대 각도' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 운전대 각도 = SteeringWheel.getCurrentSteeringWheelAngle / registerSteeringWheelAngle.
 * VHAL PERF_STEERING_ANGLE(areaId 0x0, 연속 스트림, 약 -470~500도)에 대응. 영역 인자가 없다.
 * 양수 = 왼쪽(반시계). 값은 Float(도).
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleSteeringSource(private val appContext: Context) : SteeringSource {

    private var vehicle: Vehicle? = null
    private var observer: SteeringSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : SteeringWheelAngleListener {
        override fun onSteeringWheelAngleUpdated(angle: Float?) {
            Log.d(TAG, "운전대 각도=$angle")
            main.post { observer?.onUpdate(angle) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "운전대 각도 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("운전대 각도 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun start(observer: SteeringSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val steering = v.getSteeringWheel()
            steering.getCurrentSteeringWheelAngle(
                { current -> Log.d(TAG, "현재 운전대 각도=$current"); main.post { this.observer?.onInitial(current) } },
                { e ->
                    Log.e(TAG, "현재 운전대 각도 읽기 실패: ${e.message}")
                    main.post { this.observer?.onInitial(null) }
                }
            )
            steering.registerSteeringWheelAngle(listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("운전대 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getSteeringWheel()?.unregisterSteeringWheelAngle(listener)
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
        private const val TAG = "SteeringSource"
    }
}
