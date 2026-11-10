package com.example.test.yeonsu

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 진행 상태·수행 기록 저장(QuestStore) 테스트. 실제 JSON 저장 경로를 메모리 FakePrefs 위에서 그대로 쓴다(사용자 기록과 무관).
 */
class QuestStoreTest {
    private val left = QuestData.quest("vehicle", 1, 0)    // 왼쪽 방향지시등 켰다가 끄기(2단계)
    private val right = QuestData.quest("vehicle", 1, 1)   // 오른쪽 방향지시등 켰다가 끄기(2단계)
    private fun records(p: FakePrefs) = JSONArray(p.getString("records", "[]"))
    private fun finish(store: QuestStore, q: Quest): Boolean {
        store.start(q)
        var done = false
        repeat(q.steps.size) { done = store.advance(q, verified = true) }
        return done
    }

    @Test fun 정상_완료는_기록_정확히_한_건() {
        val p = FakePrefs(); val store = QuestStore(p)
        assertTrue(finish(store, left))
        assertEquals(1, records(p).length())
        assertEquals("done", store.state(left).status)
        assertFalse(store.state(left).running)
    }
    @Test fun 완료_후_같은_신호가_와도_기록이_늘지_않는다() {
        val p = FakePrefs(); val store = QuestStore(p)
        finish(store, left)
        repeat(3) { assertFalse(store.advance(left, verified = true)) }
        assertEquals(1, records(p).length())
    }
    @Test fun 수행_전이나_중단_후_응답은_아무것도_남기지_않는다() {
        val p = FakePrefs(); val store = QuestStore(p)
        assertFalse(store.advance(left, verified = true))
        store.start(left); store.advance(left, verified = true); store.stop(left)
        assertFalse(store.advance(left, verified = true))
        assertEquals("new", store.state(left).status)
        assertEquals(0, records(p).length())
    }
    @Test fun 재수행_중단은_완료_기록_뱃지를_그대로_두고_지금_수행만_취소한다() {
        val p = FakePrefs(); val store = QuestStore(p)
        finish(store, left); finish(store, right)          // 방향지시등 카테고리 전부 완료 → 뱃지
        assertTrue(store.hasBadge("vehicle", 1))
        val before = records(p).toString()
        store.start(left)
        assertTrue(store.state(left).running)
        assertEquals("done", store.state(left).status)     // 재수행 중에도 저장 상태는 완료
        store.advance(left, verified = true)               // 1단계까지 진행
        assertEquals(1, store.state(left).step)
        store.stop(left)
        assertEquals("done", store.state(left).status)
        assertFalse(store.state(left).running)
        assertEquals(before, records(p).toString())
        assertTrue(store.hasBadge("vehicle", 1))
        assertTrue(store.categoryDone("vehicle", 1))
        assertEquals(2, store.count("done"))
    }
    @Test fun 재수행을_끝까지_하면_기록이_한_건_더_붙고_완료_상태_유지() {
        val p = FakePrefs(); val store = QuestStore(p)
        finish(store, left)
        assertTrue(finish(store, left))
        assertEquals(2, records(p).length())
        assertEquals("done", store.state(left).status)
        assertFalse(store.state(left).running)
        assertFalse(p.data.containsKey("r:" + left.id))
    }
    @Test fun 재수행_단계는_처음부터_시작하고_진행중_수에_세지_않는다() {
        val p = FakePrefs(); val store = QuestStore(p)
        finish(store, left)
        store.start(left)
        assertEquals(0, store.state(left).step)
        assertEquals(0, store.count("doing"))
        assertEquals(1, store.count("done"))
    }
    @Test fun 처음_수행_중단은_시작_전으로_돌아간다() {
        val p = FakePrefs(); val store = QuestStore(p)
        store.start(left); store.advance(left)
        store.stop(left)
        assertEquals("new", store.state(left).status)
        assertEquals(0, records(p).length())
    }
    @Test fun 저장된_기록은_새_저장소로_다시_열어도_그대로다() {
        val p = FakePrefs()
        finish(QuestStore(p), left)
        val again = QuestStore(p)   // 앱을 다시 켠 것과 같은 상황
        assertEquals("done", again.state(left).status)
        assertEquals(1, again.records().size)
        assertTrue(again.records()[0].verified)
    }
}
