package com.example.test.yeonsu.signal

/**
 * 방향지시등 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 하나의 상태 기계로 세 가지 퀘스트를 판정한다(TurnSignalQuest).
 * - LEFT  (왼쪽 켜고 끄기): 시작 이후 OFF → LEFT → OFF 만 완료.
 * - RIGHT (오른쪽 켜고 끄기): 시작 이후 OFF → RIGHT → OFF 만 완료.
 * - OFF   (방향지시등 끄기): 시작 이후 LEFT/RIGHT(켜짐) 확인 후 OFF 가 오면 완료.
 *
 * 공통 규칙:
 * - 같은 상태가 반복 수신되어도 다음 단계로 넘어가지 않는다(중복 무시).
 * - null 은 완료로 취급하지 않는다(상태 불명으로 보고 단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * 방향 퀘스트(LEFT/RIGHT) 세부:
 * - 시작 시 목표 방향이 이미 켜져 있으면 먼저 꺼야 하므로 NEED_OFF_FIRST 에서 대기한다.
 * - 반대 방향이 들어오면 순서를 초기화하고 다시 OFF(WAIT_LEFT)부터 확인한다.
 *
 * 끄기 퀘스트(OFF) 세부:
 * - 시작 시 이미 켜져 있으면(LEFT/RIGHT) 바로 끄도록 WAIT_OFF 에서 대기한다.
 * - 처음부터 OFF(또는 불명)면 완료하지 않고 먼저 켜도록 NEED_ON 에서 대기한다.
 *
 * 이 클래스는 "현재까지 확인된 신호"만 입력받아 단계를 전이한다.
 * SDK 연결/구독/오류 처리는 TurnSignalQuestController 가 담당한다.
 */

/** 판정에 쓰는 방향지시등 값 (SDK enum과 1:1로 매핑되지만 테스트를 위해 독립적으로 둔다). */
enum class Signal { OFF, LEFT, RIGHT }

/** 이 상태 기계가 판정하는 퀘스트 종류. */
enum class TurnSignalQuest { LEFT, RIGHT, OFF }

/** 순서 판정 단계. */
enum class SeqStage {
    /** 방향 퀘스트: 시작 시 이미 목표 방향이라 먼저 꺼야 하는 상태. OFF 를 보면 WAIT_LEFT 로. */
    NEED_OFF_FIRST,
    /**
     * 방향 퀘스트: OFF 가 확인됨. 목표 방향 켜기를 기다린다.
     * (기존 왼쪽 퀘스트 때부터 쓰던 이름이라 유지한다. 오른쪽 퀘스트에서도 "목표 방향 켜기 대기"를 뜻한다.)
     */
    WAIT_LEFT,
    /** 방향 퀘스트: 목표 방향이 확인됨. 다시 OFF(끄기)를 기다린다. */
    WAIT_OFF_AGAIN,
    /** 끄기 퀘스트: 아직 꺼져 있어 먼저 켜야 하는 상태. LEFT/RIGHT 를 보면 WAIT_OFF 로. */
    NEED_ON,
    /** 끄기 퀘스트: 켜짐이 확인됨. OFF(끄기)를 기다린다. */
    WAIT_OFF,
    /** 완료. */
    DONE
}

/**
 * 순서 판정 상태 기계. 스레드 안전성은 호출자(메인 스레드)가 보장한다.
 *
 * @param initial 시작 시점의 신호.
 * @param quest 판정할 퀘스트 종류. 지정하지 않으면 기존 동작(LEFT)과 동일하다(하위호환).
 */
class TurnSignalSequence(initial: Signal?, private val quest: TurnSignalQuest = TurnSignalQuest.LEFT) {

    /** 방향 퀘스트에서 "켜야 하는" 목표 방향(LEFT/RIGHT). 끄기 퀘스트에서는 사용하지 않는다. */
    private val target: Signal? = when (quest) {
        TurnSignalQuest.LEFT -> Signal.LEFT
        TurnSignalQuest.RIGHT -> Signal.RIGHT
        TurnSignalQuest.OFF -> null
    }

    var stage: SeqStage = initialStage(initial)
        private set

    private fun initialStage(initial: Signal?): SeqStage = when (quest) {
        TurnSignalQuest.LEFT, TurnSignalQuest.RIGHT ->
            // 시작값이 목표 방향이면 먼저 끄기, 그 외(OFF/반대방향/null)는 바로 WAIT_LEFT.
            if (initial == target) SeqStage.NEED_OFF_FIRST else SeqStage.WAIT_LEFT
        TurnSignalQuest.OFF ->
            // 시작 시 이미 켜져 있으면 바로 끄기(WAIT_OFF), 꺼져 있거나 불명이면 먼저 켜기(NEED_ON).
            if (initial == Signal.LEFT || initial == Signal.RIGHT) SeqStage.WAIT_OFF else SeqStage.NEED_ON
    }

