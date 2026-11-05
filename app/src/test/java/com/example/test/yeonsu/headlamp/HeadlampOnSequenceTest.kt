package com.example.test.yeonsu.headlamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * '하향등' 순서 판정(HeadlampOnSequence) 단위 테스트.
 * SDK 비의존 순수 로직이므로 JVM 테스트로 바로 돌아간다.
 */
class HeadlampOnSequenceTest {

    @Test
    fun 시작시_이미켜짐이면_바로_DONE() {
        val seq = HeadlampOnSequence(HeadlampState.ON)
        assertEquals(HeadlampStage.DONE, seq.stage)
        assertTrue(seq.isDone)
    }

    @Test
    fun 시작시_꺼짐이면_WAIT_ON_이고_켜면_완료() {
        val seq = HeadlampOnSequence(HeadlampState.OFF)
        assertEquals(HeadlampStage.WAIT_ON, seq.stage)
        assertFalse(seq.isDone)
        assertTrue(seq.onSignal(HeadlampState.ON))
        assertTrue(seq.isDone)
    }

    @Test
    fun 주간주행등은_하향등켜짐으로_인정하지않는다() {
        val seq = HeadlampOnSequence(HeadlampState.DAYTIME_RUNNING)
        assertEquals(HeadlampStage.WAIT_ON, seq.stage)
        assertFalse(seq.onSignal(HeadlampState.DAYTIME_RUNNING)) // 여전히 미완료
        assertFalse(seq.isDone)
        assertTrue(seq.onSignal(HeadlampState.ON))               // 하향등 켜짐에서만 완료
    }

    @Test
    fun 꺼짐반복은_완료로_안간다() {
        val seq = HeadlampOnSequence(HeadlampState.OFF)
        assertFalse(seq.onSignal(HeadlampState.OFF))
        assertFalse(seq.onSignal(HeadlampState.OFF))
        assertEquals(HeadlampStage.WAIT_ON, seq.stage)
        assertTrue(seq.onSignal(HeadlampState.ON))
    }

    @Test
    fun null은_완료로_취급하지않고_단계유지() {
        val seq = HeadlampOnSequence(HeadlampState.OFF)
        assertFalse(seq.onSignal(null))
        assertEquals(HeadlampStage.WAIT_ON, seq.stage)
        assertTrue(seq.onSignal(HeadlampState.ON))
    }

    @Test
    fun 시작값이_null이면_WAIT_ON() {
        val seq = HeadlampOnSequence(null)
        assertEquals(HeadlampStage.WAIT_ON, seq.stage)
        assertFalse(seq.isDone)
    }

    @Test
    fun 완료후_입력은_다시_true를_돌려주지않는다() {
        val seq = HeadlampOnSequence(HeadlampState.OFF)
        assertTrue(seq.onSignal(HeadlampState.ON))
        assertFalse(seq.onSignal(HeadlampState.OFF))
        assertFalse(seq.onSignal(HeadlampState.ON))
    }
}
