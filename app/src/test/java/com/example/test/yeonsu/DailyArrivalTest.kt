package com.example.test.yeonsu
import com.example.test.yeonsu.DailyArrival.Result
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class DailyArrivalTest {
    private val gas = listOf("주유소", "주유소")
    @Test fun 고른_목적지와_다른_곳에_도착하면_완료하지_않는다() {
        assertEquals(Result.MISMATCH, DailyArrival.judge(gas, "GS칼텍스 강남주유소", "스타벅스 강남점"))
    }
    @Test fun 고른_곳이고_이름에_장소_종류가_있으면_내비_확인() {
        assertEquals(Result.MATCHED, DailyArrival.judge(gas, "GS칼텍스 강남주유소", "GS칼텍스 강남 주유소"))
        assertEquals(Result.MATCHED, DailyArrival.judge(listOf("패스트푸드점", "맥도날드"), "맥도날드 역삼DT점", "맥도날드 역삼DT점"))
    }
    @Test fun 이름만으로_장소_종류를_모르면_사용자_확인() {
        assertEquals(Result.UNCONFIRMED, DailyArrival.judge(gas, "SK에너지 역삼점", "SK에너지 역삼점"))
        assertEquals("고른 목적지를 모르면 확정하지 않는다", Result.UNCONFIRMED, DailyArrival.judge(gas, null, "강남주유소"))
        assertEquals(Result.UNKNOWN, DailyArrival.judge(gas, "강남주유소", ""))
        assertEquals(Result.UNKNOWN, DailyArrival.judge(gas, "강남주유소", null))
    }
    @Test fun 완료_연속_탭은_기록_하나만_남긴다_그리고_완료_방식을_구분한다() {
        val p = FakePrefs()
        val s = QuestStore(p)
        val q = QuestData.quest("daily", 0, 0)
        assertTrue(s.complete(q, source = QuestStore.SOURCE_NAVI, nowMs = 10_000))
        assertFalse(s.complete(q, source = QuestStore.SOURCE_NAVI, nowMs = 10_300))
        assertFalse(s.complete(q, source = QuestStore.SOURCE_NAVI, nowMs = 12_999))
        assertEquals(1, JSONArray(p.getString("records", "[]")).length())
        assertTrue("시간이 지난 재수행은 새 기록", s.complete(q, source = QuestStore.SOURCE_USER, nowMs = 20_000))
        val r = s.records()
        assertEquals(2, r.size)
        assertEquals(QuestStore.SOURCE_USER, r[0].source); assertEquals(QuestStore.SOURCE_NAVI, r[1].source)
        assertFalse(r[0].verified); assertFalse(r[1].verified)
        assertEquals("done", s.state(q).status)
    }
    @Test fun 예전_기록은_완료_방식이_없다() {
        val p = FakePrefs(); p.data["records"] = """[{"id":"daily-0-0","name":"x","at":1,"verified":false}]"""
        assertEquals(null, QuestStore(p).records()[0].source)
    }
}