    /** 완료 여부. */
    val isDone: Boolean get() = stage == SeqStage.DONE

    /**
     * 신호 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true(완료는 단 한 번만 true 를 돌려준다).
     */
    fun onSignal(signal: Signal?): Boolean {
        // null = 상태 불명. 완료로 취급하지 않고 단계도 바꾸지 않는다.
        if (signal == null) return false
        return when (quest) {
            TurnSignalQuest.LEFT, TurnSignalQuest.RIGHT -> onDirectional(signal)
            TurnSignalQuest.OFF -> onOff(signal)
        }
    }

    /** LEFT/RIGHT 공통: OFF → 목표방향 → OFF. 반대 방향은 순서를 깬다. */
    private fun onDirectional(signal: Signal): Boolean {
        val opposite = if (target == Signal.LEFT) Signal.RIGHT else Signal.LEFT
        // 반대 방향은 어느 단계에서든 순서를 깬다 → OFF(WAIT_LEFT)부터 다시(단, 이미 DONE 이면 유지).
        if (signal == opposite) {
            if (stage != SeqStage.DONE) stage = SeqStage.WAIT_LEFT
            return false
        }
        when (stage) {
            SeqStage.NEED_OFF_FIRST -> {
                // 시작 시 목표 방향이었던 경우. OFF 가 확인되면 정상 순서 시작점으로.
                if (signal == Signal.OFF) stage = SeqStage.WAIT_LEFT
                // 목표 방향 반복 수신은 무시(아직 한 번도 끄지 않았으므로).
            }
            SeqStage.WAIT_LEFT -> {
                // OFF 반복은 무시. 목표 방향이 와야 다음 단계.
                if (signal == target) stage = SeqStage.WAIT_OFF_AGAIN
            }
            SeqStage.WAIT_OFF_AGAIN -> {
                // 목표 방향 반복은 무시. OFF 가 와야 완료.
                if (signal == Signal.OFF) {
                    stage = SeqStage.DONE
                    return true
                }
            }
            else -> {
                // DONE 등: 완료 후 들어오는 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 끄기 퀘스트: 켜짐(LEFT/RIGHT) 확인 후 OFF 면 완료. */
    private fun onOff(signal: Signal): Boolean {
        when (stage) {
            SeqStage.NEED_ON -> {
                // 아직 꺼져 있음. 켜짐(LEFT/RIGHT)이 확인되면 끄기 대기로.
                if (signal == Signal.LEFT || signal == Signal.RIGHT) stage = SeqStage.WAIT_OFF
                // OFF 반복은 무시(처음부터 OFF 면 완료하지 않음).
            }
            SeqStage.WAIT_OFF -> {
                // 켜짐 상태 반복/방향 전환(LEFT↔RIGHT)은 여전히 켜짐 → 유지. OFF 가 와야 완료.
                if (signal == Signal.OFF) {
                    stage = SeqStage.DONE
                    return true
                }
            }
            else -> {
                // DONE 등: 완료 후 입력은 무시.
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (stage) {
        SeqStage.NEED_OFF_FIRST -> when (quest) {
            TurnSignalQuest.RIGHT -> "지금 오른쪽 깜빡이가 켜져 있어. 레버를 원위치로 돌려 먼저 꺼줘."
            else -> "지금 왼쪽 깜빡이가 켜져 있어. 레버를 원위치로 돌려 먼저 꺼줘."
        }
        SeqStage.WAIT_LEFT -> when (quest) {
            TurnSignalQuest.RIGHT -> "레버를 위로 올려서 오른쪽 깜빡이를 켜봐."
            else -> "레버를 아래로 내려서 왼쪽 깜빡이를 켜봐."
        }
        SeqStage.WAIT_OFF_AGAIN -> "좋아, 켰어! 이제 레버를 원위치로 돌려 깜빡이를 꺼줘."
        SeqStage.NEED_ON -> "먼저 깜빡이를 켜볼까? 레버를 위나 아래로 움직여봐."
        SeqStage.WAIT_OFF -> "지금 깜빡이가 켜져 있어. 레버를 원위치로 돌려 꺼줘."
        SeqStage.DONE -> "완벽해. 확인했어!"
    }
}
