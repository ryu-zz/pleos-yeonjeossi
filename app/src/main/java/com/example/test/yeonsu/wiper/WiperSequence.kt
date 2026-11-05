package com.example.test.yeonsu.wiper

/**
 * 와이퍼 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 두 종류를 하나의 상태 기계로 판정한다(WiperQuest).
 * - ON  (와이퍼 한 번, vehicle-2-0, "와이퍼를 한 번 작동"): 수행 중 ON 확인 시 완료.
 * - OFF (와이퍼 끄기, vehicle-2-4, "OFF 위치로"): 수행 중 OFF 확인 시 완료.
 *
 * 공통 규칙(상향등/창문 등과 동일):
 * - 시작 시 이미 목표 상태면 반대 상태를 한 번 거친 뒤 다시 목표가 되어야 완료(조작 연습형).
 * - null(불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 WiperQuestController 가 담당한다.
 */

/** 판정할 와이퍼 퀘스트 종류. */
enum class WiperQuest { ON, OFF }

enum class WiperStage { NEED_OPPOSITE_FIRST, WAIT_TARGET, DONE }

class WiperSequence(initialOn: Boolean?, private val quest: WiperQuest) {

    private fun isTarget(on: Boolean?): Boolean {
        if (on == null) return false
        return when (quest) {
            WiperQuest.ON -> on
            WiperQuest.OFF -> !on
        }
    }

    var stage: WiperStage =
        if (isTarget(initialOn)) WiperStage.NEED_OPPOSITE_FIRST else WiperStage.WAIT_TARGET
        private set

    val isDone: Boolean get() = stage == WiperStage.DONE

    /**
     * 작동 여부 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(on: Boolean?): Boolean {
        if (on == null) return false
        when (stage) {
            WiperStage.NEED_OPPOSITE_FIRST -> {
                if (!isTarget(on)) stage = WiperStage.WAIT_TARGET
            }
            WiperStage.WAIT_TARGET -> {
                if (isTarget(on)) {
                    stage = WiperStage.DONE
                    return true
                }
            }
            WiperStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    fun guideText(): String = when (quest) {
        WiperQuest.ON -> when (stage) {
            WiperStage.NEED_OPPOSITE_FIRST -> "와이퍼가 이미 작동 중이야. 한 번 껐다가 다시 작동해줘."
            WiperStage.WAIT_TARGET -> "와이퍼를 한 번 작동해줘."
            WiperStage.DONE -> "좋아, 와이퍼가 작동했어!"
        }
        WiperQuest.OFF -> when (stage) {
            WiperStage.NEED_OPPOSITE_FIRST -> "와이퍼가 이미 꺼져 있어. 한 번 켰다가 다시 꺼줘."
            WiperStage.WAIT_TARGET -> "와이퍼 레버를 OFF 위치로 돌려줘."
            WiperStage.DONE -> "좋아, 와이퍼를 껐어!"
        }
    }
}
