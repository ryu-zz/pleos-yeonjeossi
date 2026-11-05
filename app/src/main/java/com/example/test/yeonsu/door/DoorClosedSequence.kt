package com.example.test.yeonsu.door

/**
 * 운전석 '문 확인' 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 인정 규칙: 시작 이후 "닫힘(CLOSED)"이 확인되면 완료한다.
 * - 시작 시 이미 닫혀 있으면 바로 완료한다.
 * - 시작 시 열려 있으면 닫으라고 안내하고, 닫힘이 확인되면 완료한다.
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 DoorQuestController 가 담당한다.
 */

/** 순서 판정 단계. */
enum class DoorStage {
    /** 열려 있어 닫기를 기다리는 상태. */
    WAIT_CLOSED,
    /** 닫힘이 확인됨. */
    DONE
}

/**
 * 순서 판정 상태 기계. 스레드 안전성은 호출자(메인 스레드)가 보장한다.
 *
 * @param initial 시작 시점의 문 상태. CLOSED 면 바로 DONE, 그 외(OPEN/null)는 WAIT_CLOSED.
 */
class DoorClosedSequence(initial: DoorState?) {

    var stage: DoorStage = if (initial == DoorState.CLOSED) DoorStage.DONE else DoorStage.WAIT_CLOSED
        private set

    /** 완료 여부. */
    val isDone: Boolean get() = stage == DoorStage.DONE

    /**
     * 신호 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true(완료는 단 한 번만 true 를 돌려준다).
     *
     * 주의: 생성자에서 이미 DONE(시작 시 닫힘)이 된 경우, 그 완료 통지는 컨트롤러가
     *       onInitial 처리에서 담당한다. 여기서는 "입력으로 새로" 완료된 경우만 true 를 돌려준다.
     */
    fun onSignal(state: DoorState?): Boolean {
        // null = 상태 불명. 완료로 취급하지 않고 단계도 바꾸지 않는다.
        if (state == null) return false
        when (stage) {
            DoorStage.WAIT_CLOSED -> {
                if (state == DoorState.CLOSED) {
                    stage = DoorStage.DONE
                    return true
                }
                // OPEN 반복은 무시(계속 닫기 대기).
            }
            DoorStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (stage) {
        DoorStage.WAIT_CLOSED -> "운전석 문이 열려 있어. 문을 닫아줘."
        DoorStage.DONE -> "좋아, 문이 닫혔어!"
    }
}
