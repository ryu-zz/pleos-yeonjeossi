package com.example.test.yeonsu.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 오른쪽(RIGHT)·끄기(OFF) 퀘스트의 컨트롤러 경로 테스트 (가짜 수신원 주입).
 * 중단/화면 이탈 후 늦은 콜백, 재연결, 중복 완료 방지를 새 종류에서도 검증한다.
 */
class TurnSignalRightOffControllerTest {

    private class FakeSource : TurnSignalSource {
        var observer: TurnSignalSource.Observer? = null
        var stopped = false
        override fun start(observer: TurnSignalSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(s: Signal?) = observer?.onInitial(s)
        fun update(s: Signal?) = observer?.onUpdate(s)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : TurnSignalQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(quest: TurnSignalQuest): Triple<FakeSource, RecordingCallback, TurnSignalQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = TurnSignalQuestController(src, cb, quest)
        c.start()
        return Triple(src, cb, c)
    }

    // ───── RIGHT ─────

    @Test
    fun 오른쪽_정상완료_1회() {
        val (src, cb, _) = setup(TurnSignalQuest.RIGHT)
        src.initial(Signal.OFF)
        src.update(Signal.RIGHT)
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 오른쪽_stop이후_늦은콜백은_무시() {
        val (src, cb, c) = setup(TurnSignalQuest.RIGHT)
        src.initial(Signal.OFF)
        src.update(Signal.RIGHT)
        c.stop()
        assertTrue(src.stopped)
        src.update(Signal.OFF) // 늦게 도착 → 무시
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun 오른쪽_onError후_이전진행_이어받지않음() {
        val (src, cb, _) = setup(TurnSignalQuest.RIGHT)
        src.initial(Signal.OFF)
        src.update(Signal.RIGHT)  // WAIT_OFF_AGAIN 진행
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        src.update(Signal.OFF)    // 재연결 첫 신호가 OFF 여도 완료 아님
        assertEquals(0, cb.completedCount)
        src.update(Signal.RIGHT)
        src.update(Signal.OFF)    // 새 순서로만 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 오른쪽_완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup(TurnSignalQuest.RIGHT)
        src.initial(Signal.OFF)
        src.update(Signal.RIGHT)
        src.update(Signal.OFF)
        src.update(Signal.RIGHT)
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
    }

    // ───── OFF ─────

    @Test
    fun 끄기_시작이미켜짐_바로끄면_완료1회() {
        val (src, cb, _) = setup(TurnSignalQuest.OFF)
        src.initial(Signal.LEFT) // 이미 켜짐
        src.update(Signal.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 끄기_처음부터OFF면_완료안됨_켜고꺼야_완료() {
        val (src, cb, _) = setup(TurnSignalQuest.OFF)
        src.initial(Signal.OFF)
        src.update(Signal.OFF)   // 아직 완료 아님
        assertEquals(0, cb.completedCount)
        src.update(Signal.RIGHT) // 켜짐 확인
        src.update(Signal.OFF)   // 끄기 → 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 끄기_stop이후_늦은OFF는_무시() {
        val (src, cb, c) = setup(TurnSignalQuest.OFF)
        src.initial(Signal.LEFT) // WAIT_OFF
        c.stop()
        src.update(Signal.OFF)   // 늦게 도착 → 무시
        assertEquals(0, cb.completedCount)
    }
}
