package com.example.test.yeonsu.window

import android.content.Context

/**
 * 운전석 창문 퀘스트(vehicle-4-0 열기 / vehicle-4-1 닫기)의 수신 제어부.
 *
 * 역할:
 * - WindowSource(기본: Vehicle SDK WindowPosition, ROW_1_LEFT)를 구독하고, 수신 위치를 순수 판정 로직(WindowSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(다른 신호 컨트롤러와 동일):
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false).
 * - null 은 완료로 취급하지 않는다.
 * - onError 이후에는 순서 판정을 초기화하고, 다음 수신부터 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 재진입(새 인스턴스)마다 새 Sequence 로 시작한다.
 *
 * 두 종류 모두 "수행 중 전이"에서만 완료하므로, 시작값만으로는 완료하지 않는다.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 WindowSource 주입) JVM 단위 테스트가 가능하다.
 */
class WindowQuestController(
    private val source: WindowSource,
    private val callback: Callback,
    private val quest: WindowQuest,
) {
    constructor(appContext: Context, callback: Callback, quest: WindowQuest) :
        this(VehicleWindowSource(appContext.applicationContext), callback, quest)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: WindowSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : WindowSource.Observer {
        override fun onInitial(position: Int?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = WindowSequence(position, quest)
                sequence = seq
                callback.onStageChanged(seq.guideText())
            }
        }
        override fun onUpdate(position: Int?) = handle(position)
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

    private fun handle(position: Int?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            val s = WindowSequence(position, quest)
            sequence = s
            callback.onStageChanged(s.guideText())
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(position)
        if (seq.stage != before) callback.onStageChanged(seq.guideText())
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
        /** 운전석 창문 열기 퀘스트 ID. */
        const val QUEST_ID_OPEN = "vehicle-4-0"
        /** 창문 닫기 퀘스트 ID. */
        const val QUEST_ID_CLOSE = "vehicle-4-1"

        private val QUEST_BY_ID = mapOf(
            QUEST_ID_OPEN to WindowQuest.OPEN,
            QUEST_ID_CLOSE to WindowQuest.CLOSE,
        )

        /** 이 퀘스트 ID 가 창문 위치 신호로 자동 완료되는 대상인지. */
        fun isWindowQuest(questId: String): Boolean = QUEST_BY_ID.containsKey(questId)

        /** 퀘스트 ID 에 해당하는 판정 종류. 대상이 아니면 null. */
        fun questKind(questId: String): WindowQuest? = QUEST_BY_ID[questId]
    }
}
