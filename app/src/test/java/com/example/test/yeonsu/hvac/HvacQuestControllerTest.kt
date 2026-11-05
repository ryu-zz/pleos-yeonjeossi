package com.example.test.yeonsu.hvac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * HvacQuestController 테스트 (가짜 수신원 주입).
 * 조작 연습형(AC_ON 등)과 상태 확인형(TEMP_22, FAN_3) 판정, 외기(목표=해제),
 * 시작 상태 처리, 중단 후 늦은 콜백, 오류 후 초기화, 중복 완료 방지, null 을 검증한다.
 *
 * 주의: 수신원이 각 값을 "목표 충족 여부(isTarget)"로 바꿔 올리므로, 테스트는 isTarget 불리언으로 구동한다.
 */
class HvacQuestControllerTest {

    private class FakeSource : HvacSource {
        var observer: HvacSource.Observer? = null
        var stopped = false
        override fun start(observer: HvacSource.Observer) { this.observer = observer }
        override fun stop() { stopped = true }
        fun initial(v: Boolean?) = observer?.onInitial(v)
        fun update(v: Boolean?) = observer?.onUpdate(v)
        fun error(reason: String) = observer?.onError(reason)
    }

    private class RecordingCallback : HvacQuestController.Callback {
        var completedCount = 0
        var errorCount = 0
        override fun onStageChanged(guide: String) {}
        override fun onCompleted() { completedCount++ }
        override fun onError(reason: String) { errorCount++ }
    }

    private fun setup(q: HvacQuest): Triple<FakeSource, RecordingCallback, HvacQuestController> {
        val src = FakeSource()
        val cb = RecordingCallback()
        val c = HvacQuestController(src, cb, q)
        c.start()
        return Triple(src, cb, c)
    }

    // ───── 조작 연습형 (AC_ON) ─────

    @Test
    fun 에어컨_정상완료_끔에서_켬() {
        val (src, cb, _) = setup(HvacQuest.AC_ON)
        src.initial(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
        assertEquals(0, cb.errorCount)
    }

    @Test
    fun 에어컨_시작이미켬이면_바로완료안됨_껐다켜야완료() {
        val (src, cb, _) = setup(HvacQuest.AC_ON)
        src.initial(true)
        assertEquals(0, cb.completedCount)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 에어컨_켬반복만으로는_완료안됨() {
        val (src, cb, _) = setup(HvacQuest.AC_ON)
        src.initial(true)
        src.update(true)
        assertEquals(0, cb.completedCount)
    }

    // ───── 상태 확인형 (TEMP_22, FAN_3) ─────

    @Test
    fun 온도_시작이미22도면_바로완료() {
        val (src, cb, _) = setup(HvacQuest.TEMP_22)
        src.initial(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 온도_시작은다른값_22도도달시_완료() {
        val (src, cb, _) = setup(HvacQuest.TEMP_22)
        src.initial(false)
        assertEquals(0, cb.completedCount)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 풍량_시작이미3단계면_바로완료() {
        val (src, cb, _) = setup(HvacQuest.FAN_3)
        src.initial(true)
        assertEquals(1, cb.completedCount)
    }

    // ───── 외기 모드 (목표 = recirc 해제, isTarget 로 환산되어 들어옴) ─────

    @Test
    fun 외기_정상완료_내기순환켬에서_해제() {
        val (src, cb, _) = setup(HvacQuest.RECIRC_OFF)
        src.initial(false) // 지금 외기 아님(내기순환 켜짐) → isTarget=false
        src.update(true)   // 외기로 전환 → isTarget=true
        assertEquals(1, cb.completedCount)
    }

    // ───── 공통 규칙 ─────

    @Test
    fun null은_완료아님() {
        val (src, cb, _) = setup(HvacQuest.MIRROR_HEAT_ON)
        src.initial(null)
        src.update(null)
        assertEquals(0, cb.completedCount)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun stop이후_늦은콜백은_무시() {
        val (src, cb, c) = setup(HvacQuest.AC_ON)
        src.initial(false)
        c.stop()
        assertTrue(src.stopped)
        src.update(true)
        assertEquals(0, cb.completedCount)
    }

    @Test
    fun onError후_초기화_재연결시_다시판정() {
        val (src, cb, _) = setup(HvacQuest.SEAT_VENT_ON)
        src.initial(false)
        src.error("연결 끊김")
        assertEquals(1, cb.errorCount)
        // 재연결 첫 값이 target 이어도(조작 연습형) 바로 완료 아님.
        src.update(true)
        assertEquals(0, cb.completedCount)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 완료후_추가콜백와도_완료는_1회() {
        val (src, cb, _) = setup(HvacQuest.AC_ON)
        src.initial(false)
        src.update(true)
        src.update(false)
        src.update(true)
        assertEquals(1, cb.completedCount)
    }

    @Test
    fun 퀘스트ID_매핑_확인() {
        assertTrue(HvacQuestController.isHvacQuest("vehicle-5-0"))
        assertTrue(HvacQuestController.isHvacQuest("vehicle-5-8"))
        assertEquals(HvacQuest.FAN_3, HvacQuestController.questKind("vehicle-5-2"))
        // 송풍 방향(5-3)은 보류이므로 대상 아님.
        assertEquals(false, HvacQuestController.isHvacQuest("vehicle-5-3"))
    }
}
