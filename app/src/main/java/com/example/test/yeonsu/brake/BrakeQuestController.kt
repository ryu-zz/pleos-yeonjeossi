package com.example.test.yeonsu.brake

import android.content.Context

/**
 * '브레이크 위치' 퀘스트(vehicle-0-5)의 수신 제어부.
 *
 * 역할:
 * - BrakeSource(기본: Vehicle SDK Brake pedal status)를 구독하고,
 *   수신 상태를 순수 판정 로직(BrakePressSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(다른 신호 컨트롤러와 동일):
 * - stop() 이후 콜백 무시(active=false). null 은 완료 아님.
 * - onError 이후 판정 초기화, 다음 수신부터 재확인. 완료는 1회만 통지.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 BrakeSource 주입) JVM 단위 테스트가 가능하다.
 */
class BrakeQuestController(
    private val source: BrakeSource,
    private val callback: Callback,
) {
    constructor(appContext: Context, callback: Callback) :
        this(VehicleBrakeSource(appContext.applicationContext), callback)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: BrakePressSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : BrakeSource.Observer {
        override fun onInitial(pressed: Boolean?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = BrakePressSequence(pressed)
                sequence = seq
                callback.onStageChanged(seq.guideText())
            }
        }
        override fun onUpdate(pressed: Boolean?) = handle(pressed)
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

    private fun handle(pressed: Boolean?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            val s = BrakePressSequence(pressed)
            sequence = s
            callback.onStageChanged(s.guideText())
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(pressed)
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
        /** 이 컨트롤러가 담당하는 퀘스트 ID (브레이크 위치). */
        const val QUEST_ID = "vehicle-0-5"

        fun isBrakeQuest(questId: String): Boolean = questId == QUEST_ID
    }
}
