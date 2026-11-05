package com.example.test.yeonsu.highbeam

import android.content.Context

/**
 * 상향등 퀘스트(vehicle-3-2 켜기 / vehicle-3-3 끄기)의 수신 제어부.
 *
 * 역할:
 * - HighBeamSource(기본: Vehicle SDK HighBeamLightState)를 구독하고, 수신 값을 순수 판정 로직(HighBeamSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(방향지시등/문/하향등 컨트롤러와 동일):
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false).
 * - null 은 완료로 취급하지 않는다.
 * - onError(연결 끊김/구독 실패) 이후에는 순서 판정을 초기화하고, 다음 수신(재연결)부터 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 재진입(새 인스턴스)마다 새 Sequence 로 시작한다.
 *
 * 두 종류 모두 "수행 중 전이"(OFF→ON 또는 ON→OFF)에서만 완료하므로, 시작값만으로는 완료하지 않는다.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 HighBeamSource 주입) JVM 단위 테스트가 가능하다.
 */
class HighBeamQuestController(
    private val source: HighBeamSource,
    private val callback: Callback,
    private val quest: HighBeamQuest,
) {
    /** 앱 실제 경로: Vehicle SDK 수신원(상향등)을 사용한다. */
    constructor(appContext: Context, callback: Callback, quest: HighBeamQuest) :
        this(VehicleHighBeamSource(appContext.applicationContext), callback, quest)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: HighBeamSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : HighBeamSource.Observer {
        override fun onInitial(state: HighBeamState?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = HighBeamSequence(state, quest)
                sequence = seq
                callback.onStageChanged(seq.guideText())
            }
        }
        override fun onUpdate(state: HighBeamState?) = handle(state)
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

    private fun handle(state: HighBeamState?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            val s = HighBeamSequence(state, quest)
            sequence = s
            callback.onStageChanged(s.guideText())
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(state)
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
        /** 상향등 켜기 퀘스트 ID. */
        const val QUEST_ID_ON = "vehicle-3-2"
        /** 상향등 끄기 퀘스트 ID. */
        const val QUEST_ID_OFF = "vehicle-3-3"

        private val QUEST_BY_ID = mapOf(
            QUEST_ID_ON to HighBeamQuest.ON,
            QUEST_ID_OFF to HighBeamQuest.OFF,
        )

        /** 이 퀘스트 ID 가 상향등 신호로 자동 완료되는 대상인지. */
        fun isHighBeamQuest(questId: String): Boolean = QUEST_BY_ID.containsKey(questId)

        /** 퀘스트 ID 에 해당하는 판정 종류. 대상이 아니면 null. */
        fun questKind(questId: String): HighBeamQuest? = QUEST_BY_ID[questId]
    }
}
