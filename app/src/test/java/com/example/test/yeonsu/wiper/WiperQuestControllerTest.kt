package com.example.test.yeonsu.wiper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WiperQuestController 테스트 (가짜 수신원 주입).
 * 한 번(ON)/끄기(OFF) 정상 완료, 시작 상태 처리, 중단 후 늦은 콜백, 오류 후 초기화, 중복 완료 방지, null.
 */
class WiperQuestControllerTest {

    private class FakeSource : WiperSource {
        var observer: WiperSource.Observer? = null
        var stopped = false
        override fun start(observer: WiperSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(v: Boolean?) = observer?.onInitial(v)
        fun update(v: Boolean?) = observer?.onUpdate(v)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : WiperQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(q: WiperQuest): Triple<FakeSource, RecordingCallback, WiperQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = WiperQuestController(src, cb, q)
        c.start()
        return Triple(src, cb, c)
    }

    @Test
    fun 한번_정상완료_꺼짐에서_작동() {
        val (src, cb, _) = setup(WiperQuest.ON)
        src.initial(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 한번_시작이미작동이면_바로완료안됨() {
        val (src, cb, _) = setup(WiperQuest.ON)
        src.initial(true)
        assertEquals(0, cb.completedCount)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 끄기_정상완료_작동에서_꺼짐() {
        val (src, cb, _) = setup(WiperQuest.OFF)
        src.initial(true)
        src.update(false)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun null은_완료아님() {
        val (src, cb, _) = setup(WiperQuest.ON)
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop이후_늦은콜백은_무시() {
        val (src, cb, c) = setup(WiperQuest.ON)
        src.initial(false)
        c.stop()
        assertTrue(src.stopped)
        src.update(true)
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun onError후_초기화_재연결시_다시판정() {
        val (src, cb, _) = setup(WiperQuest.ON)
        src.initial(false)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        // 재연결 첫 값이 작동(true)이면(이전 진행 이어받기 금지) 바로 완료 아님.
        src.update(true)
        assertEquals(0, cb.completedCount)
        // 껐다가 다시 작동해야 완료.
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup(WiperQuest.ON)
        src.initial(false)
        src.update(true)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 매핑_확인_단계형은대상아님() {
        assertTrue(WiperQuestController.isWiperQuest("vehicle-2-0"))
        assertTrue(WiperQuestController.isWiperQuest("vehicle-2-4"))
        // 자동/저속/고속(2-1/2-2/2-3)은 보류 → 대상 아님.
        assertEquals(false, WiperQuestController.isWiperQuest("vehicle-2-1"))
        assertEquals(false, WiperQuestController.isWiperQuest("vehicle-2-2"))
        assertEquals(false, WiperQuestController.isWiperQuest("vehicle-2-3"))
    }
}
