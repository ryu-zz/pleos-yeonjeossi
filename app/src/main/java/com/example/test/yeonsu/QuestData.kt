package com.example.test.yeonsu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 팀 HTML 시제품(app.js)의 퀘스트 데이터를 그대로 옮긴 것. 화면 확인용 예시 목록이다. */
class Quest(val id: String, val name: String, val c: Int, val steps: List<String>, val desc: String,
            val doneMsg: String? = null, val avail: String? = null, val tip: String? = null)
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

    val tips = mapOf(
        "운전석 앞뒤 조절" to "페달을 끝까지 밟아도 무릎이 살짝 굽혀지는 위치!",
        "좌석 높이" to "앞 도로가 잘 보이고, 머리와 천장 사이에 주먹 하나가 들어갈 높이!",
        "등받이" to "어깨와 등이 시트에 닿고, 팔을 뻗으면 손목이 핸들 위에 걸치는 각도!",
        "안전벨트" to "어깨 벨트는 목 옆, 허리 벨트는 골반 아래로 '딸깍' 소리가 날 때까지!",
        "문 확인" to "계기판의 문 열림 표시가 꺼졌는지 꼭 확인!",
        "브레이크 위치" to "두 페달 중 넓은 쪽(왼쪽)이 브레이크, 오른발로 밟아요!",
        "액셀 위치" to "브레이크 오른쪽의 좁은 페달! 처음엔 살살, 발뒤꿈치는 바닥에!",
        "왼쪽 방향지시등" to "핸들을 돌리기 전에 먼저! 레버를 아래로 내리면 왼쪽이에요.",
        "오른쪽 방향지시등" to "핸들을 돌리기 전에 먼저! 레버를 위로 올리면 오른쪽이에요.",
        "방향지시등 끄기" to "보통 핸들이 돌아오면 저절로 꺼지지만, 안 꺼졌으면 직접 돌려놓기!",
        "차선 변경 깜빡이" to "레버를 살짝만 건드리면 세 번 깜빡! 차선을 옮기기 전에 써요.",
        "와이퍼 한 번" to "레버를 살짝 올렸다 놓으면 딱 한 번만 쓱! 가볍게 닦을 때 좋아요.",
        "자동 와이퍼" to "비 감지 센서가 알아서 속도를 맞춰줘요. 빗길에 가장 편해요!",
        "저속 와이퍼" to "가랑비나 이슬비처럼 약한 비일 때 쓰는 단계!",
        "고속 와이퍼" to "앞이 안 보일 만큼 센 비일 때만! 시야가 트이면 바로 낮추기!",
        "와이퍼 끄기" to "비가 그치면 OFF로! 마른 유리에서 돌리면 유리가 긁혀요.",
        "자동 전조등" to "어두워지면 알아서 켜져요. 평소엔 AUTO에 두면 끝!",
        "하향등" to "터널과 밤길의 기본 불빛! 앞차에 눈부심을 주지 않아요.",
        "상향등" to "레버를 앞으로 밀기! 앞차나 마주 오는 차가 없을 때만 써요.",
        "상향등 끄기" to "앞차나 마주 오는 차가 보이면 바로! 레버를 원래대로 돌려요.",
        "운전석 창문" to "버튼을 누르고 있는 만큼 내려가요. 손이나 물건이 끼지 않게 확인!",
        "창문 닫기" to "버튼을 위로 당겨 끝까지! 닫히는 동안 손과 얼굴을 창에서 떼기!",
        "뒷좌석 창문 잠금" to "아이가 탈 땐 필수! 뒷좌석 창문을 운전석에서만 조작할 수 있어요.",
        "전체 문 잠금" to "자물쇠 버튼 한 번이면 모든 문이 잠겨요. 출발 전 습관으로!",
        "트렁크" to "열기 전에 뒤에 사람이나 물건이 없는지 확인! 닫을 땐 손 끼임 조심!",
        "에어컨" to "A/C 버튼에 불이 켜지면 냉방과 습기 제거가 시작돼요!",
        "온도 조절" to "여름엔 24℃ 안팎이 쾌적해요. 너무 낮추면 오히려 피곤해요!",
        "풍량 조절" to "처음엔 세게, 시원해지면 낮춰요. 너무 세면 건조해져요!",
        "송풍 방향" to "얼굴 쪽은 시원하게, 발 쪽은 따뜻하게, 앞유리 쪽은 김서림 제거!",
        "내기순환" to "터널이나 매연 많은 길에서 쓰기! 오래 켜두면 졸리니 가끔 환기!",
        "외기 모드" to "평소엔 외기 모드! 실내 공기가 계속 신선하게 바뀌어요.",
        "앞유리 성에 제거" to "앞유리에 김이 서려 안 보일 때 바로! 에어컨과 같이 켜면 더 빨라요.",
        "사이드미러 열선" to "비 오는 날과 겨울 아침, 거울 김서림을 없애 뒤쪽 시야를 확보!",
        "시트 열선·통풍" to "겨울엔 열선, 여름엔 통풍! 1단계부터 약하게 시작해요.",
        "운전대 방향" to "왼쪽으로 돌리면 차도 왼쪽으로! 한 번에 크게 말고 부드럽게!",
        "주행 모드" to "ECO는 부드럽게 연비 위주! 처음엔 ECO나 일반 모드가 좋아요.",
        "크루즈 버튼" to "막히지 않는 길에서 속도를 유지해줘요. 브레이크를 밟으면 바로 해제돼요!",
        "전자식 주차 브레이크" to "P 버튼 하나로 주차 브레이크 작동! 주차할 땐 꼭 확인!",
        "충전구 열기" to "충전기 가까이 세우고 버튼 한 번! 충전구 앞은 비워두기!",
        "목표 충전량" to "평소엔 80%까지가 배터리에 좋아요. 장거리 갈 때만 100%!",
        "충전 상태 확인" to "남은 %와 완료까지 걸리는 시간을 함께 보기!",
        "목적지 음성 검색" to "마이크 버튼을 누르고 장소 이름만 또박또박! 운전 중엔 음성으로!",
        "경유지 추가" to "목적지는 그대로 두고, 가는 길에 들를 곳만 끼워 넣어요!",
        "주유소·충전소 검색" to "'가까운 충전소 찾아줘'처럼 말하면 돼요. 잔량 20% 전에 미리 찾기!",
        "타이어 경고등 연습" to "급브레이크는 금물! 천천히 속도를 줄여 안전한 곳에서 점검!",
        "엔진오일 경고 연습" to "계속 달리면 엔진이 상해요. 바로 안전한 곳에 세우기!",
        "브레이크 경고 연습" to "무리하게 달리지 말고 세운 뒤 긴급출동 부르기!",
        "접촉사고 연습" to "비상등 켜고 안전한 곳에 세우기 → 사진 찍기 → 보험사 연락!",
        "고속도로 고장 연습" to "차 안에 있지 말고 가드레일 밖으로 대피! 삼각대는 차 뒤쪽 멀리!",
        "에어백 전개 연습" to "부상자부터 확인하고 119 신고! 시동은 끄기!",
    )
    fun names(type: String, c: Int): List<String> = if (type == "vehicle") vehicleTutorials[c].map { it.name } else daily[c]

    fun quest(type: String, c: Int, i: Int): Quest {
        val isV = type == "vehicle"
        if (isV) {
            val t = vehicleTutorials[c][i]
            return Quest("$type-$c-$i", t.name, c, listOf(t.guide), t.guide, t.done, t.avail, tips[t.name])
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
