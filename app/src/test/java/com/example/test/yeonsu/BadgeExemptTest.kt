package com.example.test.yeonsu

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 자동 판정 보류 중인 크루즈를 뱃지 조건에서 빼는 규칙. 기존 데이터를 지우지 않는지 함께 본다. */
class BadgeExemptTest {
    private val steer = QuestData.quest("vehicle", 6, 0)    // 운전대 왼쪽으로 돌렸다가 풀기
    private val cruise = QuestData.quest("vehicle", 6, 1)   // 크루즈 켰다가 끄기(보류)

    private fun finish(store: QuestStore, q: Quest): Boolean {
        store.start(q); var d = false; repeat(q.steps.size) { d = store.advance(q, verified = true) }; return d
    }

    @Test fun 크루즈만_뱃지_조건에서_빠진다() {
        assertEquals("크루즈 켰다가 끄기", cruise.name)
        assertTrue(badgeExempt(cruise))
        assertFalse(badgeExempt(steer))
        val exempt = QuestData.cats.getValue("vehicle").indices.flatMap { c -> QuestData.names("vehicle", c).indices.map { QuestData.quest("vehicle", c, it) } }.filter(::badgeExempt)
        assertEquals(listOf("크루즈 켰다가 끄기"), exempt.map { it.name })
        // 일상 퀘스트는 영향 없음
        assertFalse(badgeExempt(QuestData.quest("daily", 0, 0)))
    }

    @Test fun 운전대만_완료해도_주행_기능_뱃지를_받는다() {
        val p = FakePrefs(); val store = QuestStore(p)
        assertFalse(store.categoryDone("vehicle", 6))
        assertTrue(finish(store, steer))
        assertTrue(store.categoryDone("vehicle", 6))
        assertTrue(store.hasBadge("vehicle", 6))
        assertEquals("new", store.state(cruise).status)          // 크루즈 상태는 건드리지 않는다
    }

    @Test fun 규칙이_바뀌기_전에_운전대를_완료한_사용자는_다음_실행에서_뱃지를_받고_기존_데이터는_그대로다() {
        val p = FakePrefs()
        p.data["p:vehicle-6-0"] = "done|2"
        p.data["records"] = JSONArray().put(org.json.JSONObject().put("id", "vehicle-6-0").put("name", steer.name).put("at", 5L).put("verified", true)).toString()
        val before = HashMap(p.data)
        val store = QuestStore(p)                              // 이관 + 바뀐 뱃지 조건 확인
        assertTrue(store.hasBadge("vehicle", 6))
        assertFalse("팝업은 띄우지 않는다", p.data.containsKey("pendingBadge"))
        before.forEach { (k, v) -> assertEquals(k, v, p.data[k]) }   // 원래 키·값은 그대로
    }

    @Test fun 크루즈를_예전에_완료한_기록과_상태도_지우지_않는다() {
        val p = FakePrefs()
        p.data["p:vehicle-6-1"] = "done|2"
        p.data["records"] = JSONArray().put(org.json.JSONObject().put("id", "vehicle-6-1").put("name", cruise.name).put("at", 5L).put("verified", true)).toString()
        val store = QuestStore(p)
        assertEquals("done", store.state(cruise).status)
        assertEquals(1, store.records().size)
    }
}
