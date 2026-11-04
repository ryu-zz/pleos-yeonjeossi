package com.example.test.yeonsu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 팀 HTML 시제품(app.js)의 퀘스트 데이터를 그대로 옮긴 것. 화면 확인용 예시 목록이다. */
class Quest(val id: String, val name: String, val c: Int, val steps: List<String>, val desc: String,
            val doneMsg: String? = null, val avail: String? = null)
/** 상세 튜토리얼.xlsx 한 줄: 이름, 앱 안내, 완료 안내, 데이터 상태. */
class Tutorial(val name: String, val guide: String, val done: String, val avail: String)

object QuestData {
    val cats = mapOf(
        "vehicle" to listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문·문·트렁크", "공조·시야", "주행 기능", "전기차 기능", "내비·음성", "비상상황"),
        "daily" to listOf("차 관리", "드라이브스루", "주차", "고속도로", "보호구역", "생활 이동", "특수 도로", "시간·날씨", "도전"),
    )
    private val vehicleSymbols = listOf("◉", "↔", "☂", "☀", "▱", "♨", "◎", "⚡", "⌖", "△")
    private val dailySymbols = listOf("◉", "↔", "☂", "☀", "▱", "♨", "◎", "⌖", "△")
    fun symbol(type: String, c: Int) = (if (type == "vehicle") vehicleSymbols else dailySymbols)[c]
    val vehicleTutorials: List<List<Tutorial>> = listOf(
        listOf(
            Tutorial("운전석 앞뒤 조절", "페달을 편하게 밟을 수 있도록 좌석을 조절해보세요.", "좋아요. 좌석 위치를 조절했어요.", "확인 가능"),
            Tutorial("좌석 높이", "도로가 잘 보이도록 좌석 높이를 조절해보세요.", "좋아요. 좌석 높이를 조절했어요.", "확인 가능"),
            Tutorial("등받이", "등이 좌석에 닿도록 등받이를 조절해보세요.", "좋아요. 등받이를 조절했어요.", "확인 가능"),
            Tutorial("안전벨트", "운전석 안전벨트를 착용하세요.", "좋아요. 안전벨트를 착용했어요.", "확인 가능"),
            Tutorial("문 확인", "운전석 문을 닫아주세요.", "좋아요. 문이 닫혔어요.", "확인 가능"),
            Tutorial("브레이크 위치", "브레이크를 밟아보세요.", "좋아요. 이 페달이 브레이크예요.", "확인 가능"),
            Tutorial("액셀 위치", "화면에서 액셀 페달 위치를 확인하세요.", "액셀 위치를 확인했어요.", "설명만 가능"),
        ),
        listOf(
            Tutorial("왼쪽 방향지시등", "레버를 아래로 내려 왼쪽 방향지시등을 켜보세요.", "좋아요. 왼쪽 방향지시등을 켰어요.", "확인 가능"),
            Tutorial("오른쪽 방향지시등", "레버를 위로 올려 오른쪽 방향지시등을 켜보세요.", "좋아요. 오른쪽 방향지시등을 켰어요.", "확인 가능"),
            Tutorial("방향지시등 끄기", "레버를 원래 위치로 돌려보세요.", "좋아요. 방향지시등을 껐어요.", "확인 가능"),
            Tutorial("차선 변경 깜빡이", "레버를 살짝 움직여 세 번 깜빡이게 해보세요.", "좋아요. 차선 변경 때 사용하는 기능이에요.", "부분 가능"),
        ),
        listOf(
            Tutorial("와이퍼 한 번", "와이퍼를 한 번 작동해보세요.", "좋아요. 와이퍼가 한 번 작동했어요.", "확인 가능"),
            Tutorial("자동 와이퍼", "레버를 AUTO 위치에 놓아보세요.", "좋아요. 자동 와이퍼를 켰어요.", "확인 가능"),
            Tutorial("저속 와이퍼", "레버를 LOW 위치에 놓아보세요.", "좋아요. 저속 와이퍼를 켰어요.", "확인 가능"),
            Tutorial("고속 와이퍼", "레버를 HIGH 위치에 놓아보세요.", "좋아요. 고속 와이퍼를 켰어요.", "확인 가능"),
            Tutorial("와이퍼 끄기", "와이퍼를 OFF 위치로 돌려주세요.", "좋아요. 원래 상태로 돌렸어요.", "확인 가능"),
        ),
        listOf(
            Tutorial("자동 전조등", "조명 스위치를 AUTO에 맞춰보세요.", "좋아요. 자동 전조등을 설정했어요.", "부분 가능"),
            Tutorial("하향등", "전조등을 켜보세요.", "좋아요. 전조등을 켰어요.", "확인 가능"),
            Tutorial("상향등", "레버를 밀어 상향등을 켜보세요.", "좋아요. 상향등을 켰어요.", "확인 가능"),
            Tutorial("상향등 끄기", "상향등을 꺼주세요.", "좋아요. 상향등을 껐어요.", "확인 가능"),
        ),
        listOf(
            Tutorial("운전석 창문", "운전석 창문을 조금 내려보세요.", "좋아요. 창문을 열었어요.", "확인 가능"),
            Tutorial("창문 닫기", "운전석 창문을 완전히 닫아보세요.", "좋아요. 창문을 닫았어요.", "확인 가능"),
            Tutorial("뒷좌석 창문 잠금", "창문 잠금 버튼을 눌러보세요.", "좋아요. 뒷좌석 창문을 잠갔어요.", "확인 가능"),
            Tutorial("전체 문 잠금", "전체 문 잠금 버튼을 눌러보세요.", "좋아요. 모든 문을 잠갔어요.", "확인 가능"),
            Tutorial("트렁크", "뒤쪽 공간을 확인하고 트렁크 버튼을 눌러보세요.", "좋아요. 트렁크를 열었어요.", "부분 가능"),
        ),
        listOf(
            Tutorial("에어컨", "A/C 버튼을 눌러보세요.", "좋아요. 에어컨을 켰어요.", "확인 가능"),
            Tutorial("온도 조절", "운전석 온도를 22도로 맞춰보세요.", "좋아요. 22도로 설정했어요.", "확인 가능"),
            Tutorial("풍량 조절", "풍량을 3단계로 맞춰보세요.", "좋아요. 풍량을 조절했어요.", "확인 가능"),
            Tutorial("송풍 방향", "바람 방향을 얼굴 쪽으로 바꿔보세요.", "좋아요. 바람 방향을 바꿨어요.", "확인 가능"),
            Tutorial("내기순환", "내기순환 버튼을 눌러보세요.", "좋아요. 내기순환을 켰어요.", "확인 가능"),
            Tutorial("외기 모드", "다시 외기 모드로 바꿔보세요.", "좋아요. 외기 모드로 바꿨어요.", "확인 가능"),
            Tutorial("앞유리 성에 제거", "앞유리 모양 버튼을 눌러보세요.", "좋아요. 성에 제거 기능을 켰어요.", "확인 가능"),
            Tutorial("사이드미러 열선", "사이드미러 열선을 켜보세요.", "좋아요. 열선을 켰어요.", "확인 가능"),
            Tutorial("시트 열선·통풍", "시트 통풍을 1단계로 켜보세요.", "좋아요. 시트 통풍을 켰어요.", "확인 가능"),
        ),
        listOf(
            Tutorial("운전대 방향", "운전대를 왼쪽으로 조금 돌려보세요.", "좋아요. 운전대 방향을 확인했어요.", "확인 가능"),
            Tutorial("주행 모드", "주행 모드를 ECO로 변경해보세요.", "좋아요. ECO 모드로 변경했어요.", "확인 가능"),
            Tutorial("크루즈 버튼", "화면에서 크루즈 버튼 위치와 사용 조건을 확인하세요.", "크루즈 버튼 위치를 확인했어요.", "부분 가능"),
            Tutorial("전자식 주차 브레이크", "전자식 주차 브레이크 버튼 위치를 확인하세요.", "버튼 위치를 확인했어요.", "확인 필요"),
        ),
        listOf(
            Tutorial("충전구 열기", "충전구 열기 버튼을 눌러보세요.", "충전구를 열었어요.", "자동 판정 어려움"),
            Tutorial("목표 충전량", "목표 충전량을 80%로 설정해보세요.", "좋아요. 80%로 설정했어요.", "부분 가능"),
            Tutorial("충전 상태 확인", "현재 배터리 잔량과 충전 상태를 확인하세요.", "현재 충전 상태를 확인했어요.", "확인 가능"),
        ),
        listOf(
            Tutorial("목적지 음성 검색", "마이크 버튼을 누르고 목적지를 말해보세요.", "좋아요. 목적지를 찾았어요.", "부분 가능"),
            Tutorial("경유지 추가", "가는 길에 들를 장소를 추가해보세요.", "좋아요. 경유지를 추가했어요.", "확인 가능"),
            Tutorial("주유소·충전소 검색", "가까운 충전소 찾아줘라고 말해보세요.", "좋아요. 가까운 충전소를 찾았어요.", "부분 가능"),
        ),
        listOf(
            Tutorial("타이어 경고등 연습", "타이어 경고등이 켜지면 안전한 곳에 정차하고 점검하세요.", "대처 순서를 확인했어요.", "가상 연습 가능"),
            Tutorial("엔진오일 경고 연습", "엔진오일 경고가 켜지면 안전한 곳에 정차하고 점검하세요.", "대처 순서를 확인했어요.", "가상 연습 가능"),
            Tutorial("브레이크 경고 연습", "브레이크 경고가 켜지면 안전한 곳에 정차하고 긴급출동을 요청하세요.", "대처 순서를 확인했어요.", "가상 연습 가능"),
            Tutorial("접촉사고 연습", "비상등을 켜고 안전한 곳에 정차한 뒤 현장을 기록하세요.", "사고 대응 순서를 확인했어요.", "가상 연습 가능"),
            Tutorial("고속도로 고장 연습", "정차 후 차량 밖 안전한 곳으로 대피하고 신고하세요.", "고장 대응 순서를 확인했어요.", "가상 연습 가능"),
            Tutorial("에어백 전개 연습", "부상자를 확인하고 119에 신고하세요.", "긴급 대응 순서를 확인했어요.", "가상 연습 가능"),
        ),
    )
    private val daily = listOf(
        listOf("출발 전 차량 한 바퀴", "주유·충전 준비 확인", "타이어 상태 살펴보기"),
        listOf("주문 전 동선 확인", "결제와 수령 준비"),
        listOf("주차선 안에 정차", "하차 전 주변 확인", "주차 위치 기억하기"),
        listOf("진입 전 경로 확인", "출구 미리 확인하기"),
        listOf("보호구역 표지 알아보기", "보행자 주변 살피기"),
        listOf("익숙한 목적지 다녀오기", "동승자와 출발 준비"),
        listOf("좁은 길 미리 살피기", "지하주차장 시야 확보"),
        listOf("야간 출발 준비", "비 오는 날 출발 준비"),
        listOf("출발 준비 혼자 해보기", "익숙한 경로 돌아보기"),
    )

