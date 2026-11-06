package com.example.test.yeonsu.hvac

import android.content.Context

/**
 * 공조·시야 퀘스트(cat5)의 수신 제어부.
 *
 * 역할:
 * - HvacSource(기본: Vehicle SDK)를 구독하고, 수신한 "목표 충족 여부"를 순수 판정 로직(HvacSequence)에 넘긴다.
 * - 단계 변화/오류/완료를 콜백으로 화면에 알린다.
 *
 * 안전 규칙(다른 신호 컨트롤러와 동일):
 * - stop() 이후 도착하는 콜백은 모두 무시한다(active=false).
 * - null 은 완료로 취급하지 않는다.
 * - onError 이후에는 순서 판정을 초기화하고, 다음 수신부터 다시 확인한다.
 * - 완료는 수행 1회당 정확히 한 번만 통지한다(completed 플래그).
 * - 재진입(새 인스턴스)마다 새 Sequence 로 시작한다.
 *
 * 상태 확인형(온도/풍량)은 시작 시 이미 목표면 onInitial 에서 바로 완료한다.
 *
 * SDK 에 직접 의존하지 않으므로(가짜 HvacSource 주입) JVM 단위 테스트가 가능하다.
 */
class HvacQuestController(
    private val source: HvacSource,
    private val callback: Callback,
    private val quest: HvacQuest,
) {
    constructor(appContext: Context, callback: Callback, quest: HvacQuest) :
        this(VehicleHvacSource(appContext.applicationContext, quest), callback, quest)

    interface Callback {
        fun onStageChanged(guide: String)
        fun onCompleted()
        fun onError(reason: String)
    }

    private var sequence: HvacSequence? = null
    private var active = false
    private var completed = false

    private val observer = object : HvacSource.Observer {
        override fun onInitial(isTarget: Boolean?) {
            if (!active || completed) return
            if (sequence == null) {
                val seq = HvacSequence(isTarget, quest.requiresTransition)
                sequence = seq
                callback.onStageChanged(guideText(seq))
                if (seq.isDone) complete()
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
            val s = HvacSequence(isTarget, quest.requiresTransition)
            sequence = s
            callback.onStageChanged(guideText(s))
            if (s.isDone) complete()
            return
        }
        val before = seq.stage
        val justDone = seq.onSignal(isTarget)
        if (seq.stage != before) callback.onStageChanged(guideText(seq))
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

    /** 단계별 안내 문구. 퀘스트 종류에 맞춰 조작을 안내한다. */
    private fun guideText(seq: HvacSequence): String {
        val done = "좋아, 확인했어!"
        if (seq.stage == HvacStage.DONE) return when (quest) {
            HvacQuest.AC_ON -> "좋아, 에어컨을 켰어!"
            HvacQuest.TEMP_22 -> "좋아, 22도로 맞췄어!"
            HvacQuest.FAN_3 -> "좋아, 풍량을 3단계로 맞췄어!"
            HvacQuest.RECIRC_ON -> "좋아, 내기순환을 켰어!"
            HvacQuest.RECIRC_OFF -> "좋아, 외기 모드로 바꿨어!"
            HvacQuest.DEFROST_ON -> "좋아, 성에 제거를 켰어!"
            HvacQuest.MIRROR_HEAT_ON -> "좋아, 사이드미러 열선을 켰어!"
        }
        val needOpposite = seq.stage == HvacStage.NEED_OPPOSITE_FIRST
        return when (quest) {
            HvacQuest.AC_ON -> if (needOpposite) "에어컨이 이미 켜져 있어. 한 번 껐다가 A/C 버튼을 다시 눌러줘." else "A/C 버튼을 눌러 에어컨을 켜줘."
            HvacQuest.TEMP_22 -> "운전석 온도를 22도로 맞춰줘."
            HvacQuest.FAN_3 -> "풍량을 3단계로 맞춰줘."
            HvacQuest.RECIRC_ON -> if (needOpposite) "내기순환이 이미 켜져 있어. 한 번 껐다가 다시 켜줘." else "내기순환 버튼을 눌러줘."
            HvacQuest.RECIRC_OFF -> if (needOpposite) "이미 외기 모드야. 내기순환을 한 번 켰다가 다시 외기로 바꿔줘." else "내기순환을 꺼서 외기 모드로 바꿔줘."
            HvacQuest.DEFROST_ON -> if (needOpposite) "성에 제거가 이미 켜져 있어. 한 번 껐다가 앞유리 버튼을 다시 눌러줘." else "앞유리 성에 제거 버튼을 눌러줘."
            HvacQuest.MIRROR_HEAT_ON -> if (needOpposite) "사이드미러 열선이 이미 켜져 있어. 한 번 껐다가 다시 켜줘." else "사이드미러 열선을 켜줘."
        }
    }

    companion object {
        fun isHvacQuest(questId: String): Boolean = HvacQuest.isHvacQuest(questId)
        fun questKind(questId: String): HvacQuest? = HvacQuest.questKind(questId)
    }
}
