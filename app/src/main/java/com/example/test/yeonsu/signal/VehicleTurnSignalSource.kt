package com.example.test.yeonsu.signal

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 방향지시등 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleTurnSignalSource(private val appContext: Context) : TurnSignalSource {

    private var vehicle: Vehicle? = null
    private var observer: TurnSignalSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            val s = map(state)
            main.post { observer?.onUpdate(s) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "turn signal 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("방향지시등 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    private fun map(v: TurnSignalValue?): Signal? = when (v) {
        TurnSignalValue.OFF -> Signal.OFF
        TurnSignalValue.LEFT -> Signal.LEFT
        TurnSignalValue.RIGHT -> Signal.RIGHT
        null -> null
    }

    override fun start(observer: TurnSignalSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val ts = v.getTurnSignal()
            ts.getCurrentTurnSignalState(
                onComplete = { current -> main.post { this.observer?.onInitial(map(current)) } },
                onFailed = { e ->
                    Log.e(TAG, "현재 상태 읽기 실패: ${e.message}")
                    // 현재값을 못 읽어도 구독으로 이어가도록 초기값 null 통지.
                    main.post { this.observer?.onInitial(null) }
                }
            )
            ts.registerTurnSignalState(listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("방향지시등 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getTurnSignal()?.unregisterTurnSignalState(listener)
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
        private const val TAG = "TurnSignalSource"
    }
}
