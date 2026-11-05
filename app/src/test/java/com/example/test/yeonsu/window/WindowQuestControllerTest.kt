package com.example.test.yeonsu.window

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WindowQuestController 테스트 (가짜 수신원 주입).
 * 열기/닫기 정상 완료, 시작 상태 처리, 중단 후 늦은 콜백, 오류 후 초기화, 중복 완료 방지를 검증한다.
 */
class WindowQuestControllerTest {

    private class FakeSource : WindowSource {
        var observer: WindowSource.Observer? = null
        var stopped = false
        override fun start(observer: WindowSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(p: Int?) = observer?.onInitial(p)
        fun update(p: Int?) = observer?.onUpdate(p)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : WindowQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(quest: WindowQuest): Triple<FakeSource, RecordingCallback, WindowQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = WindowQuestController(src, cb, quest)
        c.start()
        return Triple(src, cb, c)
    }

    // ───── 열기 ─────

    @Test
    fun 열기_정상완료_닫힘_열림() {
        val (src, cb, _) = setup(WindowQuest.OPEN)
        src.initial(0)
        src.update(25)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 열기_시작이미열림이면_바로완료안됨() {
        val (src, cb, _) = setup(WindowQuest.OPEN)
        src.initial(40)
        assertEquals(0, cb.completedCount)
        src.update(0)
        src.update(20)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 열기_stop이후_늦은열림은_무시() {
        val (src, cb, c) = setup(WindowQuest.OPEN)
        src.initial(0)
        c.stop()
        assertTrue(src.stopped)
        src.update(50)
        assertEquals(0, cb.completedCount)
    }

    // ───── 닫기 ─────

    @Test
    fun 닫기_정상완료_열림_완전닫힘() {
        val (src, cb, _) = setup(WindowQuest.CLOSE)
        src.initial(70)
        src.update(10)  // 부분만 닫힘 → 미완료
        assertEquals(0, cb.completedCount)
        src.update(0)   // 완전히 닫힘 → 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 닫기_시작이미닫힘이면_바로완료안됨() {
        val (src, cb, _) = setup(WindowQuest.CLOSE)
        src.initial(0)
        assertEquals(0, cb.completedCount)
        src.update(30)
        src.update(0)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 닫기_onError후_초기화_재연결시_다시판정() {
        val (src, cb, _) = setup(WindowQuest.CLOSE)
        src.initial(50)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        src.update(0)   // 재연결 첫 값이 0이어도(이전 진행 이어받기 금지) 완료 아님
        assertEquals(0, cb.completedCount)
        src.update(30)
        src.update(0)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 닫기_완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup(WindowQuest.CLOSE)
        src.initial(40)
        src.update(0)
        src.update(30)
        src.update(0)
        assertEquals(1, cb.completedCount)
    }
}
