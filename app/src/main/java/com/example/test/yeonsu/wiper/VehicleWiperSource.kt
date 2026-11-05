package com.example.test.yeonsu.wiper

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.constant.control.WiperState
import ai.pleos.playground.vehicle.listener.WiperStateListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '와이퍼' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 앞유리 와이퍼 = WindowArea(WindowValue.FRONT_WINDSHIELD).
 * getCurrentWiperState / registerWiperState (WiperStateListener).
 * WiperState 매핑: ON → on=true, OFF → on=false, OTHER/SERVICE/null → null(불명, 완료 아님).
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleWiperSource(private val appContext: Context) : WiperSource {

    private val area = WindowArea(WindowValue.FRONT_WINDSHIELD)
    private var vehicle: Vehicle? = null
    private var observer: WiperSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : WiperStateListener {
        override fun onWiperStateUpdated(state: WiperState?) {
            val on = map(state)
            Log.d(TAG, "와이퍼 상태=$state → on=$on")
            main.post { observer?.onUpdate(on) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "와이퍼 상태 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("와이퍼 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    private fun map(s: WiperState?): Boolean? = when (s) {
        WiperState.ON -> true
        WiperState.OFF -> false
        else -> null
    }

    override fun start(observer: WiperSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val wiper = v.getWiper()
            wiper.getCurrentWiperState(
                area,
                { current -> Log.d(TAG, "현재 와이퍼 상태=$current"); main.post { this.observer?.onInitial(map(current)) } },
                { e ->
                    Log.e(TAG, "현재 와이퍼 상태 읽기 실패: ${e.message}")
                    main.post { this.observer?.onInitial(null) }
                }
            )
            wiper.registerWiperState(area, listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("와이퍼 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getWiper()?.unregisterWiperState(area, listener)
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
        private const val TAG = "WiperSource"
    }
}
