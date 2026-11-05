package com.example.test.yeonsu.signal

import android.content.Context

/**
 * 왼쪽 방향지시등 퀘스트의 수신 제어부.
 *
 * 역할:
 * - TurnSignalSource(기본: Vehicle SDK)를 구독하고, 수신 신호를 순수 판정 로직(TurnSignalSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙:
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false).
 * - null 은 완료로 취급하지 않는다(순수 로직이 단계를 유지).
 * - onError(연결 끊김/구독 실패) 이후에는 순서 판정을 초기화하고, 다음 수신(재연결)부터 새 OFF 로 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 재진입(새 인스턴스)마다 새 Sequence 로 시작하므로 이전 수행 중 상태를 이어 성공시키지 않는다.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 TurnSignalSource 주입) JVM 단위 테스트가 가능하다.
 */
class TurnSignalQuestController(
    private val source: TurnSignalSource,
    private val callback: Callback,
    private val quest: TurnSignalQuest = TurnSignalQuest.LEFT,
) {
    /** 앱 실제 경로: Vehicle SDK 수신원을 사용한다. */
    constructor(appContext: Context, callback: Callback, quest: TurnSignalQuest = TurnSignalQuest.LEFT) :
        this(VehicleTurnSignalSource(appContext.applicationContext), callback, quest)

    interface Callback {
        /** 단계가 바뀌었거나 안내 문구를 갱신해야 할 때. */
        fun onStageChanged(guide: String)
        /** OFF→LEFT→OFF 가 확인되어 완료되었을 때. 수행 1회당 정확히 한 번만 호출된다. */
        fun onCompleted()
        /** 구독 실패/오류 등 완료가 아닌 사유. */
        fun onError(reason: String)
    }

    private var sequence: TurnSignalSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : TurnSignalSource.Observer {
        override fun onInitial(signal: Signal?) {
            if (!active || completed) return
            // 현재값으로 순서 시작(LEFT면 먼저 끄기 안내). 이미 시작돼 있으면 유지.
            if (sequence == null) {
                sequence = TurnSignalSequence(signal, quest)
                callback.onStageChanged(sequence!!.guideText())
            }
        }
        override fun onUpdate(signal: Signal?) = handle(signal)
        override fun onError(reason: String) {
            if (!active) return
            // 연결 끊김/오류: 순서를 버리고 사유를 알린다. 다음 수신부터 새 OFF 로 다시 시작.
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

    private fun handle(signal: Signal?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            // onError 로 초기화됐거나 초기값 전에 업데이트가 먼저 온 경우: 이 값으로 새로 시작.
            val s = TurnSignalSequence(signal, quest)
            sequence = s
            callback.onStageChanged(s.guideText())
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(signal)
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
        /** 왼쪽 방향지시등 퀘스트 ID. (기존 상수 유지 - 하위호환) */
        const val QUEST_ID = "vehicle-1-0"

        /** 차량 신호로 판정하는 퀘스트 ID → 판정 종류 매핑. 여기 없는 ID 는 신호 판정 대상이 아니다. */
        private val QUEST_BY_ID = mapOf(
            "vehicle-1-0" to TurnSignalQuest.LEFT,   // 왼쪽 방향지시등
            "vehicle-1-1" to TurnSignalQuest.RIGHT,  // 오른쪽 방향지시등
            "vehicle-1-2" to TurnSignalQuest.OFF,    // 방향지시등 끄기
        )

        /** 이 퀘스트 ID 가 차량 신호로 자동 완료되는 대상인지. */
        fun isSignalQuest(questId: String): Boolean = QUEST_BY_ID.containsKey(questId)

        /** 퀘스트 ID 에 해당하는 판정 종류. 신호 대상이 아니면 null. */
        fun questKind(questId: String): TurnSignalQuest? = QUEST_BY_ID[questId]
    }
}
