package com.example.test.yeonsu.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 왼쪽 방향지시등 순서 판정(TurnSignalSequence) 단위 테스트.
 * SDK 비의존 순수 로직이므로 JVM 테스트로 바로 돌아간다.
 */
class TurnSignalSequenceTest {

    /** 신호 목록을 순서대로 주입하고, "새로 완료된" 횟수를 센다. */
    private fun feed(initial: Signal?, vararg signals: Signal?): Pair<TurnSignalSequence, Int> {
        val seq = TurnSignalSequence(initial)
        var completions = 0
        for (s in signals) if (seq.onSignal(s)) completions++
        return seq to completions
    }

    @Test
    fun 정상순서_OFF에서시작_LEFT_OFF_완료() {
        // 시작 시 OFF → LEFT → OFF
        val (seq, done) = feed(Signal.OFF, Signal.LEFT, Signal.OFF)
        assertTrue(seq.isDone)
        assertEquals(1, done)
    }

    @Test
    fun 시작시_LEFT면_먼저꺼야_완료된다() {
        val seq = TurnSignalSequence(Signal.LEFT)
        assertEquals(SeqStage.NEED_OFF_FIRST, seq.stage)
        // 아직 켜진 상태라 LEFT 반복은 완료로 안 감
        assertFalse(seq.onSignal(Signal.LEFT))
        // 먼저 끄기(OFF) → 이제 WAIT_LEFT
        assertFalse(seq.onSignal(Signal.OFF))
        assertEquals(SeqStage.WAIT_LEFT, seq.stage)
        // 다시 LEFT → OFF 로 완료
        assertFalse(seq.onSignal(Signal.LEFT))
        assertTrue(seq.onSignal(Signal.OFF))
        assertTrue(seq.isDone)
    }

    @Test
    fun RIGHT가_들어오면_순서초기화_후_다시OFF부터() {
        val seq = TurnSignalSequence(Signal.OFF)
        assertFalse(seq.onSignal(Signal.LEFT))          // WAIT_OFF_AGAIN
        assertEquals(SeqStage.WAIT_OFF_AGAIN, seq.stage)
        assertFalse(seq.onSignal(Signal.RIGHT))          // 초기화 → WAIT_LEFT
        assertEquals(SeqStage.WAIT_LEFT, seq.stage)
        // 여기서 바로 OFF가 와도 완료 아님(LEFT를 아직 안 거침)
        assertFalse(seq.onSignal(Signal.OFF))
        // 정상 순서 다시: LEFT → OFF
        assertFalse(seq.onSignal(Signal.LEFT))
        assertTrue(seq.onSignal(Signal.OFF))
    }

    @Test
    fun 중복이벤트는_다음단계로_넘어가지않는다() {
        val seq = TurnSignalSequence(Signal.OFF)
        // OFF 여러 번
        assertFalse(seq.onSignal(Signal.OFF))
        assertFalse(seq.onSignal(Signal.OFF))
        assertEquals(SeqStage.WAIT_LEFT, seq.stage)
        // LEFT 여러 번
        assertFalse(seq.onSignal(Signal.LEFT))
        assertFalse(seq.onSignal(Signal.LEFT))
        assertEquals(SeqStage.WAIT_OFF_AGAIN, seq.stage)
        // OFF 로 완료는 단 한 번만 true
        assertTrue(seq.onSignal(Signal.OFF))
        assertFalse(seq.onSignal(Signal.OFF))   // 완료 후 추가 OFF는 false
        assertFalse(seq.onSignal(Signal.LEFT))  // 완료 후 어떤 입력도 false
    }

    @Test
    fun null은_완료로_취급하지않고_단계유지() {
        val seq = TurnSignalSequence(Signal.OFF)
        assertFalse(seq.onSignal(Signal.LEFT))   // WAIT_OFF_AGAIN
        assertFalse(seq.onSignal(null))          // 무시
        assertEquals(SeqStage.WAIT_OFF_AGAIN, seq.stage)
        assertTrue(seq.onSignal(Signal.OFF))     // 정상 완료
    }

    @Test
    fun 완료는_수행1회당_한번만_true() {
        val (_, done) = feed(Signal.OFF, Signal.LEFT, Signal.OFF, Signal.OFF, Signal.LEFT, Signal.OFF)
        assertEquals(1, done)
    }

    @Test
    fun 시작시_RIGHT면_OFF부터_다시() {
        // 시작값이 RIGHT면 WAIT_LEFT 로 시작(초기 LEFT만 NEED_OFF_FIRST)
        val seq = TurnSignalSequence(Signal.RIGHT)
        assertEquals(SeqStage.WAIT_LEFT, seq.stage)
    }
}
