package com.example.test.yeonsu.headlamp

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.listener.LightStateListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '하향등(저빔)' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 하향등 = Light.getCurrentLowBeamLightState / registerLowBeamLightState (LightStateListener).
 * LightState 매핑: OFF→OFF, ON→ON, DAYTIME_RUNNING→DAYTIME_RUNNING, null→불명.
 * (SDK 소스에서 LowBeamLightState 가 OFF/ON/DAYTIME_RUNNING 로 구분됨을 확인하고 채택했다.)
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleHeadlampSource(private val appContext: Context) : HeadlampSource {

    private var vehicle: Vehicle? = null
    private var observer: HeadlampSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : LightStateListener {
        override fun onLightStateUpdated(state: LightState?) {
            val s = map(state)
            main.post { observer?.onUpdate(s) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "하향등 상태 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("전조등 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    private fun map(v: LightState?): HeadlampState? = when (v) {
        LightState.OFF -> HeadlampState.OFF
        LightState.ON -> HeadlampState.ON
        LightState.DAYTIME_RUNNING -> HeadlampState.DAYTIME_RUNNING
        null -> null
    }

    override fun start(observer: HeadlampSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val light = v.getLight()
            light.getCurrentLowBeamLightState(
                { current -> main.post { this.observer?.onInitial(map(current)) } },
                { e ->
                    Log.e(TAG, "현재 하향등 상태 읽기 실패: ${e.message}")
                    // 현재값을 못 읽어도 구독으로 이어가도록 초기값 null 통지.
                    main.post { this.observer?.onInitial(null) }
                }
            )
            light.registerLowBeamLightState(listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("전조등 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getLight()?.unregisterLowBeamLightState(listener)
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
        private const val TAG = "HeadlampSource"
    }
}
