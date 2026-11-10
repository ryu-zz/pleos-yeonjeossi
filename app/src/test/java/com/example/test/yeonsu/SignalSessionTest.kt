package com.example.test.yeonsu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 판정 세션(SignalSession) 테스트: 늦은 응답, 다른 퀘스트 응답, 조회 중첩, 화면 이탈 후 복귀.
 */
class SignalSessionTest {
    private val name = "왼쪽 방향지시등 켰다가 끄기"
    private fun session(id: String = "vehicle-1-0") = SignalSession(id) { QuestSignals.find(name) }
    private fun feed(s: SignalSession, stage: Int, v: Any?, t: Long): SignalSession.Result =
        s.onResponse(s.request(t)!!, stage, v)

    @Test fun 중단이나_화면이탈로_닫힌_뒤_도착한_응답은_무시한다() {
        val s = session(); s.begin()
        val t1 = s.request(0)!!
        s.end()                                     // 중단·onStop
        assertEquals(SignalSession.Result.IGNORED, s.onResponse(t1, 0, "LEFT"))
    }
    @Test fun 다시_열린_세션은_이전_세션의_늦은_응답을_받지_않는다() {
        val s = session(); s.begin()
        val old = s.request(0)!!
        s.end(); s.begin()                          // 화면 복귀 → 새 세션
        assertEquals(SignalSession.Result.IGNORED, s.onResponse(old, 0, "LEFT"))
    }
    @Test fun 다른_퀘스트로_보낸_응답은_무시한다() {
        val a = session("vehicle-1-0"); a.begin()
        val b = session("vehicle-1-1"); b.begin()
        val ta = a.request(0)!!
        b.request(0)
        assertEquals(SignalSession.Result.IGNORED, b.onResponse(ta, 0, "RIGHT"))
    }
    @Test fun 앞_조회의_응답이_오기_전에는_새_조회를_보내지_않는다() {
        val s = session(); s.begin()
        assertNotNull(s.request(0))
        assertNull(s.request(800))                  // 아직 응답 없음 → 중첩 금지
        assertNotNull(s.request(SignalSession.STUCK_MS + 1))   // 응답이 사라졌으면 일정 시간 뒤 다시 허용
    }
    @Test fun 더_새_조회가_나간_뒤_늦게_온_옛_응답은_무시한다() {
        val s = session(); s.begin()
        val old = s.request(0)!!
        val fresh = s.request(SignalSession.STUCK_MS + 1)!!
        assertEquals(SignalSession.Result.IGNORED, s.onResponse(old, 0, "LEFT"))
        assertEquals(SignalSession.Result.NONE, s.onResponse(fresh, 0, "OFF"))
    }
    @Test fun 화면이탈_후_복귀하면_이탈_전_값과_이탈_중_변화로는_단계가_끝나지_않는다() {
        val s = session(); s.begin()
        feed(s, 0, "OFF", 0)
        s.end()                                     // 홈 이동: 이 사이 사용자가 LEFT 로 바꿈
        s.begin()                                   // 복귀
        assertEquals("복귀 후 첫 값은 비교 대상(이전 값)이 없어 완료로 치지 않는다", SignalSession.Result.NONE, feed(s, 0, "LEFT", 1000))
        assertEquals(SignalSession.Result.NONE, feed(s, 0, "LEFT", 2000))
        assertEquals(SignalSession.Result.NONE, feed(s, 0, "OFF", 3000))
        assertEquals("복귀 후 직접 켠 것만 인정", SignalSession.Result.STAGE_DONE, feed(s, 0, "LEFT", 4000))
    }
    @Test fun 닫힌_세션은_조회를_보내지_않는다() {
        val s = session(); s.begin(); s.end()
        assertNull(s.request(0))
    }
}
