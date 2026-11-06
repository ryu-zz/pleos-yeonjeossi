package com.example.test.yeonsu.polling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PollSequence(순서 판정 상태 기계) 단위 테스트.
 * 조작 연습형: 시작이 목표면 반대 상태를 거쳐야 완료. null 무시. 완료 1회.
 */
class PollSequenceTest {

    @Test
    fun 시작비목표_목표도달시_완료() {
        val s = PollSequence(initialIsTarget = false)
        assertEquals(PollStage.WAIT_TARGET, s.stage)
        assertTrue(s.onSignal(true))
        assertTrue(s.isDone)
    }

    @Test
    fun 시작이미목표_반대거쳐야_완료() {
        val s = PollSequence(initialIsTarget = true)
        assertEquals(PollStage.NEED_OPPOSITE_FIRST, s.stage)
        assertFalse(s.onSignal(true))                 // 계속 목표: 전이 없음
        assertEquals(PollStage.NEED_OPPOSITE_FIRST, s.stage)
        assertFalse(s.onSignal(false))                // 반대 상태 통과
        assertEquals(PollStage.WAIT_TARGET, s.stage)
        assertTrue(s.onSignal(true))                  // 다시 목표 → 완료
    }

    @Test
    fun null은_전이없음() {
        val s = PollSequence(initialIsTarget = false)
        assertFalse(s.onSignal(null))
        assertEquals(PollStage.WAIT_TARGET, s.stage)
    }

    @Test
    fun 완료후_추가신호는_false() {
        val s = PollSequence(initialIsTarget = false)
        assertTrue(s.onSignal(true))
        assertFalse(s.onSignal(true))
        assertFalse(s.onSignal(false))
    }

    @Test
    fun 시작null은_목표대기() {
        val s = PollSequence(initialIsTarget = null)
        assertEquals(PollStage.WAIT_TARGET, s.stage)
        assertTrue(s.onSignal(true))
    }
}
