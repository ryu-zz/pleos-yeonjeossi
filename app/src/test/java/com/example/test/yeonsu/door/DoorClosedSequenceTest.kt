package com.example.test.yeonsu.door

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 운전석 '문 확인' 순서 판정(DoorClosedSequence) 단위 테스트.
 * SDK 비의존 순수 로직이므로 JVM 테스트로 바로 돌아간다.
 */
class DoorClosedSequenceTest {

    @Test
    fun 시작시_이미닫힘이면_바로_DONE() {
        val seq = DoorClosedSequence(DoorState.CLOSED)
        assertEquals(DoorStage.DONE, seq.stage)
        assertTrue(seq.isDone)
    }

    @Test
    fun 시작시_열림이면_WAIT_CLOSED_이고_닫으면_완료() {
        val seq = DoorClosedSequence(DoorState.OPEN)
        assertEquals(DoorStage.WAIT_CLOSED, seq.stage)
        assertFalse(seq.isDone)
        assertTrue(seq.onSignal(DoorState.CLOSED))   // 닫힘 → 완료(새로 완료 true)
        assertTrue(seq.isDone)
    }

    @Test
    fun 열림반복은_완료로_안간다() {
        val seq = DoorClosedSequence(DoorState.OPEN)
        assertFalse(seq.onSignal(DoorState.OPEN))
        assertFalse(seq.onSignal(DoorState.OPEN))
        assertEquals(DoorStage.WAIT_CLOSED, seq.stage)
        assertTrue(seq.onSignal(DoorState.CLOSED))
    }

    @Test
    fun null은_완료로_취급하지않고_단계유지() {
        val seq = DoorClosedSequence(DoorState.OPEN)
        assertFalse(seq.onSignal(null))
        assertEquals(DoorStage.WAIT_CLOSED, seq.stage)
        assertTrue(seq.onSignal(DoorState.CLOSED))
    }

    @Test
    fun 시작값이_null이면_WAIT_CLOSED() {
        val seq = DoorClosedSequence(null)
        assertEquals(DoorStage.WAIT_CLOSED, seq.stage)
        assertFalse(seq.isDone)
    }

    @Test
    fun 완료후_입력은_다시_true를_돌려주지않는다() {
        val seq = DoorClosedSequence(DoorState.OPEN)
        assertTrue(seq.onSignal(DoorState.CLOSED))   // 완료
        assertFalse(seq.onSignal(DoorState.OPEN))    // 완료 후 입력 무시
        assertFalse(seq.onSignal(DoorState.CLOSED))
    }
}
