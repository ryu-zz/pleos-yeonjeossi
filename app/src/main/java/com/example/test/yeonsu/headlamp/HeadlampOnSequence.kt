package com.example.test.yeonsu.headlamp

/**
 * '하향등' 퀘스트(vehicle-3-1, "전조등을 켜보세요.")의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 인정 규칙: 시작 이후 하향등 "켜짐(ON)"이 확인되면 완료한다.
 * - 시작 시 이미 켜져 있으면 바로 완료한다.
 * - 꺼져 있으면 켜라고 안내하고, 켜짐이 확인되면 완료한다.
 * - 주간주행등(DAYTIME_RUNNING)은 하향등 켜짐으로 인정하지 않는다(미완료, 켜기 안내 유지).
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 HeadlampQuestController 가 담당한다.
 */

/** 순서 판정 단계. */
enum class HeadlampStage {
    /** 꺼짐/주간주행등 등 아직 하향등이 켜지지 않아 켜기를 기다리는 상태. */
    WAIT_ON,
    /** 하향등 켜짐이 확인됨. */
    DONE
}

/**
 * 순서 판정 상태 기계. 스레드 안전성은 호출자(메인 스레드)가 보장한다.
 *
 * @param initial 시작 시점의 하향등 상태. ON 이면 바로 DONE, 그 외(OFF/DAYTIME_RUNNING/null)는 WAIT_ON.
 */
class HeadlampOnSequence(initial: HeadlampState?) {

    var stage: HeadlampStage = if (initial == HeadlampState.ON) HeadlampStage.DONE else HeadlampStage.WAIT_ON
        private set

    /** 완료 여부. */
    val isDone: Boolean get() = stage == HeadlampStage.DONE

    /**
     * 신호 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true(완료는 단 한 번만 true 를 돌려준다).
     *
     * 주의: 생성자에서 이미 DONE(시작 시 켜짐)이 된 경우, 그 완료 통지는 컨트롤러가
     *       onInitial 처리에서 담당한다. 여기서는 "입력으로 새로" 완료된 경우만 true 를 돌려준다.
     */
    fun onSignal(state: HeadlampState?): Boolean {
        // null = 상태 불명. 완료로 취급하지 않고 단계도 바꾸지 않는다.
        if (state == null) return false
        when (stage) {
            HeadlampStage.WAIT_ON -> {
                if (state == HeadlampState.ON) {
                    stage = HeadlampStage.DONE
                    return true
                }
                // OFF/DAYTIME_RUNNING 은 하향등 켜짐이 아님 → 계속 대기.
            }
            HeadlampStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (stage) {
        HeadlampStage.WAIT_ON -> "전조등(하향등)을 켜줘. 레버를 돌려 전조등을 켜봐."
        HeadlampStage.DONE -> "좋아, 전조등을 켰어!"
    }
}
