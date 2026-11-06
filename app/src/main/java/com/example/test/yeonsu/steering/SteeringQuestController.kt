package com.example.test.yeonsu.steering

import android.content.Context

/**
 * '운전대 방향' 퀘스트(vehicle-6-0)의 수신 제어부.
 *
 * 역할:
 * - SteeringSource(기본: Vehicle SDK SteeringWheel 각도)를 구독하고,
 *   수신 각도를 순수 판정 로직(SteeringLeftSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(다른 신호 컨트롤러와 동일):
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false).
 * - null 은 완료로 취급하지 않는다.
 * - onError 이후에는 판정을 초기화하고(기준 각도 재설정), 다음 수신부터 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 재진입(새 인스턴스)마다 새 Sequence(새 기준 각도)로 시작한다.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 SteeringSource 주입) JVM 단위 테스트가 가능하다.
 */
class SteeringQuestController(
    private val source: SteeringSource,
    private val callback: Callback,
) {
    constructor(appContext: Context, callback: Callback) :
        this(VehicleSteeringSource(appContext.applicationContext), callback)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: SteeringLeftSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : SteeringSource.Observer {
        override fun onInitial(angle: Float?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = SteeringLeftSequence(angle)
                sequence = seq
                callback.onStageChanged(seq.guideText())
            }
        }
        override fun onUpdate(angle: Float?) = handle(angle)
        override fun onError(reason: String) {
            if (!active) return
            sequence = null
            callback.onError(reason)
        }
    }

    fun start() {
        if (active) return
        active = true
        completed = false
        sequence = null
        source.start(observer)
    }

    private fun handle(angle: Float?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            val s = SteeringLeftSequence(angle)
            sequence = s
            callback.onStageChanged(s.guideText())
            return
        }
        val wasDone = seq.isDone
        val justDone = seq.onSignal(angle)
        if (seq.isDone != wasDone) callback.onStageChanged(seq.guideText())
        if (justDone && !completed) {
            completed = true
            callback.onCompleted()
        }
    }

    fun stop() {
        active = false
        source.stop()
    }

    companion object {
        /** 이 컨트롤러가 담당하는 퀘스트 ID (운전대 방향). */
        const val QUEST_ID = "vehicle-6-0"

        /**
         * 운전대 방향(vehicle-6-0)은 현재 통합 범위에서 자동 판정을 보류한다(항상 false).
         *
         * 사유: 각도의 물리적 좌/우(부호) 규약을 독립적으로 확정할 근거가 없다.
         * - Pleos SDK/SteeringWheel API 에 방향 부호 문서/상수 없음.
         * - AOSP PERF_STEERING_ANGLE 문서에 좌=양수/음수 명시 없음(업계 관행도 센서는 보통 우=양수로 상반).
         * - Mac 가상 콘솔은 steering 부호가 SDK 수신값과 반전되어(콘솔 -40 → 앱 +40) 물리 방향 기준이 되지 못함.
         * 판정 로직(SteeringLeftSequence: 양수=왼쪽, +임계)과 수신 경로 구현은 그대로 두고,
         * 왼쪽 완료/오른쪽 미완료를 신뢰성 있게 증명할 수 있을 때 다시 연결한다.
         */
        fun isSteeringQuest(questId: String): Boolean = false
    }
}
