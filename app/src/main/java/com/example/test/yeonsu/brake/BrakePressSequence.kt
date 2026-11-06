package com.example.test.yeonsu.brake

/**
 * '브레이크 위치' 퀘스트(vehicle-0-5, "브레이크를 밟아보세요")의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 조작 연습형: 시작 이후 "눌림(pressed=true)"이 확인되면 완료한다.
 * - 시작 시 이미 밟고 있으면, 안내가 '밟아보기'이므로 바로 완료하지 않고
 *   한 번 뗐다가 다시 밟는 전이를 요구한다.
 * - 시작 시 떼져 있으면 밟으라고 안내하고, 눌림이 확인되면 완료한다.
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 BrakeQuestController 가 담당한다.
 */

enum class BrakeStage {
    /** 시작 시 이미 밟고 있어, 한 번 뗐다가 다시 밟아야 하는 단계. */
    NEED_RELEASE_FIRST,
    /** 눌림을 기다리는 단계. */
    WAIT_PRESS,
    /** 완료. */
    DONE
}

class BrakePressSequence(initial: Boolean?) {

    var stage: BrakeStage =
        if (initial == true) BrakeStage.NEED_RELEASE_FIRST else BrakeStage.WAIT_PRESS
        private set

    val isDone: Boolean get() = stage == BrakeStage.DONE

    /**
     * 눌림 여부 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(pressed: Boolean?): Boolean {
        if (pressed == null) return false
        when (stage) {
            BrakeStage.NEED_RELEASE_FIRST -> {
                if (!pressed) stage = BrakeStage.WAIT_PRESS
            }
            BrakeStage.WAIT_PRESS -> {
                if (pressed) {
                    stage = BrakeStage.DONE
                    return true
                }
            }
            BrakeStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    fun guideText(): String = when (stage) {
        BrakeStage.NEED_RELEASE_FIRST -> "브레이크를 이미 밟고 있어. 한 번 뗐다가 다시 밟아줘."
        BrakeStage.WAIT_PRESS -> "브레이크 페달을 밟아줘."
        BrakeStage.DONE -> "좋아, 이 페달이 브레이크야!"
    }
}
