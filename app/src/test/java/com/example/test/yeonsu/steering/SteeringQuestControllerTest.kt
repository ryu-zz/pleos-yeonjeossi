package com.example.test.yeonsu.steering

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SteeringQuestController / SteeringLeftSequence 테스트 (가짜 수신원 주입).
 * 왼쪽 회전 감지 완료, 기준 각도 처리, 오른쪽/소량 회전은 미완료, 중단 후 늦은 콜백,
 * 오류 후 기준 재설정, 중복 완료 방지, null 을 검증한다.
 */
class SteeringQuestControllerTest {

    private class FakeSource : SteeringSource {
        var observer: SteeringSource.Observer? = null
        var stopped = false
        override fun start(observer: SteeringSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(v: Float?) = observer?.onInitial(v)
        fun update(v: Float?) = observer?.onUpdate(v)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : SteeringQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(): Triple<FakeSource, RecordingCallback, SteeringQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = SteeringQuestController(src, cb)
        c.start()
        return Triple(src, cb, c)
    }

    // SDK 정의: 왼쪽 = 음수. 따라서 '기준 대비 음의 방향 변화'가 왼쪽이고 완료 조건이다.

    @Test
    fun 왼쪽으로_충분히돌리면_완료() {
        val (src, cb, _) = setup()
        src.initial(0f)
        src.update(-10f)  // 왼쪽이지만 아직 임계(15도) 미만
        assertEquals(0, cb.completedCount)
        src.update(-20f)  // 기준 0 에서 -20 → |변화| ≥ 15, 왼쪽 → 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 기준이_0이_아니어도_상대변화로_판정() {
        val (src, cb, _) = setup()
        src.initial(-100f)  // 시작 각도 -100
        src.update(-110f)   // -10, 미완료
        assertEquals(0, cb.completedCount)
        src.update(-118f)   // -18 ≤ -15 → 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 오른쪽으로_돌리면_완료안됨() {
        val (src, cb, _) = setup()
        src.initial(0f)
        src.update(30f)  // 오른쪽(양수) → 완료 아님
        src.update(50f)
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun null은_완료아님_기준유지() {
        val (src, cb, _) = setup()
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(0f)    // 기준 설정
        src.update(-20f)  // -20 → 왼쪽 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop이후_늦은콜백은_무시() {
        val (src, cb, c) = setup()
        src.initial(0f)
        c.stop()
        assertTrue(src.stopped)
        src.update(-40f)
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun onError후_기준재설정_재연결시_다시판정() {
        val (src, cb, _) = setup()
        src.initial(0f)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        // 재연결: 새 기준을 다시 잡고, 그 기준 대비 왼쪽(음의) 변화로만 완료.
        src.update(-100f)  // 새 기준
        src.update(-110f)  // -10, 미완료
        assertEquals(0, cb.completedCount)
        src.update(-120f)  // -20 → 완료
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup()
        src.initial(0f)
        src.update(-30f)
        src.update(-60f)
        src.update(0f)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 매핑_확인() {
        // 운전대 방향(6-0): SDK 정의(왼쪽=음수)로 좌/우 부호를 확정해 자동 판정 재연결 → 대상.
        assertTrue(SteeringQuestController.isSteeringQuest("vehicle-6-0"))
        assertEquals(false, SteeringQuestController.isSteeringQuest("vehicle-6-1"))
    }
}
