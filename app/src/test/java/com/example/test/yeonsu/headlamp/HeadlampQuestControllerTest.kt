package com.example.test.yeonsu.headlamp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HeadlampQuestController 테스트 (가짜 수신원 FakeHeadlampSource 주입).
 * SDK 없이 수신 오류/중단/늦은 콜백/중복 완료/초기 완료/주간주행등 오판정 방지 경로를 검증한다.
 */
class HeadlampQuestControllerTest {

    private class FakeHeadlampSource : HeadlampSource {
        var observer: HeadlampSource.Observer? = null
        var started = false
        var stopped = false
        override fun start(observer: HeadlampSource.Observer) { this.observer = observer; started = true }
        override fun stop() { stopped = true }
        fun initial(s: HeadlampState?) = observer?.onInitial(s)
        fun update(s: HeadlampState?) = observer?.onUpdate(s)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : HeadlampQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        val guides = mutableListOf<String>()
        override fun onStageChanged(guide: String) { guides.add(guide) }
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(): Triple<FakeHeadlampSource, RecordingCallback, HeadlampQuestController> {
        val src = FakeHeadlampSource()
        val cb = RecordingCallback()
        val c = HeadlampQuestController(src, cb)
        c.start()
        return Triple(src, cb, c)
    }

    @Test
    fun 시작시_이미켜짐이면_바로_완료1회() {
        val (src, cb, _) = setup()
        src.initial(HeadlampState.ON)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 시작시_꺼짐이면_켤때_완료() {
        val (src, cb, _) = setup()
        src.initial(HeadlampState.OFF)
        assertEquals(0, cb.completedCount)
        src.update(HeadlampState.ON)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 주간주행등에서는_완료안됨_하향등ON에서만_완료() {
        val (src, cb, _) = setup()
        src.initial(HeadlampState.DAYTIME_RUNNING)
        src.update(HeadlampState.DAYTIME_RUNNING)
        assertEquals(0, cb.completedCount)
        src.update(HeadlampState.ON)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun null초기값은_완료안되고_이후_ON에_완료() {
        val (src, cb, _) = setup()
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(HeadlampState.ON)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop이후_늦은_ON콜백은_무시() {
        val (src, cb, c) = setup()
        src.initial(HeadlampState.OFF)
        c.stop()
        assertTrue(src.stopped)
        src.update(HeadlampState.ON) // 늦게 도착 → 무시
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun onError후_순서초기화_재연결시_다시판정() {
        val (src, cb, _) = setup()
        src.initial(HeadlampState.OFF)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        assertEquals(0, cb.completedCount)
        src.update(HeadlampState.OFF)
        assertEquals(0, cb.completedCount)
        src.update(HeadlampState.ON)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup()
        src.initial(HeadlampState.OFF)
        src.update(HeadlampState.ON) // 완료
        src.update(HeadlampState.OFF)
        src.update(HeadlampState.ON)
        assertEquals(1, cb.completedCount)
    }
}
