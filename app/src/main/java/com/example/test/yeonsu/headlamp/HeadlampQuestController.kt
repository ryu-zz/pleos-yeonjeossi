package com.example.test.yeonsu.headlamp

import android.content.Context

/**
 * '하향등' 퀘스트(vehicle-3-1)의 수신 제어부.
 *
 * 역할:
 * - HeadlampSource(기본: Vehicle SDK LowBeamLightState)를 구독하고, 수신 값을 순수 판정 로직(HeadlampOnSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(방향지시등/문 컨트롤러와 동일):
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false).
 * - null 은 완료로 취급하지 않는다.
 * - onError(연결 끊김/구독 실패) 이후에는 순서 판정을 초기화하고, 다음 수신(재연결)부터 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 재진입(새 인스턴스)마다 새 Sequence 로 시작한다.
 *
 * '하향등'은 시작 시 이미 켜져 있으면 바로 완료해야 하므로, onInitial 에서도 완료를 통지한다.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 HeadlampSource 주입) JVM 단위 테스트가 가능하다.
 */
class HeadlampQuestController(
    private val source: HeadlampSource,
    private val callback: Callback,
) {
    /** 앱 실제 경로: Vehicle SDK 수신원(하향등)을 사용한다. */
    constructor(appContext: Context, callback: Callback) :
        this(VehicleHeadlampSource(appContext.applicationContext), callback)

    interface Callback {
        /** 단계가 바뀌었거나 안내 문구를 갱신해야 할 때. */
        fun onStageChanged(guide: String)
        /** 하향등 켜짐이 확인되어 완료되었을 때. 수행 1회당 정확히 한 번만 호출된다. */
        fun onCompleted()
        /** 구독 실패/오류 등 완료가 아닌 사유. */
        fun onError(reason: String)
    }

    private var sequence: HeadlampOnSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : HeadlampSource.Observer {
        override fun onInitial(state: HeadlampState?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = HeadlampOnSequence(state)
                sequence = seq
                callback.onStageChanged(seq.guideText())
                // 시작 시 이미 켜져 있으면 바로 완료.
                if (seq.isDone) complete()
            }
        }
        override fun onUpdate(state: HeadlampState?) = handle(state)
        override fun onError(reason: String) {
            if (!active) return
            // 연결 끊김/오류: 순서를 버리고 사유를 알린다. 다음 수신부터 다시 시작.
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

    private fun handle(state: HeadlampState?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            // onError 로 초기화됐거나 초기값 전에 업데이트가 먼저 온 경우: 이 값으로 새로 시작.
            val s = HeadlampOnSequence(state)
            sequence = s
            callback.onStageChanged(s.guideText())
            if (s.isDone) complete()
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(state)
        if (seq.stage != before) callback.onStageChanged(seq.guideText())
        if (justDone) complete()
    }

    private fun complete() {
        if (completed) return
        completed = true
        callback.onCompleted()
    }

    fun stop() {
        active = false
        source.stop()
    }

    companion object {
        /** 이 컨트롤러가 담당하는 퀘스트 ID (하향등). */
        const val QUEST_ID = "vehicle-3-1"

        /** 이 퀘스트 ID 가 하향등 신호로 자동 완료되는 대상인지. */
        fun isHeadlampQuest(questId: String): Boolean = questId == QUEST_ID
    }
}
