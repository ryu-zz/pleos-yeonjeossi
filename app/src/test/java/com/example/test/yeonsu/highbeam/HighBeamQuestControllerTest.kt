package com.example.test.yeonsu.highbeam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HighBeamQuestController 테스트 (가짜 수신원 주입).
 * 켜기/끄기 각각 정상 완료, 시작 상태 처리, 중단 후 늦은 콜백, 오류 후 초기화, 중복 완료 방지를 검증한다.
 */
class HighBeamQuestControllerTest {

    private class FakeSource : HighBeamSource {
        var observer: HighBeamSource.Observer? = null
        var stopped = false
        override fun start(observer: HighBeamSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(s: HighBeamState?) = observer?.onInitial(s)
        fun update(s: HighBeamState?) = observer?.onUpdate(s)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : HighBeamQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(quest: HighBeamQuest): Triple<FakeSource, RecordingCallback, HighBeamQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = HighBeamQuestController(src, cb, quest)
        c.start()
        return Triple(src, cb, c)
    }

    // ───── 켜기 ─────

    @Test
    fun 켜기_정상완료_OFF_ON() {
        val (src, cb, _) = setup(HighBeamQuest.ON)
        src.initial(HighBeamState.OFF)
        src.update(HighBeamState.ON)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 켜기_시작이미ON이면_바로완료안됨_껐다켜야완료() {
        val (src, cb, _) = setup(HighBeamQuest.ON)
        src.initial(HighBeamState.ON)
        assertEquals(0, cb.completedCount)   // 시작값만으로는 완료 안 함
        src.update(HighBeamState.OFF)
        src.update(HighBeamState.ON)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 켜기_stop이후_늦은ON은_무시() {
        val (src, cb, c) = setup(HighBeamQuest.ON)
        src.initial(HighBeamState.OFF)
        c.stop()
        assertTrue(src.stopped)
        src.update(HighBeamState.ON)
        assertEquals(0, cb.completedCount)
    }

    // ───── 끄기 ─────

    @Test
    fun 끄기_정상완료_ON_OFF() {
        val (src, cb, _) = setup(HighBeamQuest.OFF)
        src.initial(HighBeamState.ON)
        src.update(HighBeamState.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 끄기_시작이미OFF면_바로완료안됨_켰다꺼야완료() {
        val (src, cb, _) = setup(HighBeamQuest.OFF)
        src.initial(HighBeamState.OFF)
        assertEquals(0, cb.completedCount)
        src.update(HighBeamState.ON)
        src.update(HighBeamState.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 끄기_onError후_초기화_재연결시_다시판정() {
        val (src, cb, _) = setup(HighBeamQuest.OFF)
        src.initial(HighBeamState.ON)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        // 재연결: 다시 ON → OFF 로만 완료
        src.update(HighBeamState.ON)
        assertEquals(0, cb.completedCount)
        src.update(HighBeamState.OFF)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 끄기_완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup(HighBeamQuest.OFF)
        src.initial(HighBeamState.ON)
        src.update(HighBeamState.OFF)
        src.update(HighBeamState.ON)
        src.update(HighBeamState.OFF)
        assertEquals(1, cb.completedCount)
    }
}
