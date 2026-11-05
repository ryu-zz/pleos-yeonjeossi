package com.example.test.yeonsu.signal

/**
 * 왼쪽 방향지시등 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 인정 순서: 시작 이후 확인된 OFF → LEFT → OFF 만 완료로 인정한다.
 * - 시작 시 LEFT이면 먼저 꺼야 하므로 NEED_OFF_FIRST 단계에서 대기한다.
 * - 같은 상태가 반복 수신되어도 다음 단계로 넘어가지 않는다(중복 무시).
 * - RIGHT가 들어오면 순서를 초기화하고 다시 OFF부터 확인한다.
 * - null은 완료로 취급하지 않는다(상태 불명으로 보고 단계 유지).
 *
 * 이 클래스는 "현재까지 확인된 신호"만 입력받아 단계를 전이한다.
 * SDK 연결/구독/오류 처리는 TurnSignalQuestController가 담당한다.
 */

/** 판정에 쓰는 방향지시등 값 (SDK enum과 1:1로 매핑되지만 테스트를 위해 독립적으로 둔다). */
enum class Signal { OFF, LEFT, RIGHT }

/** 순서 판정 단계. */
enum class SeqStage {
    /** 시작 시 이미 LEFT라 먼저 꺼야 하는 상태. OFF를 보면 WAIT_LEFT로 간다. */
    NEED_OFF_FIRST,
    /** OFF가 확인됨. LEFT를 기다린다. */
    WAIT_LEFT,
    /** LEFT가 확인됨. 다시 OFF(끄기)를 기다린다. */
    WAIT_OFF_AGAIN,
    /** OFF→LEFT→OFF 완료. */
    DONE
}

/**
 * 순서 판정 상태 기계. 스레드 안전성은 호출자(메인 스레드)가 보장한다.
 *
 * @param initial 시작 시점의 신호. LEFT면 NEED_OFF_FIRST(먼저 끄기 안내), 그 외(OFF/RIGHT/null)는 WAIT_LEFT.
 */
class TurnSignalSequence(initial: Signal?) {

    var stage: SeqStage = if (initial == Signal.LEFT) SeqStage.NEED_OFF_FIRST else SeqStage.WAIT_LEFT
        private set

    /** 완료 여부. */
    val isDone: Boolean get() = stage == SeqStage.DONE

    /**
     * 신호 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true(완료는 단 한 번만 true를 돌려준다).
     */
    fun onSignal(signal: Signal?): Boolean {
        // null = 상태 불명. 완료로 취급하지 않고 단계도 바꾸지 않는다.
        if (signal == null) return false

        // RIGHT는 어느 단계에서든 순서를 깬다 → OFF부터 다시(단, 이미 DONE이면 유지).
        if (signal == Signal.RIGHT) {
            if (stage != SeqStage.DONE) stage = SeqStage.WAIT_LEFT
            return false
        }

        when (stage) {
            SeqStage.NEED_OFF_FIRST -> {
                // 시작 시 LEFT였던 경우. OFF가 확인되면 정상 순서 시작점으로.
                if (signal == Signal.OFF) stage = SeqStage.WAIT_LEFT
                // LEFT 반복 수신은 무시(아직 한 번도 끄지 않았으므로).
            }
            SeqStage.WAIT_LEFT -> {
                // OFF 반복은 무시. LEFT가 와야 다음 단계.
                if (signal == Signal.LEFT) stage = SeqStage.WAIT_OFF_AGAIN
            }
            SeqStage.WAIT_OFF_AGAIN -> {
                // LEFT 반복은 무시. OFF가 와야 완료.
                if (signal == Signal.OFF) {
                    stage = SeqStage.DONE
                    return true
                }
            }
            SeqStage.DONE -> {
                // 완료 후 들어오는 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (stage) {
        SeqStage.NEED_OFF_FIRST -> "지금 왼쪽 깜빡이가 켜져 있어. 레버를 원위치로 돌려 먼저 꺼줘."
        SeqStage.WAIT_LEFT -> "레버를 아래로 내려서 왼쪽 깜빡이를 켜봐."
        SeqStage.WAIT_OFF_AGAIN -> "좋아, 켰어! 이제 레버를 원위치로 돌려 깜빡이를 꺼줘."
        SeqStage.DONE -> "완벽해. 켜고 끄기까지 확인했어!"
    }
}
