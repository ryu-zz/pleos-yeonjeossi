package com.example.test.yeonsu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 차량 퀘스트 판정(QuestSignals) 테스트. SDK 없이 판정 함수에 값 순서를 직접 넣는다.
 * 실제 화면과 같은 방식(SignalSession: 이전 값 → 지금 값, 단계마다 진행)으로 돌린다.
 */
class QuestSignalsJudgeTest {
    /** 퀘스트 하나를 처음부터 수행하면서 값을 차례로 넣는다. 끝난 단계 수를 돌려준다. */
    private fun run(name: String, vararg values: Any?): Int {
        val q = QuestData.vehicleTutorials.flatten().first { it.name == name }
        val stepsTotal = q.steps?.size ?: 1
        val s = SignalSession("test") { QuestSignals.find(name) }
        s.begin()
        var stage = 0
        var t = 0L
        for (v in values) {
            val ticket = s.request(t) ?: error("조회가 막혔다")
            t += 1000
            if (s.onResponse(ticket, stage, v) == SignalSession.Result.STAGE_DONE) {
                stage++
                if (stage >= stepsTotal) return stage
            }
        }
        return stage
    }
    private fun steps(name: String) = QuestData.vehicleTutorials.flatten().first { it.name == name }.steps?.size ?: 1

    // ── 이번에 실패했던 사례: 다른 모드·강도 변경은 끄기가 아니다 ──
    @Test fun 왼쪽_방향지시등_켠뒤_오른쪽은_끄기가_아니다() {
        assertEquals(1, run("왼쪽 방향지시등 켰다가 끄기", "OFF", "LEFT", "RIGHT"))
    }
    @Test fun 왼쪽_방향지시등_켠뒤_OFF는_완료() {
        assertEquals(2, run("왼쪽 방향지시등 켰다가 끄기", "OFF", "LEFT", "OFF"))
        assertEquals(2, run("왼쪽 방향지시등 켰다가 끄기", "OFF", "LEFT", "RIGHT", "OFF"))   // 오른쪽을 거쳐도 실제 OFF 에서만 끝난다
    }
    @Test fun 오른쪽_방향지시등_켠뒤_왼쪽은_끄기가_아니다() {
        assertEquals(1, run("오른쪽 방향지시등 켰다가 끄기", "OFF", "RIGHT", "LEFT"))
        assertEquals(2, run("오른쪽 방향지시등 켰다가 끄기", "OFF", "RIGHT", "OFF"))
    }
    @Test fun 자동_와이퍼_AUTO에서_저속은_끄기가_아니다() {
        assertEquals(1, run("자동 와이퍼 켰다가 끄기", "OFF", "AUTO", "CONTINUOUS_LEVEL_1"))
        assertEquals(1, run("자동 와이퍼 켰다가 끄기", "OFF", "AUTO", "CONTINUOUS_LEVEL_3"))
        assertEquals(2, run("자동 와이퍼 켰다가 끄기", "OFF", "AUTO", "OFF"))
    }
    @Test fun 저속_와이퍼_저속에서_고속은_끄기가_아니다() {
        assertEquals(1, run("저속 와이퍼 켰다가 끄기", "OFF", "CONTINUOUS_LEVEL_1", "CONTINUOUS_LEVEL_3"))
        assertEquals(1, run("저속 와이퍼 켰다가 끄기", "OFF", "CONTINUOUS_LEVEL_1", "AUTO"))
        assertEquals(2, run("저속 와이퍼 켰다가 끄기", "OFF", "CONTINUOUS_LEVEL_1", "OFF"))
    }
    @Test fun 고속_와이퍼_고속에서_저속은_끄기가_아니다() {
        assertEquals(1, run("고속 와이퍼 켰다가 끄기", "OFF", "CONTINUOUS_LEVEL_3", "CONTINUOUS_LEVEL_1"))
        assertEquals(2, run("고속 와이퍼 켰다가 끄기", "OFF", "CONTINUOUS_LEVEL_3", "OFF"))
    }
    @Test fun 시트_통풍_강도변경과_열선은_끄기가_아니다() {
        assertEquals(1, run("시트 통풍 켰다가 끄기", 0, -1, -2))
        assertEquals(1, run("시트 통풍 켰다가 끄기", 0, -1, 1))
        assertEquals(2, run("시트 통풍 켰다가 끄기", 0, -1, 0))
        assertEquals(2, run("시트 통풍 켰다가 끄기", 0, -1, -2, 0))
    }
    @Test fun 와이퍼_켰다가_끄기는_작동상태_ON에서_OFF만_완료() {
        assertEquals(2, run("와이퍼 켰다가 끄기", "OFF", "ON", "OFF"))
        assertEquals(1, run("와이퍼 켰다가 끄기", "OFF", "ON", "OTHER"))   // 알 수 없는 상태는 끄기가 아니다
    }

