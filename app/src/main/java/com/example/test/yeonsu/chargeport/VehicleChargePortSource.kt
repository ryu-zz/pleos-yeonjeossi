package com.example.test.yeonsu.chargeport

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '충전구' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 충전구 = EvBattery.isEvChargePortOpen / registerEvChargePortOpen (EvChargePortOpenListener).
 * 값: true=열림, false=닫힘, null=불명.
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleChargePortSource(private val appContext: Context) : ChargePortSource {

    private var vehicle: Vehicle? = null
    private var observer: ChargePortSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : EvChargePortOpenListener {
        override fun onEvChargePortOpenUpdated(open: Boolean?) {
            Log.d(TAG, "충전구 열림=$open")
            main.post { observer?.onUpdate(open) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "충전구 상태 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("충전구 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun start(observer: ChargePortSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val battery = v.getEvBattery()
            battery.isEvChargePortOpen(
                { current -> Log.d(TAG, "현재 충전구 열림=$current"); main.post { this.observer?.onInitial(current) } },
                { e ->
                    Log.e(TAG, "현재 충전구 상태 읽기 실패: ${e.message}")
                    main.post { this.observer?.onInitial(null) }
                }
            )
            battery.registerEvChargePortOpen(listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("충전구 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getEvBattery()?.unregisterEvChargePortOpen(listener)
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
        private const val TAG = "ChargePortSource"
    }
}
