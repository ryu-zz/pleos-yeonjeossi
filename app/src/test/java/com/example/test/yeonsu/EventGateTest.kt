package com.example.test.yeonsu
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
class EventGateTest {
    @Test fun 열기_전과_닫은_뒤_결과는_버린다() {
        val g = EventGate()
        assertFalse(g.accept(0)); assertFalse(g.accept(1))
        val t = g.open()
        assertTrue(g.accept(t))
        g.close()
        assertFalse("닫은 뒤 늦게 온 결과", g.accept(t))
    }
    @Test fun 다시_열면_이전_구간의_늦은_결과는_버린다() {
        val g = EventGate()
        val old = g.open()
        g.close()               // 화면 이탈
        val now = g.open()      // 복귀
        assertFalse("복귀 전에 시작된 인식의 결과", g.accept(old))
        assertTrue(g.accept(now))
    }
    @Test fun 열린채_다시_열어도_이전_번호는_무효() {
        val g = EventGate()
        val a = g.open(); val b = g.open()
        assertFalse(g.accept(a)); assertTrue(g.accept(b)); assertTrue(g.active)
    }
    @Test fun 오류_안내() {
        assertTrue(EventErrors.describe("TIMEOUT").silentRetry)
        val p = EventErrors.describe("PERMISSION_DENIED")
        assertFalse(p.silentRetry); assertTrue(p.message.contains("권한")); assertTrue(p.message.contains("다시 시도"))
        assertTrue(EventErrors.describe("MIC_ERROR").message.contains("마이크"))
        assertTrue(EventErrors.describe("NOT_SUPPORTED_API").message.contains("사용할 수 없어요"))
        assertTrue(EventErrors.describe("뭔가_새_오류").message.contains("뭔가_새_오류"))
        assertEquals(3, EventErrors.MAX_SILENT_RETRY)
    }
}
