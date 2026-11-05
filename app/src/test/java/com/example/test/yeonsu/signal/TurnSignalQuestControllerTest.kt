package com.example.test.yeonsu.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TurnSignalQuestController 테스트 (가짜 수신원 FakeSource 주입).
 * SDK 없이 수신 오류/중단/늦은 콜백/중복 완료 경로를 검증한다.
 *
 * (순서 전이 자체의 정확성은 TurnSignalSequenceTest 에서 별도로 검증한다.)
 */
class TurnSignalQuestControllerTest {

    /** 테스트가 직접 신호를 밀어 넣는 가짜 수신원. */
    private class FakeSource : TurnSignalSource {
        var observer: TurnSignalSource.Observer? = null
        var started = false
        var stopped = false
        override fun start(observer: TurnSignalSource.Observer) { this.observer = observer; started = true }
        override fun stop() { stopped = true }
        // 테스트 편의 함수
        fun initial(s: Signal?) = observer?.onInitial(s)
        fun update(s: Signal?) = observer?.onUpdate(s)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : TurnSignalQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        val guides = mutableListOf<String>()
        override fun onStageChanged(guide: String) { guides.add(guide) }
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(): Triple<FakeSource, RecordingCallback, TurnSignalQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = TurnSignalQuestController(src, cb)
        c.start()
        return Triple(src, cb, c)
    }

    @Test
    fun 정상순서_OFF_LEFT_OFF_로_완료1회() {
        val (src, cb, _) = setup()
        src.initial(Signal.OFF)
        src.update(Signal.LEFT)
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun null은_단계진행이나_완료로_이어지지않는다() {
        val (src, cb, _) = setup()
        src.initial(Signal.OFF)
        src.update(Signal.LEFT)
        src.update(null)          // 무시
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(Signal.OFF)    // 정상 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun onError_이후에는_순서초기화되고_재연결시_새OFF부터() {
        val (src, cb, _) = setup()
        src.initial(Signal.OFF)
        src.update(Signal.LEFT)   // WAIT_OFF_AGAIN 까지 진행
        src.error("연결 끊김")      // 순서 초기화
        assertEquals(1, cb.errorCount)
        assertEquals(0, cb.completedCount)
        // 재연결 후 바로 OFF 가 와도(이전 LEFT 이어받기 금지) 완료되면 안 된다
        src.update(Signal.OFF)
        assertEquals(0, cb.completedCount)
        // 새 순서: LEFT → OFF 여야 완료
        src.update(Signal.LEFT)
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop_이후_도착한_콜백은_무시된다() {
        val (src, cb, c) = setup()
        src.initial(Signal.OFF)
        src.update(Signal.LEFT)
        c.stop()                  // 화면 이탈/중단
        assertTrue(src.stopped)
        // 늦게 도착한 OFF (이전 구독의 지연 콜백)
        src.update(Signal.OFF)
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백이_와도_완료는_1회만() {
        val (src, cb, _) = setup()
        src.initial(Signal.OFF)
        src.update(Signal.LEFT)
        src.update(Signal.OFF)    // 완료
        // 완료 뒤 중복/추가 신호
        src.update(Signal.LEFT)
        src.update(Signal.OFF)
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 시작시_LEFT면_먼저꺼야_완료된다_컨트롤러경로() {
        val (src, cb, _) = setup()
        src.initial(Signal.LEFT)  // NEED_OFF_FIRST
        src.update(Signal.LEFT)   // 반복 무시
        assertEquals(0, cb.completedCount)
        src.update(Signal.OFF)    // 먼저 끄기 → WAIT_LEFT
        src.update(Signal.LEFT)
        src.update(Signal.OFF)    // 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun RIGHT개입시_완료안됨_새LEFT_OFF로만_완료() {
        val (src, cb, _) = setup()
        src.initial(Signal.OFF)
        src.update(Signal.LEFT)
        src.update(Signal.RIGHT)  // 초기화
        src.update(Signal.OFF)    // 아직 완료 아님
        assertEquals(0, cb.completedCount)
        src.update(Signal.LEFT)
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료시_콜백이_정확히_한번_그리고_error는_0() {
        val (src, cb, _) = setup()
        src.initial(Signal.OFF)
        src.update(Signal.LEFT)
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
        assertTrue(cb.guides.isNotEmpty())
        assertFalse(cb.guides.first().isBlank())
    }
}
