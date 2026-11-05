package com.example.test.yeonsu.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 오른쪽 방향지시등(RIGHT)·방향지시등 끄기(OFF) 순서 판정 단위 테스트.
 * SDK 비의존 순수 로직(TurnSignalSequence)이므로 JVM 테스트로 바로 돌아간다.
 */
class TurnSignalRightOffSequenceTest {

    private fun feed(quest: TurnSignalQuest, initial: Signal?, vararg signals: Signal?): Pair<TurnSignalSequence, Int> {
        val seq = TurnSignalSequence(initial, quest)
        var completions = 0
        for (s in signals) if (seq.onSignal(s)) completions++
        return seq to completions
    }

    // ───── 오른쪽 방향지시등 (vehicle-1-1): OFF → RIGHT → OFF ─────

    @Test
    fun 오른쪽_정상순서_OFF_RIGHT_OFF_완료1회() {
        val (seq, done) = feed(TurnSignalQuest.RIGHT, Signal.OFF, Signal.RIGHT, Signal.OFF)
        assertTrue(seq.isDone)
        assertEquals(1, done)
    }

    @Test
    fun 오른쪽_시작시_RIGHT면_먼저꺼야_완료된다() {
        val seq = TurnSignalSequence(Signal.RIGHT, TurnSignalQuest.RIGHT)
        assertEquals(SeqStage.NEED_OFF_FIRST, seq.stage)
        assertFalse(seq.onSignal(Signal.RIGHT))   // 반복 무시
        assertFalse(seq.onSignal(Signal.OFF))     // 먼저 끄기 → WAIT_LEFT(목표 켜기 대기)
        assertEquals(SeqStage.WAIT_LEFT, seq.stage)
        assertFalse(seq.onSignal(Signal.RIGHT))   // 켜기
        assertTrue(seq.onSignal(Signal.OFF))      // 끄기 → 완료
        assertTrue(seq.isDone)
    }

    @Test
    fun 오른쪽_중간에_LEFT들어오면_순서초기화() {
        val seq = TurnSignalSequence(Signal.OFF, TurnSignalQuest.RIGHT)
        assertFalse(seq.onSignal(Signal.RIGHT))   // WAIT_OFF_AGAIN
        assertEquals(SeqStage.WAIT_OFF_AGAIN, seq.stage)
        assertFalse(seq.onSignal(Signal.LEFT))    // 반대방향 → 초기화 WAIT_LEFT
        assertEquals(SeqStage.WAIT_LEFT, seq.stage)
        // 여기서 바로 OFF가 와도 완료 아님(RIGHT를 아직 안 거침)
        assertFalse(seq.onSignal(Signal.OFF))
        assertFalse(seq.onSignal(Signal.RIGHT))
        assertTrue(seq.onSignal(Signal.OFF))      // 정상 완료
    }

    @Test
    fun 오른쪽_왼쪽신호로는_완료되지않는다() {
        // 왼쪽만 켜고 끄는 동작으로는 오른쪽 퀘스트가 완료되면 안 된다.
        val (seq, done) = feed(TurnSignalQuest.RIGHT, Signal.OFF, Signal.LEFT, Signal.OFF)
        assertFalse(seq.isDone)
        assertEquals(0, done)
    }

    @Test
    fun 오른쪽_중복이벤트는_다음단계로_넘어가지않고_완료는_한번만() {
        val (_, done) = feed(
            TurnSignalQuest.RIGHT,
            Signal.OFF, Signal.OFF, Signal.RIGHT, Signal.RIGHT, Signal.OFF, Signal.OFF,
            Signal.RIGHT, Signal.OFF // 완료 후 추가 순서는 무시
        )
        assertEquals(1, done)
    }

    @Test
    fun 오른쪽_null은_단계유지하고_완료로_안간다() {
        val seq = TurnSignalSequence(Signal.OFF, TurnSignalQuest.RIGHT)
        assertFalse(seq.onSignal(Signal.RIGHT))   // WAIT_OFF_AGAIN
        assertFalse(seq.onSignal(null))           // 무시
        assertEquals(SeqStage.WAIT_OFF_AGAIN, seq.stage)
        assertTrue(seq.onSignal(Signal.OFF))      // 완료
    }

    // ───── 방향지시등 끄기 (vehicle-1-2): 켜짐(LEFT/RIGHT) 확인 후 OFF ─────

    @Test
    fun 끄기_시작OFF면_먼저켜고_끄면_완료_LEFT경로() {
        val seq = TurnSignalSequence(Signal.OFF, TurnSignalQuest.OFF)
        assertEquals(SeqStage.NEED_ON, seq.stage)
        assertFalse(seq.onSignal(Signal.OFF))     // 처음부터 OFF 반복은 완료 아님
        assertFalse(seq.onSignal(Signal.LEFT))    // 켜짐 확인 → WAIT_OFF
        assertEquals(SeqStage.WAIT_OFF, seq.stage)
        assertTrue(seq.onSignal(Signal.OFF))      // 끄기 → 완료
        assertTrue(seq.isDone)
    }

    @Test
    fun 끄기_시작OFF면_먼저켜고_끄면_완료_RIGHT경로() {
        val (seq, done) = feed(TurnSignalQuest.OFF, Signal.OFF, Signal.RIGHT, Signal.OFF)
        assertTrue(seq.isDone)
        assertEquals(1, done)
    }

    @Test
    fun 끄기_시작시_이미켜져있으면_바로끄면_완료() {
        val seq = TurnSignalSequence(Signal.LEFT, TurnSignalQuest.OFF)
        assertEquals(SeqStage.WAIT_OFF, seq.stage) // 이미 켜짐 → 바로 끄기 대기
        assertTrue(seq.onSignal(Signal.OFF))       // 끄기 → 완료
        assertTrue(seq.isDone)
    }

    @Test
    fun 끄기_처음부터_OFF만으로는_완료하지않는다() {
        val (seq, done) = feed(TurnSignalQuest.OFF, Signal.OFF, Signal.OFF, Signal.OFF)
        assertFalse(seq.isDone)
        assertEquals(0, done)
        assertEquals(SeqStage.NEED_ON, seq.stage)
    }

    @Test
    fun 끄기_켜짐상태_유지중_방향전환은_완료아님_OFF여야_완료() {
        // 켜짐 확인 후 LEFT↔RIGHT 전환은 여전히 켜짐 → 유지, OFF 에서만 완료.
        val seq = TurnSignalSequence(Signal.OFF, TurnSignalQuest.OFF)
        assertFalse(seq.onSignal(Signal.LEFT))   // WAIT_OFF
        assertFalse(seq.onSignal(Signal.RIGHT))  // 여전히 켜짐 → 유지
        assertEquals(SeqStage.WAIT_OFF, seq.stage)
        assertTrue(seq.onSignal(Signal.OFF))     // 완료
    }

    @Test
    fun 끄기_null은_단계유지하고_완료로_안간다() {
        val seq = TurnSignalSequence(Signal.OFF, TurnSignalQuest.OFF)
        assertFalse(seq.onSignal(Signal.RIGHT))  // WAIT_OFF
        assertFalse(seq.onSignal(null))          // 무시
        assertEquals(SeqStage.WAIT_OFF, seq.stage)
        assertTrue(seq.onSignal(Signal.OFF))     // 완료
    }

    @Test
    fun 끄기_완료는_수행1회당_한번만() {
        val (_, done) = feed(
            TurnSignalQuest.OFF,
            Signal.OFF, Signal.LEFT, Signal.OFF, // 완료
            Signal.RIGHT, Signal.OFF             // 이후 입력은 무시
        )
        assertEquals(1, done)
    }
}
