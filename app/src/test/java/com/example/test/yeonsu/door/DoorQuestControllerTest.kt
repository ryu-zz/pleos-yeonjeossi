package com.example.test.yeonsu.door

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DoorQuestController 테스트 (가짜 수신원 FakeDoorSource 주입).
 * SDK 없이 수신 오류/중단/늦은 콜백/중복 완료/초기 완료 경로를 검증한다.
 */
class DoorQuestControllerTest {

    private class FakeDoorSource : DoorSource {
        var observer: DoorSource.Observer? = null
        var started = false
        var stopped = false
        override fun start(observer: DoorSource.Observer) { this.observer = observer; started = true }
        override fun stop() { stopped = true }
        fun initial(s: DoorState?) = observer?.onInitial(s)
        fun update(s: DoorState?) = observer?.onUpdate(s)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : DoorQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        val guides = mutableListOf<String>()
        override fun onStageChanged(guide: String) { guides.add(guide) }
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(): Triple<FakeDoorSource, RecordingCallback, DoorQuestController> {
        val src = FakeDoorSource()
        val cb = RecordingCallback()
        val c = DoorQuestController(src, cb)
        c.start()
        return Triple(src, cb, c)
    }

    @Test
    fun 시작시_이미닫힘이면_바로_완료1회() {
        val (src, cb, _) = setup()
        src.initial(DoorState.CLOSED)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 시작시_열림이면_닫을때_완료() {
        val (src, cb, _) = setup()
        src.initial(DoorState.OPEN)
        assertEquals(0, cb.completedCount)
        src.update(DoorState.CLOSED)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun null초기값은_완료안되고_이후_닫힘에_완료() {
        val (src, cb, _) = setup()
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(DoorState.CLOSED)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop이후_늦은_닫힘콜백은_무시() {
        val (src, cb, c) = setup()
        src.initial(DoorState.OPEN)
        c.stop()
        assertTrue(src.stopped)
        src.update(DoorState.CLOSED) // 늦게 도착 → 무시
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun onError후_순서초기화_재연결시_다시판정() {
        val (src, cb, _) = setup()
        src.initial(DoorState.OPEN)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        assertEquals(0, cb.completedCount)
        // 재연결: 다시 열림 → 닫힘으로만 완료
        src.update(DoorState.OPEN)
        assertEquals(0, cb.completedCount)
        src.update(DoorState.CLOSED)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup()
        src.initial(DoorState.OPEN)
        src.update(DoorState.CLOSED) // 완료
        src.update(DoorState.OPEN)
        src.update(DoorState.CLOSED)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 시작직후_안내문구가_제공된다() {
        val (src, cb, _) = setup()
        src.initial(DoorState.OPEN)
        assertTrue(cb.guides.isNotEmpty())
        assertTrue(cb.guides.first().isNotBlank())
    }
}
