package com.example.test.yeonsu.polling

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PolledQuestController 테스트 (가짜 PollSource 주입, SDK 비의존).
 *
 * 폴링 퀘스트는 모두 조작 연습형이다. 수신원은 "현재 조회값이 목표인가(isTarget)"를 올리므로
 * 테스트는 그 불리언으로 구동한다(실제 주차 게이트·직렬 조회·늦은 응답 무효화는 VehiclePolledSource 책임).
 *
 * 검증:
 * - 목표 조작 → 완료(단, 시작이 이미 목표면 다른 상태를 거쳐야 함).
 * - 다른 모드(비목표)만으로는 미완료.
 * - 중단 후 늦은 응답 무시(미완료).
 * - null·오류 후 초기화·중복 완료 방지.
 */
class PolledQuestControllerTest {

    private class FakeSource : PollSource {
        var observer: PollSource.Observer? = null
        var stopped = false
        override fun start(observer: PollSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(v: Boolean?) = observer?.onInitial(v)
        fun update(v: Boolean?) = observer?.onUpdate(v)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : PolledQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        var lastGuide: String? = null
        override fun onStageChanged(guide: String) { lastGuide = guide }
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(q: PollQuest): Triple<FakeSource, RecordingCallback, PolledQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = PolledQuestController(src, cb, q)
        c.start()
        return Triple(src, cb, c)
    }

    // ───── 목표 조작 → 완료 ─────

    @Test
    fun 송풍방향_정상완료_다른방향에서_얼굴쪽으로() {
        val (src, cb, _) = setup(PollQuest.FAN_DIR_FACE)
        src.initial(false) // 지금 얼굴 쪽 아님
        src.update(true)   // 얼굴 쪽으로 변경
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 자동와이퍼_정상완료() {
        val (src, cb, _) = setup(PollQuest.WIPER_AUTO)
        src.initial(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 저속와이퍼_정상완료() {
        val (src, cb, _) = setup(PollQuest.WIPER_LOW)
        src.initial(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 고속와이퍼_정상완료() {
        val (src, cb, _) = setup(PollQuest.WIPER_HIGH)
        src.initial(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    // ───── 조작 연습형: 시작이 이미 목표면 다른 상태를 거쳐야 ─────

    @Test
    fun 시작이미목표면_바로완료안됨_다른값거쳐_다시목표여야완료() {
        val (src, cb, _) = setup(PollQuest.WIPER_HIGH)
        src.initial(true)  // 이미 고속
        assertEquals(0, cb.completedCount)
        src.update(true)   // 계속 고속만: 완료 아님
        assertEquals(0, cb.completedCount)
        src.update(false)  // 다른 단으로
        src.update(true)   // 다시 고속
        assertEquals(1, cb.completedCount)
    }

    // ───── 다른 모드(비목표)만으로는 미완료 ─────

    @Test
    fun 다른모드만으로는_완료안됨() {
        val (src, cb, _) = setup(PollQuest.WIPER_AUTO)
        src.initial(false)
        src.update(false)  // 저속/고속/OFF 등 비목표 조회
        src.update(false)
        assertEquals(0, cb.completedCount)
    }

    // ───── 중단 후 늦은 응답 무시 ─────

    @Test
    fun stop이후_늦은응답은_무시() {
        val (src, cb, c) = setup(PollQuest.FAN_DIR_FACE)
        src.initial(false)
        c.stop()
        assertTrue(src.stopped)
        src.update(true)   // 멈춘 뒤 도착한 조회 결과
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun 중단_도중목표응답이늦게와도_완료안됨() {
        val (src, cb, c) = setup(PollQuest.WIPER_LOW)
        src.initial(false)
        src.update(false)
        c.stop()
        src.update(true)   // 중단 후 지연 응답
        assertEquals(0, cb.completedCount)
    }

    // ───── null·오류·중복 완료 ─────

    @Test
    fun null은_완료아님() {
        val (src, cb, _) = setup(PollQuest.FAN_DIR_FACE)
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun onError후_초기화_재조회시_다시판정() {
        val (src, cb, _) = setup(PollQuest.WIPER_AUTO)
        src.initial(false)
        src.error("주차(P) 상태에서만 확인할 수 있어요.")
        assertEquals(1, cb.errorCount)
        // 재개 첫 값이 목표여도 바로 완료(조작 연습형이라 초기값 취급) 아님.
        src.update(true)
        assertEquals(0, cb.completedCount)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가조회와도_완료는_1회() {
        val (src, cb, _) = setup(PollQuest.FAN_DIR_FACE)
        src.initial(false)
        src.update(true)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    // ───── 퀘스트 ID 매핑 ─────

    @Test
    fun 퀘스트ID_매핑_확인() {
        assertTrue(PolledQuestController.isPollQuest("vehicle-5-3"))
        assertTrue(PolledQuestController.isPollQuest("vehicle-2-1"))
        assertTrue(PolledQuestController.isPollQuest("vehicle-2-2"))
        assertTrue(PolledQuestController.isPollQuest("vehicle-2-3"))
        assertEquals(PollQuest.FAN_DIR_FACE, PolledQuestController.questKind("vehicle-5-3"))
        assertEquals(PollQuest.WIPER_AUTO, PolledQuestController.questKind("vehicle-2-1"))
        assertEquals(PollQuest.WIPER_LOW, PolledQuestController.questKind("vehicle-2-2"))
        assertEquals(PollQuest.WIPER_HIGH, PolledQuestController.questKind("vehicle-2-3"))
        // 와이퍼 한 번(2-0)·끄기(2-4)는 폴링 대상 아님(기존 WiperState 경로).
        assertFalse(PolledQuestController.isPollQuest("vehicle-2-0"))
        assertFalse(PolledQuestController.isPollQuest("vehicle-2-4"))
    }

    // ───── 토큰 판정(목표 집합) ─────

    @Test
    fun 토큰_목표판정() {
        assertTrue(PollQuest.FAN_DIR_FACE.isTarget("FACE"))
        assertTrue(PollQuest.FAN_DIR_FACE.isTarget("FACE_AND_FLOOR"))
        assertFalse(PollQuest.FAN_DIR_FACE.isTarget("FLOOR"))
        assertFalse(PollQuest.FAN_DIR_FACE.isTarget("DEFROST"))
        assertFalse(PollQuest.FAN_DIR_FACE.isTarget(null))
        assertTrue(PollQuest.WIPER_AUTO.isTarget("AUTO"))
        assertFalse(PollQuest.WIPER_AUTO.isTarget("OFF"))
        assertTrue(PollQuest.WIPER_LOW.isTarget("CONTINUOUS_LEVEL_1"))
        assertTrue(PollQuest.WIPER_HIGH.isTarget("CONTINUOUS_LEVEL_3"))
        assertFalse(PollQuest.WIPER_HIGH.isTarget("CONTINUOUS_LEVEL_1"))
    }
}