    fun names(type: String, c: Int): List<String> = if (type == "vehicle") vehicleTutorials[c].map { it.name } else daily[c]

    fun quest(type: String, c: Int, i: Int): Quest {
        val isV = type == "vehicle"
        if (isV) {
            val t = vehicleTutorials[c][i]
            return Quest("$type-$c-$i", t.name, c, listOf(t.guide), t.guide, t.done, t.avail)
        }
        val name = names(type, c)[i]
        val steps = when {
            else -> listOf("안내 내용 살펴보기", "필요한 동작 확인하기", "확인 내용 돌아보기")
        }
        val desc = "한 단계씩 익히고, 내 차에 조금 더 익숙해져요."
        return Quest("$type-$c-$i", name, c, steps, desc)
    }
}

/** 화면 사이에서 "지금 어떤 종류/카테고리/퀘스트를 보고 있는지" 유지한다(HTML의 type, category, selected). */
object QuestSession {
    var type = "vehicle"
    var category = 0
    var questIndex = 0
    fun selected(): Quest = QuestData.quest(type, category, questIndex)
}

/**
 * 퀘스트 진행·완료 기록 저장(SharedPreferences "yeonsu_quest").
 * 지금은 화면 확인용 미리보기 기록이다. 차량 SDK 관찰값으로 판정한 기록이 아니다.
 */