    // ── 운전대: 기준은 가운데(0도), 왼쪽 = 음수 ──
    @Test fun 운전대_왼쪽후_오른쪽으로_크게는_복귀가_아니다() {
        assertEquals(1, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, -200f, 300f))
        assertEquals(1, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, -200f, 6f))      // 허용 범위(±5) 밖
    }
    @Test fun 운전대_왼쪽후_가운데_복귀는_완료() {
        assertEquals(2, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, -200f, 0f))
        assertEquals(2, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, -20f, 4f))
        assertEquals(2, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, -200f, 300f, 0f)) // 오른쪽을 거쳐도 가운데에 와야 끝난다
    }
    @Test fun 운전대_시작이_가운데가_아니면_먼저_가운데를_봐야한다() {
        assertEquals(0, run("운전대 왼쪽으로 돌렸다가 풀기", -100f, -200f))       // 이미 돌아간 채 시작: 왼쪽으로 더 가도 1단계 아님
        assertEquals(1, run("운전대 왼쪽으로 돌렸다가 풀기", -100f, 0f, -200f))   // 가운데를 본 뒤 왼쪽
        assertEquals(0, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, -14f))           // 15도 미만은 왼쪽 아님
        assertEquals(0, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, 200f, 0f))       // 오른쪽은 1단계 아님
    }
    @Test fun 운전대_판정기는_수행마다_새로_만든다() {
        // 앞 수행에서 가운데를 봤던 기억이 다음 수행으로 넘어가지 않는다
        assertEquals(1, run("운전대 왼쪽으로 돌렸다가 풀기", 0f, -200f))
        assertEquals(0, run("운전대 왼쪽으로 돌렸다가 풀기", -200f, -250f))
    }

    // ── 크루즈: 대기(ENABLED)는 꺼짐이 아니다, 신뢰할 수 있는 꺼짐 값이 없어 자동 판정 보류 ──
    @Test fun 크루즈는_자동판정_보류이고_어떤_값으로도_완료되지_않는다() {
        val d = QuestSignals.find("크루즈 켰다가 끄기")
        assertNotNull(d)
        assertNotNull(d!!.pendingReason)
        val s = SignalSession("cruise") { QuestSignals.find("크루즈 켰다가 끄기") }
        s.begin()
        assertFalse("보류 판정기는 세션을 열지 않는다", s.active)
        assertEquals(null, s.request(0))
        assertFalse(d.done(0, "ENABLED", "ACTIVATED") || d.done(1, "ACTIVATED", "ENABLED") || d.done(1, "ACTIVATED", "OTHER"))
    }

    // ── 공통 판정 함수를 쓰는 모든 차량 퀘스트의 정상 흐름(값 순서는 실제 SDK 값 표기) ──
    private val normal: Map<String, List<Any>> = mapOf(
        "운전석 문 열었다가 닫기" to listOf(0, 100, 0),
        "브레이크 밟았다가 떼기" to listOf(false, true, false),
        "왼쪽 방향지시등 켰다가 끄기" to listOf("OFF", "LEFT", "OFF"),
        "오른쪽 방향지시등 켰다가 끄기" to listOf("OFF", "RIGHT", "OFF"),
        "와이퍼 켰다가 끄기" to listOf("OFF", "ON", "OFF"),
        "자동 와이퍼 켰다가 끄기" to listOf("OFF", "AUTO", "OFF"),
        "저속 와이퍼 켰다가 끄기" to listOf("OFF", "CONTINUOUS_LEVEL_1", "OFF"),
        "고속 와이퍼 켰다가 끄기" to listOf("OFF", "CONTINUOUS_LEVEL_3", "OFF"),
        "리어 와이퍼 켰다가 끄기" to listOf("OFF", "ON", "OFF"),
        "하향등 켰다가 끄기" to listOf("OFF", "ON", "OFF"),
        "상향등 켰다가 끄기" to listOf("OFF", "ON", "OFF"),
        "운전석 창문 열었다가 닫기" to listOf(0, 50, 0),
        "전체 문 풀었다가 잠그기" to listOf(true, false, true),
        "트렁크 열었다가 닫기" to listOf(0, 100, 0),
        "프렁크 열었다가 닫기" to listOf(0, 100, 0),
        "에어컨 켰다가 끄기" to listOf(false, true, false),
        "온도 조절" to listOf(24f, 22f),
        "송풍 방향" to listOf("FLOOR", "FACE"),
        "내기순환 켰다가 외기로" to listOf(false, true, false),
        "앞유리 성에 제거 켰다가 끄기" to listOf(false, true, false),
        "뒷유리 열선 켰다가 끄기" to listOf(false, true, false),
        "시트 통풍 켰다가 끄기" to listOf(0, -1, 0),
        "운전대 왼쪽으로 돌렸다가 풀기" to listOf(0f, -200f, 0f),
        "충전구 열었다가 닫기" to listOf(false, true, false),
        "충전 상태 확인" to listOf(77, 78),   // 팀 추가: 충전율 값이 바뀌면 완료
    )
    @Test fun 자동판정_차량퀘스트는_모두_정상흐름으로_완료되고_목록과_판정기가_일치한다() {
        val auto = QuestSignals.names - "크루즈 켰다가 끄기"
        assertEquals("정상 흐름 표가 판정기 목록과 같아야 한다", auto, normal.keys)
        val listed = QuestData.vehicleTutorials.flatten().map { it.name }.toSet()
        assertTrue("판정기 이름은 모두 실제 퀘스트 목록에 있어야 한다: ${QuestSignals.names - listed}", listed.containsAll(QuestSignals.names))
        normal.forEach { (name, vals) -> assertEquals("$name 정상 흐름", steps(name), run(name, *vals.toTypedArray())) }
    }
    @Test fun 정상흐름도_켜기만으로는_완료되지_않는다() {
        normal.filter { steps(it.key) == 2 }.forEach { (name, vals) ->
            assertEquals("$name 켜기만", 1, run(name, *vals.dropLast(1).toTypedArray()))
        }
    }
    @Test fun 값을_못읽으면_판정하지_않는다() {
        assertEquals(1, run("왼쪽 방향지시등 켰다가 끄기", "OFF", "LEFT", null, null))
        assertEquals(2, run("왼쪽 방향지시등 켰다가 끄기", "OFF", "LEFT", null, "OFF"))
    }
    @Test fun 와이퍼_한번_이름은_켰다가_끄기로_바뀌고_ID_위치는_그대로() {
        val wipers = QuestData.vehicleTutorials[2].map { it.name }
        assertEquals("와이퍼 켰다가 끄기", wipers[0])                      // vehicle-2-0 자리 그대로
        assertEquals("와이퍼 켰다가 끄기", QuestData.quest("vehicle", 2, 0).name)
        assertEquals("vehicle-2-0", QuestData.quest("vehicle", 2, 0).id)
        assertFalse(QuestData.vehicleTutorials.flatten().any { it.name == "와이퍼 한 번" })
    }
}
