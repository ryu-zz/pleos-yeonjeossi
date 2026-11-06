package com.example.test.yeonsu.polling

/**
 * 폴링 퀘스트의 순서 판정 로직 (SDK 비의존, 순수 Kotlin → JVM 단위 테스트 가능).
 *
 * 수신원이 올린 "현재 조회값이 목표인가(isTarget: Boolean?)"만 받아 판정한다.
 * null = 조회 불가/불명(완료로 취급하지 않음).
 *
 * 모든 폴링 퀘스트는 조작 연습형이다:
 * - 시작 시 이미 목표값이면 NEED_OPPOSITE_FIRST 에서 시작 → 비목표 값을 한 번 거쳐야 WAIT_TARGET 으로 간다.
 * - WAIT_TARGET 에서 목표값이 조회되면 완료(DONE).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다(중복 완료 방지).
 *
 * SDK 연결/조회/주차 게이트/오류 처리는 PolledQuestController·VehiclePolledSource 가 담당한다.
 */
enum class PollStage { NEED_OPPOSITE_FIRST, WAIT_TARGET, DONE }

class PollSequence(initialIsTarget: Boolean?) {

    var stage: PollStage =
        if (initialIsTarget == true) PollStage.NEED_OPPOSITE_FIRST else PollStage.WAIT_TARGET
        private set

    val isDone: Boolean get() = stage == PollStage.DONE

    /**
     * 조회 결과(목표 여부) 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(isTarget: Boolean?): Boolean {
        if (isTarget == null) return false
        when (stage) {
            PollStage.NEED_OPPOSITE_FIRST -> {
                if (!isTarget) stage = PollStage.WAIT_TARGET
            }
            PollStage.WAIT_TARGET -> {
                if (isTarget) {
                    stage = PollStage.DONE
                    return true
                }
            }
            PollStage.DONE -> {
                // 완료 후 입력은 무시.
            }
        }
        return false
    }
}
