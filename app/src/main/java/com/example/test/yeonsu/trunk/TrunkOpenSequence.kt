package com.example.test.yeonsu.trunk

/**
 * '트렁크' 퀘스트(vehicle-4-4, "트렁크 버튼을 눌러보세요")의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 조작 연습형: 시작 이후 "열림(OPEN)"이 확인되면 완료한다.
 * - 시작 시 이미 열려 있으면, 안내가 '열어보기'이므로 바로 완료하지 않고
 *   한 번 닫혔다가 다시 열리는 전이를 요구한다.
 * - 시작 시 닫혀 있으면 열라고 안내하고, 열림이 확인되면 완료한다.
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 TrunkQuestController 가 담당한다.
 */

/** 순서 판정 단계. */
enum class TrunkStage {
    /** 시작 시 이미 열려 있어, 한 번 닫혔다가 다시 열려야 하는 단계. */
    NEED_CLOSE_FIRST,
    /** 열림을 기다리는 단계. */
    WAIT_OPEN,
    /** 완료. */
    DONE
}

class TrunkOpenSequence(initial: TrunkState?) {

    var stage: TrunkStage =
        if (initial == TrunkState.OPEN) TrunkStage.NEED_CLOSE_FIRST else TrunkStage.WAIT_OPEN
        private set

    val isDone: Boolean get() = stage == TrunkStage.DONE

    /**
     * 트렁크 상태 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(state: TrunkState?): Boolean {
        if (state == null) return false
        when (stage) {
            TrunkStage.NEED_CLOSE_FIRST -> {
                if (state == TrunkState.CLOSED) stage = TrunkStage.WAIT_OPEN
            }
            TrunkStage.WAIT_OPEN -> {
                if (state == TrunkState.OPEN) {
                    stage = TrunkStage.DONE
                    return true
                }
            }
            TrunkStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (stage) {
        TrunkStage.NEED_CLOSE_FIRST -> "트렁크가 이미 열려 있어. 한 번 닫았다가 다시 열어줘."
        TrunkStage.WAIT_OPEN -> "뒤쪽에 사람이 없는지 확인하고 트렁크를 열어줘."
        TrunkStage.DONE -> "좋아, 트렁크를 열었어!"
    }
}
