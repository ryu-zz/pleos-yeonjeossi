package com.example.test.yeonsu.polling

import android.content.Context

/**
 * 폴링 퀘스트(송풍 방향 vehicle-5-3 / 자동·저속·고속 와이퍼 vehicle-2-1·2·3)의 수신 제어부.
 *
 * 역할:
 * - PollSource(기본: Vehicle SDK getter 주기 조회)를 시작하고, 조회 결과(목표 여부)를
 *   순수 판정 로직(PollSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(다른 신호 컨트롤러와 동일):
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false). → 중단·완료·화면 이탈 후 늦은 응답 무시.
 * - null(조회 불가/불명)은 완료로 취급하지 않는다.
 * - onError 이후에는 순서 판정을 초기화하고, 다음 조회부터 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 시작 시 이미 목표값이면 다른 값을 거친 뒤 다시 목표값에 도달해야 완료(PollSequence).
 *
 * SDK 에 직접 의존하지 않으므로(가짜 PollSource 주입) JVM 단위 테스트가 가능하다.
 */
class PolledQuestController(
    private val source: PollSource,
    private val callback: Callback,
    private val quest: PollQuest,
) {
    constructor(appContext: Context, callback: Callback, quest: PollQuest) :
        this(VehiclePolledSource(appContext.applicationContext, quest), callback, quest)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: PollSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : PollSource.Observer {
        override fun onInitial(isTarget: Boolean?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = PollSequence(isTarget)
                sequence = seq
                callback.onStageChanged(guideText(seq))
                // 폴링 퀘스트는 모두 조작 연습형이라 시작값만으로는 완료되지 않는다(DONE 로 시작 불가).
            }
        }
        override fun onUpdate(isTarget: Boolean?) = handle(isTarget)
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

    private fun handle(isTarget: Boolean?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            val s = PollSequence(isTarget)
            sequence = s
            callback.onStageChanged(guideText(s))
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(isTarget)
        if (seq.stage != before) callback.onStageChanged(guideText(seq))
        if (justDone && !completed) {
            completed = true
            callback.onCompleted()
        }
    }

    fun stop() {
        active = false
        source.stop()
    }

    private fun guideText(seq: PollSequence): String = when (seq.stage) {
        PollStage.DONE -> quest.doneGuide
        PollStage.NEED_OPPOSITE_FIRST -> quest.alreadyGuide
        PollStage.WAIT_TARGET -> quest.askGuide
    }

    companion object {
        fun isPollQuest(questId: String): Boolean = PollQuest.isPollQuest(questId)
        fun questKind(questId: String): PollQuest? = PollQuest.questKind(questId)
    }
}
