package com.example.test.yeonsu.highbeam

/**
 * 상향등 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 두 종류를 하나의 상태 기계로 판정한다(HighBeamQuest).
 * - ON  (상향등, vehicle-3-2, "레버를 밀어 상향등을 켜보세요."): 수행 중 ON 확인 시 완료.
 * - OFF (상향등 끄기, vehicle-3-3, "상향등을 꺼주세요."): 수행 중 OFF 확인 시 완료.
 *
 * 공통 규칙:
 * - 시작 시 이미 목표 상태면 '필요한 조작'을 안내한다(바로 완료하지 않는다).
 *   · ON 퀘스트인데 시작 시 이미 켜져 있으면 "껐다가 다시 켜줘"로 OFF→ON 을 유도.
 *   · OFF 퀘스트인데 시작 시 이미 꺼져 있으면 "켰다가 다시 꺼줘"로 ON→OFF 를 유도.
 *   (요구사항: 켜기는 수행 중 OFF→ON, 끄기는 수행 중 ON→OFF 에서 완료)
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - DAYTIME_RUNNING 은 ON 으로 인정하지 않는다.
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 HighBeamQuestController 가 담당한다.
 */

/** 판정할 상향등 퀘스트 종류. */
enum class HighBeamQuest { ON, OFF }

/** 순서 판정 단계. */
enum class HighBeamStage {
    /** 시작 시 이미 목표 상태라, 반대 상태를 먼저 거쳐야 하는 단계. */
    NEED_OPPOSITE_FIRST,
    /** 목표 전이를 기다리는 단계(ON 퀘스트는 ON 대기, OFF 퀘스트는 OFF 대기). */
    WAIT_TARGET,
    /** 완료. */
    DONE
}

/**
 * @param initial 시작 시점의 상향등 상태.
 * @param quest 판정할 종류(ON=켜기, OFF=끄기).
 */
class HighBeamSequence(initial: HighBeamState?, private val quest: HighBeamQuest) {

    /** 이 퀘스트가 "완료로 인정하는" 목표 상태인지. */
    private fun isTarget(s: HighBeamState?): Boolean = when (quest) {
        HighBeamQuest.ON -> s == HighBeamState.ON
        HighBeamQuest.OFF -> s == HighBeamState.OFF
    }

    var stage: HighBeamStage =
        if (isTarget(initial)) HighBeamStage.NEED_OPPOSITE_FIRST else HighBeamStage.WAIT_TARGET
        private set

    val isDone: Boolean get() = stage == HighBeamStage.DONE

    /**
     * 신호 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(state: HighBeamState?): Boolean {
        if (state == null) return false
        when (stage) {
            HighBeamStage.NEED_OPPOSITE_FIRST -> {
                // 시작 시 이미 목표였음. 반대 상태를 한 번 확인하면 정상 순서 시작점으로.
                if (!isTarget(state)) stage = HighBeamStage.WAIT_TARGET
                // 목표 상태 반복은 무시(아직 반대로 간 적 없음).
            }
            HighBeamStage.WAIT_TARGET -> {
                if (isTarget(state)) {
                    stage = HighBeamStage.DONE
                    return true
                }
                // 비목표 상태 반복은 무시.
            }
            HighBeamStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (quest) {
        HighBeamQuest.ON -> when (stage) {
            HighBeamStage.NEED_OPPOSITE_FIRST -> "상향등이 이미 켜져 있어. 레버를 원위치로 껐다가 다시 밀어서 켜줘."
            HighBeamStage.WAIT_TARGET -> "레버를 앞으로 밀어서 상향등을 켜줘."
            HighBeamStage.DONE -> "좋아, 상향등을 켰어!"
        }
        HighBeamQuest.OFF -> when (stage) {
            HighBeamStage.NEED_OPPOSITE_FIRST -> "상향등이 이미 꺼져 있어. 한 번 켰다가 다시 꺼줘."
            HighBeamStage.WAIT_TARGET -> "레버를 원위치로 돌려서 상향등을 꺼줘."
            HighBeamStage.DONE -> "좋아, 상향등을 껐어!"
        }
    }
}
