package com.example.test.yeonsu.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DoorLockQuestController 테스트 (가짜 수신원 주입).
 * 전체 잠김 정상 완료, 시작 이미잠김 처리, 중단 후 늦은 콜백, 오류 후 초기화, 중복 완료 방지, null 처리.
 */
class DoorLockQuestControllerTest {

    private class FakeSource : DoorLockSource {
        var observer: DoorLockSource.Observer? = null
        var stopped = false
        override fun start(observer: DoorLockSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(v: Boolean?) = observer?.onInitial(v)
        fun update(v: Boolean?) = observer?.onUpdate(v)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : DoorLockQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(): Triple<FakeSource, RecordingCallback, DoorLockQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = DoorLockQuestController(src, cb)
        c.start()
        return Triple(src, cb, c)
    }

    @Test
    fun 정상완료_미잠김에서_전체잠김() {
        val (src, cb, _) = setup()
        src.initial(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 시작이미잠김이면_바로완료안됨_풀렸다잠겨야완료() {
        val (src, cb, _) = setup()
        src.initial(true)
        assertEquals(0, cb.completedCount)
        src.update(false) // 한 짝 풀림
        src.update(true)  // 다시 전체 잠김
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
    fun stop이후_늦은잠김은_무시() {
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
        src.update(true) // 재연결 첫 값이 잠김이어도 바로 완료 아님(이전 진행 이어받기 금지)
        assertEquals(0, cb.completedCount)
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
    fun 관련없는_미잠김반복으로는_완료안됨() {
        val (src, cb, _) = setup()
        src.initial(false)
        src.update(false)
        src.update(false)
        assertEquals(0, cb.completedCount)
    }
}
