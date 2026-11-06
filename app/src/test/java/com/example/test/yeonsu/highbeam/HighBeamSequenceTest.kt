package com.example.test.yeonsu.highbeam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 상향등 켜기(ON)·끄기(OFF) 순서 판정(HighBeamSequence) 단위 테스트.
 */
class HighBeamSequenceTest {

    // ───── 켜기 (vehicle-3-2): 수행 중 OFF → ON ─────

    @Test
    fun 켜기_시작OFF면_ON에서_완료() {
        val seq = HighBeamSequence(HighBeamState.OFF, HighBeamQuest.ON)
        assertEquals(HighBeamStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(HighBeamState.ON))
        assertTrue(seq.isDone)
    }

    @Test
    fun 켜기_시작시_이미ON이면_껐다_켜야_완료() {
        val seq = HighBeamSequence(HighBeamState.ON, HighBeamQuest.ON)
        assertEquals(HighBeamStage.NEED_OPPOSITE_FIRST, seq.stage)
        assertFalse(seq.onSignal(HighBeamState.ON))   // 아직 반대(OFF) 안 거침 → 무시
        assertFalse(seq.onSignal(HighBeamState.OFF))  // OFF 확인 → WAIT_TARGET
        assertEquals(HighBeamStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(HighBeamState.ON))    // ON → 완료
    }

    @Test
    fun 켜기_주간주행등은_ON으로_인정안함() {
        val seq = HighBeamSequence(HighBeamState.OFF, HighBeamQuest.ON)
        assertFalse(seq.onSignal(HighBeamState.DAYTIME_RUNNING))
        assertFalse(seq.isDone)
        assertTrue(seq.onSignal(HighBeamState.ON))
    }

    @Test
    fun 켜기_null은_단계유지하고_완료로_안감() {
        val seq = HighBeamSequence(HighBeamState.OFF, HighBeamQuest.ON)
        assertFalse(seq.onSignal(null))
        assertEquals(HighBeamStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(HighBeamState.ON))
    }

    @Test
    fun 켜기_완료후_입력은_다시_true_안돌려줌() {
        val seq = HighBeamSequence(HighBeamState.OFF, HighBeamQuest.ON)
        assertTrue(seq.onSignal(HighBeamState.ON))
        assertFalse(seq.onSignal(HighBeamState.OFF))
        assertFalse(seq.onSignal(HighBeamState.ON))
    }

    // ───── 끄기 (vehicle-3-3): 수행 중 ON → OFF ─────

    @Test
    fun 끄기_시작ON이면_OFF에서_완료() {
        val seq = HighBeamSequence(HighBeamState.ON, HighBeamQuest.OFF)
        assertEquals(HighBeamStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(HighBeamState.OFF))
        assertTrue(seq.isDone)
    }

    @Test
    fun 끄기_시작시_이미OFF면_켰다_꺼야_완료() {
        val seq = HighBeamSequence(HighBeamState.OFF, HighBeamQuest.OFF)
        assertEquals(HighBeamStage.NEED_OPPOSITE_FIRST, seq.stage)
        assertFalse(seq.onSignal(HighBeamState.OFF))  // 아직 ON 안 거침 → 무시
        assertFalse(seq.onSignal(HighBeamState.ON))   // ON 확인 → WAIT_TARGET
        assertEquals(HighBeamStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(HighBeamState.OFF))   // OFF → 완료
    }

    @Test
    fun 끄기_ON반복은_완료로_안가고_OFF에서_완료() {
        val seq = HighBeamSequence(HighBeamState.ON, HighBeamQuest.OFF)
        assertFalse(seq.onSignal(HighBeamState.ON))
        assertEquals(HighBeamStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(HighBeamState.OFF))
    }

    @Test
    fun 끄기_null은_단계유지하고_완료로_안감() {
        val seq = HighBeamSequence(HighBeamState.ON, HighBeamQuest.OFF)
        assertFalse(seq.onSignal(null))
        assertEquals(HighBeamStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(HighBeamState.OFF))
    }
}
