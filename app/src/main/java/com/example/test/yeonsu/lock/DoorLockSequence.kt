package com.example.test.yeonsu.lock

/**
 * '전체 문 잠금' 퀘스트(vehicle-4-3)의 순서 판정 로직 (SDK 비의존, 순수 Kotlin).
 *
 * 상태 확인형: 시작 이후 "네 짝 모두 잠김(allLocked=true)"이 확인되면 완료한다.
 * - 시작 시 이미 모두 잠겨 있으면, 안내가 '잠금 버튼 누르기'이므로 바로 완료하지 않고
 *   한 번 풀렸다가(또는 한 짝이라도 열렸다가) 다시 모두 잠기는 전이를 요구한다.
 *   (조작 연습이 목적이므로 시작 상태만으로 완료하지 않는다.)
 * - 시작 시 하나라도 안 잠겨 있으면, 잠그라고 안내하고 전체 잠김이 확인되면 완료한다.
 * - null(상태 불명)은 완료로 취급하지 않는다(단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 DoorLockQuestController 가 담당한다.
 */

/** 순서 판정 단계. */
enum class LockStage {
    /** 시작 시 이미 모두 잠겨 있어, 한 번 풀렸다가 다시 잠겨야 하는 단계. */
    NEED_UNLOCK_FIRST,
    /** 전체 잠김을 기다리는 단계. */
    WAIT_LOCKED,
    /** 완료. */
    DONE
}

/**
 * @param initial 시작 시점의 전체 잠김 여부(true=모두 잠김).
 */
class DoorLockSequence(initial: Boolean?) {

    var stage: LockStage =
        if (initial == true) LockStage.NEED_UNLOCK_FIRST else LockStage.WAIT_LOCKED
        private set

    val isDone: Boolean get() = stage == LockStage.DONE

    /**
     * 전체 잠김 여부 하나를 받아 단계를 전이한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(allLocked: Boolean?): Boolean {
        if (allLocked == null) return false
        when (stage) {
            LockStage.NEED_UNLOCK_FIRST -> {
                // 한 짝이라도 풀린 상태를 한 번 확인하면 정상 순서 시작점으로.
                if (!allLocked) stage = LockStage.WAIT_LOCKED
            }
            LockStage.WAIT_LOCKED -> {
                if (allLocked) {
                    stage = LockStage.DONE
                    return true
                }
            }
            LockStage.DONE -> {
                // 완료 후 입력은 무시(중복 완료 방지).
            }
        }
        return false
    }

    /** 사용자에게 보여줄 현재 단계 안내 문구. */
    fun guideText(): String = when (stage) {
        LockStage.NEED_UNLOCK_FIRST -> "이미 모든 문이 잠겨 있어. 한 번 풀었다가 전체 문 잠금 버튼을 다시 눌러줘."
        LockStage.WAIT_LOCKED -> "전체 문 잠금 버튼을 눌러 모든 문을 잠가줘."
        LockStage.DONE -> "좋아, 모든 문을 잠갔어!"
    }
}