class QuestStore(context: Context) {
    class State(val status: String, val step: Int)
    class Record(val id: String, val name: String, val at: Long)

    private val prefs = context.getSharedPreferences("yeonsu_quest", Context.MODE_PRIVATE)

    fun state(q: Quest): State {
        val raw = prefs.getString("p:" + q.id, null) ?: return State("new", 0)
        val p = raw.split("|")
        return State(p.getOrNull(0) ?: "new", p.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    fun start(q: Quest) {
        if (state(q).status != "doing") prefs.edit().putString("p:" + q.id, "doing|0").apply()
    }

    fun stop(q: Quest) { prefs.edit().remove("p:" + q.id).apply() }

    /** 다음 단계로 넘긴다. 마지막 단계면 완료 기록을 남기고 true. */
    fun advance(q: Quest): Boolean {
        val s = state(q)
        if (s.status != "doing") return false
        val next = s.step + 1
        if (next >= q.steps.size) {
            prefs.edit().putString("p:" + q.id, "done|$next").apply()
            val arr = JSONArray(prefs.getString("records", "[]"))
            arr.put(JSONObject().put("id", q.id).put("name", q.name).put("at", System.currentTimeMillis()))
            prefs.edit().putString("records", arr.toString()).apply()
            return true
        }
        prefs.edit().putString("p:" + q.id, "doing|$next").apply()
        return false
    }

    fun count(status: String): Int = prefs.all.entries.count { it.key.startsWith("p:") && (it.value as? String)?.startsWith("$status|") == true }

    /** 최근 순. */
    fun records(): List<Record> {
        val arr = JSONArray(prefs.getString("records", "[]"))
        return (0 until arr.length()).map { val o = arr.getJSONObject(it); Record(o.getString("id"), o.getString("name"), o.getLong("at")) }.reversed()
    }
}
