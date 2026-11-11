package com.example.test.yeonsu

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 저장 데이터 이관(QuestMigration) 테스트. 과거 main 목록으로 저장된 데이터를 메모리 FakePrefs 에 만들어 넣고 옮긴다.
 * 모든 테스트는 "옮긴 뒤에도 원래 값이 하나도 사라지지 않았는지"를 함께 확인한다.
 */
class QuestMigrationTest {
    private fun rec(id: String, name: String, at: Long, verified: Boolean = true) =
        JSONObject().put("id", id).put("name", name).put("at", at).put("verified", verified)
    private fun prefsWith(vararg kv: Pair<String, Any>, records: List<JSONObject> = emptyList()): FakePrefs {
        val p = FakePrefs()
        kv.forEach { (k, v) -> p.data[k] = v }
        p.data["records"] = JSONArray(records).toString()
        return p
    }
    private fun records(p: FakePrefs) = JSONArray(p.getString("records", "[]")).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }
    /** 원래 있던 p:/r:/badge 값이 지금 키 또는 legacy: 키 어딘가에 그대로 남아 있는지(값 개수 비교). */
    private fun assertNothingLost(before: Map<String, Any?>, after: Map<String, Any?>) {
        val keys = { m: Map<String, Any?> -> m.filterKeys { k -> k.startsWith("p:") || k.startsWith("r:") || k.startsWith("badge:") || k.startsWith("legacy:") } }
        val b = keys(before).values.map { it.toString() }.sorted()
        val a = keys(after).values.map { it.toString() }
        b.groupingBy { it }.eachCount().forEach { (v, n) -> assertTrue("값 $v 가 사라졌다", a.count { it == v } >= n) }
    }

    @Test fun 변경_전_목록에서_충전구_진행과_기록_뱃지가_새_ID로_옮겨진다() {
        // cdffffb(8개 카테고리) 때 저장: 충전구=vehicle-7-0, 전기차 카테고리=7
        val t = 1_000_000L
        val p = prefsWith(
            "p:vehicle-7-0" to "done|2", "badge:vehicle:7" to t + 50,
            "p:vehicle-1-0" to "done|2",
            records = listOf(rec("vehicle-1-0", "왼쪽 방향지시등 켰다가 끄기", t - 5000), rec("vehicle-7-0", "충전구 열었다가 닫기", t)),
        )
        val before = HashMap(p.data)
        val r = QuestMigration.migrate(p)!!
        assertEquals("vehicle-8-0", r.moved["vehicle-7-0"])
        assertEquals("done|2", p.data["p:vehicle-8-0"])
        assertNull("새 7-0(목적지 음성 검색)에 충전구 진행이 붙어 있으면 안 된다", p.data["p:vehicle-7-0"])
        assertEquals("done|2", p.data["p:vehicle-1-0"])                 // 바뀌지 않은 ID 는 그대로
        val rs = records(p)
        assertEquals(2, rs.size)
        assertEquals("vehicle-8-0", rs[1].getString("id")); assertEquals("vehicle-7-0", rs[1].getString("legacyId"))
        assertEquals("충전구 열었다가 닫기", rs[1].getString("name")); assertEquals(t, rs[1].getLong("at"))
        assertFalse(rs[0].has("legacyId"))
        assertEquals(t + 50, p.data["badge:vehicle:8"])                  // 전기차 뱃지 → 지금 전기차(8)
        assertNull("내비·음성(7) 뱃지로 잘못 보이면 안 된다", p.data["badge:vehicle:7"])
        assertNothingLost(before, p.data)
        // 화면에서 쓰는 저장소로 보면 충전구가 완료, 목적지 음성 검색은 미완료
        val store = QuestStore(p)
        assertEquals("done", store.state(QuestData.quest("vehicle", 8, 0)).status)
        assertEquals("new", store.state(QuestData.quest("vehicle", 7, 0)).status)
        assertTrue(store.hasBadge("vehicle", 8)); assertFalse(store.hasBadge("vehicle", 7))
    }

    @Test fun 지금_목록으로_저장된_데이터는_아무것도_옮기지_않는다() {
        val t = 2_000_000L
        val p = prefsWith(
            "p:vehicle-7-0" to "done|1", "p:vehicle-8-0" to "done|2", "badge:vehicle:7" to t + 10,
            records = listOf(rec("vehicle-8-0", "충전구 열었다가 닫기", t - 9000), rec("vehicle-7-0", "목적지 음성 검색", t)),
        )
        val before = HashMap(p.data)
        val r = QuestMigration.migrate(p)!!
        assertTrue(r.moved.isEmpty()); assertTrue(r.legacy.isEmpty()); assertEquals(0, r.recordsRelinked)
        listOf("p:vehicle-7-0", "p:vehicle-8-0", "badge:vehicle:7", "records").forEach { assertEquals(it, before[it], p.data[it]) }
    }

    @Test fun 바뀐_자리끼리_서로_옮겨도_값이_섞이지_않는다() {
        // d092a57 때: vehicle-5-5 = 시트 통풍 / 지금: 5-5 = 뒷유리 열선, 5-6 = 시트 통풍
        val p = prefsWith(
            "p:vehicle-5-5" to "done|2",
            records = listOf(rec("vehicle-5-5", "시트 통풍 켰다가 끄기", 10L)),
        )
        val before = HashMap(p.data)
        QuestMigration.migrate(p)
        assertEquals("done|2", p.data["p:vehicle-5-6"])
        assertNull("뒷유리 열선이 완료로 보이면 안 된다", p.data["p:vehicle-5-5"])
        assertNothingLost(before, p.data)
    }

    @Test fun 지금_목록에_없는_옛_퀘스트는_legacy로_보관하고_지우지_않는다() {
        // 0eb8031 때(10개 카테고리): vehicle-7-0 = 충전구 열기(지금 없음), vehicle-7-2 = 충전 상태 확인(지금 8-1)
        val p = prefsWith(
            "p:vehicle-7-0" to "done|1", "p:vehicle-7-2" to "done|1",
            records = listOf(rec("vehicle-7-0", "충전구 열기", 1L, false), rec("vehicle-7-2", "충전 상태 확인", 2L, false)),
        )
        val before = HashMap(p.data)
        val r = QuestMigration.migrate(p)!!
        assertEquals("done|1", p.data["legacy:p:vehicle-7-0"])
        assertNull(p.data["p:vehicle-7-0"])
        assertEquals("vehicle-8-1", r.moved["vehicle-7-2"])
        assertEquals("done|1", p.data["p:vehicle-8-1"])
        val rs = records(p)
        assertEquals("vehicle-7-0", rs[0].getString("id"))   // 지금 목록에 없는 퀘스트 기록은 그대로
        assertEquals("vehicle-8-1", rs[1].getString("id"))
        assertNothingLost(before, p.data)
    }

    @Test fun 기록이_없는_진행_중_상태는_자리가_바뀐_ID면_보관하고_안_바뀐_ID면_그대로() {
        val p = prefsWith("p:vehicle-7-1" to "doing|0", "p:vehicle-5-1" to "doing|1", "r:vehicle-5-1" to "0", "p:vehicle-1-0" to "done|2")
        val before = HashMap(p.data)
        val r = QuestMigration.migrate(p)!!
        assertEquals("doing|0", p.data["legacy:p:vehicle-7-1"])            // 7-1 은 과거에 목표 충전량·경유지 추가 등 여러 퀘스트였다
        assertEquals("doing|1", p.data["p:vehicle-5-1"])                   // 5-1 은 과거에도 온도 조절
        assertEquals("0", p.data["r:vehicle-5-1"])
        assertEquals("done|2", p.data["legacy:p:vehicle-1-0"])        // 1-0 은 과거에 "왼쪽 방향지시등"(단일 동작)이라 지금과 다른 퀘스트일 수 있다
        assertNull(p.data["p:vehicle-1-0"])
        assertTrue(r.kept.contains("vehicle-5-1"))
        assertNothingLost(before, p.data)
    }

    @Test fun 같은_ID의_최근_기록이_지금_퀘스트면_그대로_둔다() {
        // 예전 7-0(충전구) 기록 뒤에 새 7-0(목적지 음성 검색)을 다시 완료한 경우
        val p = prefsWith("p:vehicle-7-0" to "done|1",
            records = listOf(rec("vehicle-7-0", "충전구 열었다가 닫기", 1L), rec("vehicle-7-0", "목적지 음성 검색", 2L)))
        QuestMigration.migrate(p)
        assertEquals("done|1", p.data["p:vehicle-7-0"])
        val rs = records(p)
        assertEquals("vehicle-8-0", rs[0].getString("id"))  // 옛 충전구 기록은 지금 충전구 ID 로
        assertEquals("vehicle-7-0", rs[1].getString("id"))
    }

    @Test fun 이름만_바뀐_와이퍼_한_번은_같은_퀘스트로_본다() {
        val p = prefsWith("p:vehicle-2-0" to "done|2", records = listOf(rec("vehicle-2-0", "와이퍼 한 번", 1L)))
        QuestMigration.migrate(p)
        assertEquals("done|2", p.data["p:vehicle-2-0"])
        assertEquals("와이퍼 한 번", records(p)[0].getString("name"))   // 기록의 이름은 바꾸지 않는다
        assertFalse(records(p)[0].has("legacyId"))
    }

    @Test fun 근거_없는_뱃지는_번호가_바뀐_카테고리면_보관하고_안_바뀐_카테고리면_그대로() {
        val p = prefsWith("badge:vehicle:7" to 5L, "badge:vehicle:1" to 6L)
        val before = HashMap(p.data)
        QuestMigration.migrate(p)
        assertEquals(5L, p.data["legacy:badge:vehicle:7"]); assertNull(p.data["badge:vehicle:7"])
        assertEquals(6L, p.data["badge:vehicle:1"])
        assertNothingLost(before, p.data)
    }

    @Test fun 한_번만_옮기고_다시_실행해도_바뀌지_않는다() {
        val p = prefsWith("p:vehicle-7-0" to "done|2", records = listOf(rec("vehicle-7-0", "충전구 열었다가 닫기", 1L)))
        assertTrue(QuestMigration.migrate(p) != null)
        val once = HashMap(p.data)
        assertNull(QuestMigration.migrate(p))
        QuestStore(p)
        assertEquals(once.filterKeys { !it.startsWith("badge:") }, p.data.filterKeys { !it.startsWith("badge:") })
        assertEquals(QuestMigration.SCHEMA, p.data[QuestMigration.KEY])
    }

    @Test fun 과거_목록과_지금_목록의_ID_비교표가_실제_목록과_맞다() {
        // 지금 목록에서 충전구는 8-0, 목적지 음성 검색은 7-0
        assertEquals("vehicle-8-0", QuestMigration.currentIdOf("충전구 열었다가 닫기"))
        assertEquals("vehicle-7-0", QuestMigration.currentIdOf("목적지 음성 검색"))
        assertEquals("vehicle-2-0", QuestMigration.currentIdOf("와이퍼 한 번"))
        assertNull(QuestMigration.currentIdOf("충전구 열기"))
        // 직전 목록(2e9d92f)은 지금과 같은 자리(와이퍼 이름만 다름)
        val prev = QuestMigration.history.last()
        assertEquals("2e9d92f", prev.rev)
        QuestData.cats.getValue("vehicle").indices.forEach { c ->
            QuestData.names("vehicle", c).forEachIndexed { i, n ->
                assertEquals(n, QuestMigration.nameAlias[prev.nameOf("vehicle-$c-$i")] ?: prev.nameOf("vehicle-$c-$i"))
            }
        }
    }
}
