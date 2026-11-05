package com.example.test.yeonsu.trunk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TrunkQuestController 테스트 (가짜 수신원 주입).
 * 열기 정상 완료, 시작 이미열림 처리, 중단 후 늦은 콜백, 오류 후 초기화, 중복 완료 방지, null 처리.
 */
class TrunkQuestControllerTest {

    private class FakeSource : TrunkSource {
        var observer: TrunkSource.Observer? = null
        var stopped = false
        override fun start(observer: TrunkSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(s: TrunkState?) = observer?.onInitial(s)
        fun update(s: TrunkState?) = observer?.onUpdate(s)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : TrunkQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(): Triple<FakeSource, RecordingCallback, TrunkQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = TrunkQuestController(src, cb)
        c.start()
        return Triple(src, cb, c)
    }

    @Test
    fun 정상완료_닫힘에서_열림() {
        val (src, cb, _) = setup()
        src.initial(TrunkState.CLOSED)
        src.update(TrunkState.OPEN)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 시작이미열림이면_바로완료안됨_닫혔다열려야완료() {
        val (src, cb, _) = setup()
        src.initial(TrunkState.OPEN)
        assertEquals(0, cb.completedCount)
        src.update(TrunkState.CLOSED)
        src.update(TrunkState.OPEN)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun null은_완료아님() {
        val (src, cb, _) = setup()
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(TrunkState.OPEN)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop이후_늦은열림은_무시() {
        val (src, cb, c) = setup()
        src.initial(TrunkState.CLOSED)
        c.stop()
        assertTrue(src.stopped)
        src.update(TrunkState.OPEN)
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun onError후_초기화_재연결시_다시판정() {
        val (src, cb, _) = setup()
        src.initial(TrunkState.CLOSED)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        // 재연결 첫 값이 OPEN 이면(이전 진행 이어받기 금지) 이미 열림으로 보고 바로 완료하지 않는다.
        src.update(TrunkState.OPEN)
        assertEquals(0, cb.completedCount)
        // 닫혔다가 다시 열려야 완료.
        src.update(TrunkState.CLOSED)
        src.update(TrunkState.OPEN)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup()
        src.initial(TrunkState.CLOSED)
        src.update(TrunkState.OPEN)
        src.update(TrunkState.CLOSED)
        src.update(TrunkState.OPEN)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 닫힘반복으로는_완료안됨() {
        val (src, cb, _) = setup()
        src.initial(TrunkState.CLOSED)
        src.update(TrunkState.CLOSED)
        assertEquals(0, cb.completedCount)
    }
}
