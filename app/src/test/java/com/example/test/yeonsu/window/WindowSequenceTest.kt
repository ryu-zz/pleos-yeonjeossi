package com.example.test.yeonsu.window

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 운전석 창문 열기(OPEN)·닫기(CLOSE) 순서 판정(WindowSequence) 단위 테스트.
 * 위치: 0=완전히 닫힘, 1~100=열림.
 */
class WindowSequenceTest {

    // ───── 열기 (vehicle-4-0): position>0 ─────

    @Test
    fun 열기_시작닫힘이면_내리면_완료() {
        val seq = WindowSequence(0, WindowQuest.OPEN)
        assertEquals(WindowStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(30))   // 부분 열림도 완료
        assertTrue(seq.isDone)
    }

    @Test
    fun 열기_부분이든_완전이든_열리면_완료() {
        assertTrue(WindowSequence(0, WindowQuest.OPEN).onSignal(1))
        assertTrue(WindowSequence(0, WindowQuest.OPEN).onSignal(100))
    }

    @Test
    fun 열기_시작시_이미열려있으면_닫았다_내려야_완료() {
        val seq = WindowSequence(50, WindowQuest.OPEN)
        assertEquals(WindowStage.NEED_OPPOSITE_FIRST, seq.stage)
        assertFalse(seq.onSignal(80))  // 아직 닫힘 안 거침 → 무시
        assertFalse(seq.onSignal(0))   // 닫힘 확인 → WAIT_TARGET
        assertEquals(WindowStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(20))   // 다시 열림 → 완료
    }

    @Test
    fun 열기_null은_단계유지하고_완료로_안감() {
        val seq = WindowSequence(0, WindowQuest.OPEN)
        assertFalse(seq.onSignal(null))
        assertEquals(WindowStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(10))
    }

    @Test
    fun 열기_완료후_입력은_다시_true_안돌려줌() {
        val seq = WindowSequence(0, WindowQuest.OPEN)
        assertTrue(seq.onSignal(40))
        assertFalse(seq.onSignal(0))
        assertFalse(seq.onSignal(40))
    }

    // ───── 닫기 (vehicle-4-1): position==0 ─────

    @Test
    fun 닫기_시작열림이면_완전히닫으면_완료() {
        val seq = WindowSequence(60, WindowQuest.CLOSE)
        assertEquals(WindowStage.WAIT_TARGET, seq.stage)
        assertFalse(seq.onSignal(10))  // 아직 완전히 닫힌 게 아님
        assertTrue(seq.onSignal(0))    // 완전히 닫힘 → 완료
        assertTrue(seq.isDone)
    }

    @Test
    fun 닫기_부분만_닫는것으로는_완료안됨() {
        val seq = WindowSequence(80, WindowQuest.CLOSE)
        assertFalse(seq.onSignal(40))
        assertFalse(seq.onSignal(5))
        assertFalse(seq.isDone)
        assertTrue(seq.onSignal(0))
    }

    @Test
    fun 닫기_시작시_이미닫혀있으면_열었다_닫아야_완료() {
        val seq = WindowSequence(0, WindowQuest.CLOSE)
        assertEquals(WindowStage.NEED_OPPOSITE_FIRST, seq.stage)
        assertFalse(seq.onSignal(0))   // 아직 열림 안 거침 → 무시
        assertFalse(seq.onSignal(30))  // 열림 확인 → WAIT_TARGET
        assertEquals(WindowStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(0))    // 다시 닫힘 → 완료
    }

    @Test
    fun 닫기_null은_단계유지하고_완료로_안감() {
        val seq = WindowSequence(50, WindowQuest.CLOSE)
        assertFalse(seq.onSignal(null))
        assertEquals(WindowStage.WAIT_TARGET, seq.stage)
        assertTrue(seq.onSignal(0))
    }
}
