package com.example.test.yeonsu.lock

import android.content.Context

/**
 * '전체 문 잠금' 퀘스트(vehicle-4-3)의 수신 제어부.
 *
 * 역할:
 * - DoorLockSource(기본: Vehicle SDK DoorLockState, 문 네 짝)를 구독하고,
 *   "전체 잠김 여부"를 순수 판정 로직(DoorLockSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(다른 신호 컨트롤러와 동일):
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false).
 * - null 은 완료로 취급하지 않는다.
 * - onError 이후에는 순서 판정을 초기화하고, 다음 수신부터 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 재진입(새 인스턴스)마다 새 Sequence 로 시작한다.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 DoorLockSource 주입) JVM 단위 테스트가 가능하다.
 */
class DoorLockQuestController(
    private val source: DoorLockSource,
    private val callback: Callback,
) {
    constructor(appContext: Context, callback: Callback) :
        this(VehicleDoorLockSource(appContext.applicationContext), callback)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: DoorLockSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : DoorLockSource.Observer {
        override fun onInitial(allLocked: Boolean?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = DoorLockSequence(allLocked)
                sequence = seq
                callback.onStageChanged(seq.guideText())
            }
        }
        override fun onUpdate(allLocked: Boolean?) = handle(allLocked)
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

    private fun handle(allLocked: Boolean?) {
        if (!active || completed) return
        val seq = sequence
        if (seq == null) {
            val s = DoorLockSequence(allLocked)
            sequence = s
            callback.onStageChanged(s.guideText())
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(allLocked)
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
        /** 이 컨트롤러가 담당하는 퀘스트 ID (전체 문 잠금). */
        const val QUEST_ID = "vehicle-4-3"

        /** 이 퀘스트 ID 가 문 잠금 신호로 자동 완료되는 대상인지. */
        fun isDoorLockQuest(questId: String): Boolean = questId == QUEST_ID
    }
}
