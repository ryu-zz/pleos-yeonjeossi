package com.example.test.yeonsu
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
/** 음성 퀘스트 문장 판정 규칙. 실제 음성 인식은 장치에서 따로 확인해야 한다(여기서는 인식된 문장이 주어졌다고 본다). */
class QuestEventsTest {
    @Test fun 목적지처럼_들리는_문장은_통과() {
        listOf("서울역으로 가자", "서울역", "강남역까지", "시청 가줘", "삼성병원으로", "집으로 안내해줘", "맛집 가고 싶어").forEach {
            assertTrue("$it 는 통과해야 한다", QuestEvents.isDestination(it))
        }
    }
    @Test fun 무관한_문장은_통과하지_않는다() {
        listOf("오늘 날씨 어때", "역사 이야기 해줘", "음악 틀어줘", "볼륨 줄여", "역", "").forEach {
            assertFalse("$it 는 통과하면 안 된다", QuestEvents.isDestination(it))
        }
    }
    @Test fun 주유소_충전소_문장() {
        assertTrue(QuestEvents.isStationQuery("가까운 충전소 찾아줘"))
        assertTrue(QuestEvents.isStationQuery("근처 주유 소 말고 주유소"))
        assertTrue(QuestEvents.isStationQuery("충 전소 알려줘"))
        assertFalse(QuestEvents.isStationQuery("서울역으로 가자"))
        assertFalse(QuestEvents.isStationQuery("충전 상태 알려줘"))
    }
}
