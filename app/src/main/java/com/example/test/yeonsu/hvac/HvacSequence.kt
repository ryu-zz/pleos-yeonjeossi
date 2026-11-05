package com.example.test.yeonsu.hvac

/**
 * 공조·시야 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 수신원이 올린 "목표 조건 충족 여부(isTarget: Boolean?)"만 받아 판정한다.
 *
 * 두 가지 판정형(HvacQuest.requiresTransition):
 * - 상태 확인형(온도 22도, 풍량 3단계): 시작 시 이미 목표면 바로 완료, 아니면 목표 도달 시 완료.
 * - 조작 연습형(그 외): 시작 시 이미 목표면 반대 상태를 한 번 거친 뒤 다시 목표가 되어야 완료.
 *   (안내가 실제 '조작'을 요구하므로 시작 상태만으로 완료하지 않는다.)
 *
 * 공통 규칙:
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 HvacQuestController 가 담당한다.
 */

enum class HvacStage {
    /** (조작 연습형) 시작 시 이미 목표라 반대 상태를 먼저 거쳐야 하는 단계. */
    NEED_OPPOSITE_FIRST,
    /** 목표 도달을 기다리는 단계. */
    WAIT_TARGET,
    /** 완료. */
    DONE
}

class HvacSequence(initialIsTarget: Boolean?, private val requiresTransition: Boolean) {

    var stage: HvacStage = when {
        initialIsTarget == true && requiresTransition -> HvacStage.NEED_OPPOSITE_FIRST
        initialIsTarget == true && !requiresTransition -> HvacStage.DONE
        else -> HvacStage.WAIT_TARGET
    }
        private set

    val isDone: Boolean get() = stage == HvacStage.DONE

    /**
     * 목표 충족 여부 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     *
     * 주의: 생성자에서 이미 DONE(상태 확인형 + 시작 시 목표)이 된 경우의 통지는
     *       컨트롤러가 onInitial 처리에서 담당한다. 여기서는 입력으로 새로 완료된 경우만 true.
     */
    fun onSignal(isTarget: Boolean?): Boolean {
        if (isTarget == null) return false
        when (stage) {
            HvacStage.NEED_OPPOSITE_FIRST -> {
                if (!isTarget) stage = HvacStage.WAIT_TARGET
            }
            HvacStage.WAIT_TARGET -> {
                if (isTarget) {
                    stage = HvacStage.DONE
                    return true
                }
            }
            HvacStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }
}
