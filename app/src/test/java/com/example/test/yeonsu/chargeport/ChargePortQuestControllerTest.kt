package com.example.test.yeonsu.chargeport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ChargePortQuestController 테스트 (가짜 수신원 주입).
 * 열기 정상 완료, 시작 이미열림 처리, 중단 후 늦은 콜백, 오류 후 초기화, 중복 완료 방지, null 처리.
 */
class ChargePortQuestControllerTest {

    private class FakeSource : ChargePortSource {
        var observer: ChargePortSource.Observer? = null
        var stopped = false
        override fun start(observer: ChargePortSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(v: Boolean?) = observer?.onInitial(v)
        fun update(v: Boolean?) = observer?.onUpdate(v)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : ChargePortQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(): Triple<FakeSource, RecordingCallback, ChargePortQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = ChargePortQuestController(src, cb)
        c.start()
        return Triple(src, cb, c)
    }

    @Test
    fun 정상완료_닫힘에서_열림() {
        val (src, cb, _) = setup()
        src.initial(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 시작이미열림이면_바로완료안됨_닫혔다열려야완료() {
        val (src, cb, _) = setup()
        src.initial(true)
        assertEquals(0, cb.completedCount)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun null은_완료아님() {
        val (src, cb, _) = setup()
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop이후_늦은열림은_무시() {
        val (src, cb, c) = setup()
        src.initial(false)
        c.stop()
        assertTrue(src.stopped)
        src.update(true)
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun onError후_초기화_재연결시_다시판정() {
        val (src, cb, _) = setup()
        src.initial(false)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        // 재연결 첫 값이 열림이면(이전 진행 이어받기 금지) 바로 완료하지 않는다.
        src.update(true)
        assertEquals(0, cb.completedCount)
        // 닫혔다가 다시 열려야 완료.
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup()
        src.initial(false)
        src.update(true)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 닫힘반복으로는_완료안됨() {
        val (src, cb, _) = setup()
        src.initial(false)
        src.update(false)
        assertEquals(0, cb.completedCount)
    }
}
