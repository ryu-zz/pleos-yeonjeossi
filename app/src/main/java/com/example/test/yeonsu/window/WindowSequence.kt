package com.example.test.yeonsu.window

/**
 * 운전석 창문 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 창문 위치(0=완전히 닫힘, 1~100=열림)를 받아 두 종류를 판정한다(WindowQuest).
 * - OPEN  (운전석 창문, vehicle-4-0, "운전석 창문을 조금 내려보세요."): 열림(position>0) 확인 시 완료.
 * - CLOSE (창문 닫기, vehicle-4-1, "운전석 창문을 완전히 닫아보세요."): 완전히 닫힘(position==0) 확인 시 완료.
 *
 * 공통 규칙:
 * - 시작 시 이미 목표 상태면, '필요한 조작'을 안내하고 반대 상태를 한 번 거쳐야 완료로 인정한다.
 *   · OPEN 인데 시작 시 이미 열려 있으면 "닫았다가 다시 내려줘"로 닫힘→열림 유도.
 *   · CLOSE 인데 시작 시 이미 닫혀 있으면 "열었다가 다시 닫아줘"로 열림→닫힘 유도.
 *   (안내 문구가 실제 조작을 요구하므로, 시작 상태만으로는 완료하지 않는다.)
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 WindowQuestController 가 담당한다.
 */

/** 판정할 창문 퀘스트 종류. */
enum class WindowQuest { OPEN, CLOSE }

/** 순서 판정 단계. */
enum class WindowStage {
    /** 시작 시 이미 목표 상태라, 반대 상태를 먼저 거쳐야 하는 단계. */
    NEED_OPPOSITE_FIRST,
    /** 목표 전이를 기다리는 단계. */
    WAIT_TARGET,
    /** 완료. */
    DONE
}

class WindowSequence(initial: Int?, private val quest: WindowQuest) {

    /** position 이 이 퀘스트의 "완료로 인정하는" 목표 상태인지. (null 은 목표 아님) */
    private fun isTarget(pos: Int?): Boolean {
        if (pos == null) return false
        return when (quest) {
            WindowQuest.OPEN -> pos > 0      // 조금이라도 열리면 열림
            WindowQuest.CLOSE -> pos == 0    // 완전히 닫힘
        }
    }

    var stage: WindowStage =
        if (isTarget(initial)) WindowStage.NEED_OPPOSITE_FIRST else WindowStage.WAIT_TARGET
        private set

    val isDone: Boolean get() = stage == WindowStage.DONE

    /**
     * 위치 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(position: Int?): Boolean {
        if (position == null) return false
        when (stage) {
            WindowStage.NEED_OPPOSITE_FIRST -> {
                // 시작 시 이미 목표였음. 반대 상태를 한 번 확인하면 정상 순서 시작점으로.
                if (!isTarget(position)) stage = WindowStage.WAIT_TARGET
            }
            WindowStage.WAIT_TARGET -> {
                if (isTarget(position)) {
                    stage = WindowStage.DONE
                    return true
                }
            }
            WindowStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (quest) {
        WindowQuest.OPEN -> when (stage) {
            WindowStage.NEED_OPPOSITE_FIRST -> "운전석 창문이 이미 열려 있어. 끝까지 닫았다가 다시 조금 내려줘."
            WindowStage.WAIT_TARGET -> "운전석 창문을 조금 내려줘."
            WindowStage.DONE -> "좋아, 창문을 열었어!"
        }
        WindowQuest.CLOSE -> when (stage) {
            WindowStage.NEED_OPPOSITE_FIRST -> "운전석 창문이 이미 닫혀 있어. 조금 내렸다가 다시 끝까지 닫아줘."
            WindowStage.WAIT_TARGET -> "운전석 창문을 끝까지 닫아줘."
            WindowStage.DONE -> "좋아, 창문을 닫았어!"
        }
    }
}
