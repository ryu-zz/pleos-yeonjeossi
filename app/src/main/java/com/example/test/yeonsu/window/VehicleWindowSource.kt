package com.example.test.yeonsu.window

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.WindowArea
import ai.pleos.playground.vehicle.constant.area.WindowValue
import ai.pleos.playground.vehicle.listener.WindowPositionListener

/**
 * Pleos Vehicle SDK 를 쓰는 실제 '운전석 창문' 수신원.
 * SDK 의존은 이 파일에만 둔다(컨트롤러/순서 로직은 SDK 비의존).
 *
 * 운전석 창문 = WindowArea(WindowValue.ROW_1_LEFT).
 * 위치 position: 0=완전히 닫힘, 1~100=열림(부분 포함). 영역별 구독이라 다른 창문 값은 들어오지 않는다.
 *
 * 콜백이 백그라운드 스레드로 올 수 있어 메인 스레드로 넘겨 순서 로직과 같은 스레드에서 처리한다.
 */
class VehicleWindowSource(private val appContext: Context) : WindowSource {

    private val area = WindowArea(WindowValue.ROW_1_LEFT)
    private var vehicle: Vehicle? = null
    private var observer: WindowSource.Observer? = null
    private val main = Handler(Looper.getMainLooper())

    private val listener = object : WindowPositionListener {
        override fun onWindowPositionUpdated(position: Int?) {
            main.post { observer?.onUpdate(position) }
        }
        override fun onFailed(e: Exception) {
            Log.e(TAG, "창문 위치 구독 실패: ${e.javaClass.simpleName} ${e.message}")
            main.post { observer?.onError("창문 상태 신호를 받지 못했어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun start(observer: WindowSource.Observer) {
        this.observer = observer
        try {
            val v = Vehicle(appContext)
            v.initialize()
            vehicle = v
            val window = v.getWindow()
            window.getCurrentWindowPosition(
                area,
                { current -> main.post { this.observer?.onInitial(current) } },
                { e ->
                    Log.e(TAG, "현재 창문 위치 읽기 실패: ${e.message}")
                    main.post { this.observer?.onInitial(null) }
                }
            )
            window.registerWindowPosition(area, listener)
        } catch (e: Exception) {
            Log.e(TAG, "SDK 초기화 실패: ${e.message}")
            main.post { this.observer?.onError("창문 기능을 사용할 수 없어요. (${e.javaClass.simpleName})") }
        }
    }

    override fun stop() {
        observer = null
        try {
            vehicle?.getWindow()?.unregisterWindowPosition(area, listener)
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
        private const val TAG = "WindowSource"
    }
}
