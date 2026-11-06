package com.example.test.yeonsu.wiper

import android.content.Context

/**
 * 와이퍼 퀘스트(와이퍼 한 번 vehicle-2-0 / 와이퍼 끄기 vehicle-2-4)의 수신 제어부.
 *
 * 역할:
 * - WiperSource(기본: Vehicle SDK WiperState, 앞유리)를 구독하고, 작동 여부를 순수 판정 로직(WiperSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(다른 신호 컨트롤러와 동일):
 * - stop() 이후 콜백 무시(active=false). null 은 완료 아님.
 * - onError 이후 판정 초기화, 다음 수신부터 재확인. 완료는 1회만 통지.
 *
 * 자동/저속/고속 와이퍼(2-1/2-2/2-3)는 단계(WiperSwitch)를 상태 변경으로 받을 수 없어(SDK register 없음)
 * 이 컨트롤러의 자동 판정 대상이 아니다(보류).
 *
 * SDK 에 직접 의존하지 않으므로(가짜 WiperSource 주입) JVM 단위 테스트가 가능하다.
 */
class WiperQuestController(
    private val source: WiperSource,
    private val callback: Callback,
    private val quest: WiperQuest,
) {
    constructor(appContext: Context, callback: Callback, quest: WiperQuest) :
        this(VehicleWiperSource(appContext.applicationContext), callback, quest)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: WiperSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : WiperSource.Observer {
        override fun onInitial(on: Boolean?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = WiperSequence(on, quest)
                sequence = seq
                callback.onStageChanged(seq.guideText())
            }
        }
        override fun onUpdate(on: Boolean?) = handle(on)
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

    private fun handle(on: Boolean?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            val s = WiperSequence(on, quest)
            sequence = s
            callback.onStageChanged(s.guideText())
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(on)
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
        /** 와이퍼 끄기 퀘스트 ID. */
        const val QUEST_ID_OFF = "vehicle-2-4"

        // 와이퍼 한 번(vehicle-2-0)은 자동 판정 대상에서 제외한다.
        // 플랫폼 와이퍼 상태는 OFF/ON 만 구분하고(Low/High/Auto 가 모두 ON), '한 번 단발' 조작을
        // 연속 작동과 구분할 수 없어 연속 작동만으로도 완료될 수 있기 때문이다.
        // 끄기(ON→OFF)는 단발 구분이 필요 없으므로 자동 판정을 유지한다.
        private val BY_ID = mapOf(
            QUEST_ID_OFF to WiperQuest.OFF,
        )

        fun isWiperQuest(questId: String): Boolean = BY_ID.containsKey(questId)
        fun questKind(questId: String): WiperQuest? = BY_ID[questId]
    }
}
