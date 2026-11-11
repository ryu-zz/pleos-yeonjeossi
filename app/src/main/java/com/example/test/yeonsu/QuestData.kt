package com.example.test.yeonsu

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** 팀 HTML 시제품(app.js)의 퀘스트 데이터를 그대로 옮긴 것. 화면 확인용 예시 목록이다. */
class Quest(val id: String, val name: String, val c: Int, val steps: List<String>, val desc: String,
            val doneMsg: String? = null, val avail: String? = null, val tip: String? = null)
/** 상세 튜토리얼.xlsx 한 줄: 이름, 앱 안내, 완료 안내, 데이터 상태. */
/** 일상 퀘스트.xlsx 한 줄: 이름, 난이도(별), 직접 완료 여부. */
class DailyQ(val name: String, val stars: String, val manual: Boolean)
/** steps: 한 퀘스트 안의 단계(예: 켜보세요 → 꺼보세요). 없으면 한 단계. */
class Tutorial(val name: String, val guide: String, val done: String, val avail: String, val steps: List<String>? = null)

object QuestData {
    val cats = mapOf(
        "vehicle" to listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "내비·음성", "전기차"),
        "daily" to listOf("차 관리", "드라이브스루", "주차", "고속도로", "보호구역", "생활 이동", "특수 도로", "시간·날씨", "도전"),
    )
    val vehicleTutorials: List<List<Tutorial>> = listOf(
        listOf(
            Tutorial("운전석 문 열었다가 닫기", "운전석 문을 열었다가 닫아보세요.", "좋아요. 문을 열었다가 닫았어요.", "확인 가능", listOf("운전석 문을 열어보세요", "운전석 문을 닫아보세요")),
            Tutorial("브레이크 밟았다가 떼기", "브레이크를 밟았다가 발을 떼어보세요.", "좋아요. 이 페달이 브레이크예요.", "확인 가능", listOf("브레이크를 밟아보세요", "브레이크에서 발을 떼어보세요")),
        ),
        listOf(
            Tutorial("왼쪽 방향지시등 켰다가 끄기", "왼쪽 방향지시등을 켰다가 꺼보세요.", "좋아요. 왼쪽 방향지시등을 켰다가 껐어요.", "확인 가능", listOf("레버를 아래로 내려 왼쪽 방향지시등을 켜보세요", "레버를 원래 위치로 돌려 꺼보세요")),
            Tutorial("오른쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등을 켰다가 꺼보세요.", "좋아요. 오른쪽 방향지시등을 켰다가 껐어요.", "확인 가능", listOf("레버를 위로 올려 오른쪽 방향지시등을 켜보세요", "레버를 원래 위치로 돌려 꺼보세요")),
        ),
        listOf(
            Tutorial("와이퍼 켰다가 끄기", "와이퍼를 켰다가 꺼보세요.", "좋아요. 와이퍼를 켰다가 껐어요.", "확인 가능", listOf("레버를 올려 와이퍼를 켜보세요", "레버를 내려 와이퍼를 꺼보세요")),
            Tutorial("자동 와이퍼 켰다가 끄기", "자동 와이퍼를 켰다가 꺼보세요.", "좋아요. 자동 와이퍼를 켰다가 껐어요.", "확인 가능", listOf("레버를 AUTO 위치에 놓아보세요", "레버를 OFF 위치로 돌려 꺼보세요")),
            Tutorial("저속 와이퍼 켰다가 끄기", "저속 와이퍼를 켰다가 꺼보세요.", "좋아요. 저속 와이퍼를 켰다가 껐어요.", "확인 가능", listOf("레버를 LOW 위치에 놓아보세요", "레버를 OFF 위치로 돌려 꺼보세요")),
            Tutorial("고속 와이퍼 켰다가 끄기", "고속 와이퍼를 켰다가 꺼보세요.", "좋아요. 고속 와이퍼를 켰다가 껐어요.", "확인 가능", listOf("레버를 HIGH 위치에 놓아보세요", "레버를 OFF 위치로 돌려 꺼보세요")),
            Tutorial("리어 와이퍼 켰다가 끄기", "뒷유리 와이퍼(리어 와이퍼)를 켰다가 꺼보세요.", "좋아요. 리어 와이퍼를 켰다가 껐어요.", "확인 가능", listOf("리어 와이퍼를 켜보세요", "리어 와이퍼를 꺼보세요")),
        ),
        listOf(
            Tutorial("하향등 켰다가 끄기", "하향등(전조등)을 켰다가 꺼보세요.", "좋아요. 전조등을 켰다가 껐어요.", "확인 가능", listOf("전조등을 켜보세요", "전조등을 꺼보세요")),
            Tutorial("상향등 켰다가 끄기", "상향등을 켰다가 꺼보세요.", "좋아요. 상향등을 켰다가 껐어요.", "확인 가능", listOf("레버를 밀어 상향등을 켜보세요", "상향등을 꺼보세요")),
        ),
        listOf(
            Tutorial("운전석 창문 열었다가 닫기", "운전석 창문을 내렸다가 완전히 닫아보세요.", "좋아요. 창문을 열었다가 닫았어요.", "확인 가능", listOf("운전석 창문을 조금 내려보세요", "운전석 창문을 완전히 닫아보세요")),
            Tutorial("전체 문 풀었다가 잠그기", "전체 문 잠금을 풀었다가 다시 잠가보세요.", "좋아요. 모든 문을 풀었다가 잠갔어요.", "확인 가능", listOf("잠금 해제 버튼으로 문을 풀어보세요", "전체 문 잠금 버튼으로 다시 잠가보세요")),
            Tutorial("트렁크 열었다가 닫기", "뒤쪽 공간을 확인하고 트렁크를 열었다가 닫아보세요.", "좋아요. 트렁크를 열었다가 닫았어요.", "부분 가능", listOf("트렁크를 열어보세요", "트렁크를 닫아보세요")),
            Tutorial("프렁크 열었다가 닫기", "앞쪽 짐칸(프렁크)을 열었다가 닫아보세요.", "좋아요. 프렁크를 열었다가 닫았어요.", "확인 가능", listOf("프렁크를 열어보세요", "프렁크를 닫아보세요")),
        ),
        listOf(
            Tutorial("에어컨 켰다가 끄기", "A/C 버튼으로 에어컨을 켰다가 꺼보세요.", "좋아요. 에어컨을 켰다가 껐어요.", "확인 가능", listOf("A/C 버튼을 눌러 에어컨을 켜보세요", "A/C 버튼을 다시 눌러 꺼보세요")),
            Tutorial("온도 조절", "운전석 온도를 22도로 맞춰보세요.", "좋아요. 22도로 설정했어요.", "확인 가능"),
            Tutorial("송풍 방향", "바람 방향을 얼굴 쪽으로 바꿔보세요.", "좋아요. 바람 방향을 바꿨어요.", "확인 가능"),
            Tutorial("내기순환 켰다가 외기로", "내기순환을 켰다가 다시 외기 모드로 바꿔보세요.", "좋아요. 내기순환을 켰다가 외기로 돌렸어요.", "확인 가능", listOf("내기순환 버튼을 눌러보세요", "다시 외기 모드로 바꿔보세요")),
            Tutorial("앞유리 성에 제거 켰다가 끄기", "앞유리 성에 제거를 켰다가 꺼보세요.", "좋아요. 성에 제거를 켰다가 껐어요.", "확인 가능", listOf("앞유리 모양 버튼을 눌러 켜보세요", "버튼을 다시 눌러 꺼보세요")),
            Tutorial("뒷유리 열선 켰다가 끄기", "뒷유리 열선을 켰다가 꺼보세요.", "좋아요. 뒷유리 열선을 켰다가 껐어요.", "확인 가능", listOf("뒷유리 열선 버튼을 눌러 켜보세요", "버튼을 다시 눌러 꺼보세요")),
            Tutorial("시트 통풍 켰다가 끄기", "시트 통풍을 1단계로 켰다가 꺼보세요.", "좋아요. 시트 통풍을 켰다가 껐어요.", "확인 가능", listOf("시트 통풍을 1단계로 켜보세요", "시트 통풍을 꺼보세요")),
        ),
        listOf(
            Tutorial("운전대 왼쪽으로 돌렸다가 풀기", "운전대를 왼쪽으로 조금 돌렸다가 가운데로 돌려놓아 보세요.", "좋아요. 운전대 방향을 확인했어요.", "확인 가능", listOf("운전대를 왼쪽으로 조금 돌려보세요", "운전대를 가운데로 돌려놓아 보세요")),
            Tutorial("크루즈 켰다가 끄기", "크루즈 컨트롤을 켰다가 꺼보세요.", "좋아요. 크루즈를 켰다가 껐어요.", "확인 가능", listOf("크루즈 버튼으로 크루즈를 켜보세요", "크루즈를 꺼보세요")),
        ),
        // 내비·음성 (내 브랜치에서 가져옴): 음성 인식(SpeechToText)과 내비(NaviHelper) 신호로 자동 완료. 전기차는 맨 마지막이어야 해서 그 앞에 둔다.
        listOf(
            Tutorial("목적지 음성 검색", "목적지를 말해보세요. 말한 문장만 확인하고 검색은 실행하지 않아요.", "좋아요. 목적지를 말했어요.", "부분 가능"),
            Tutorial("경유지 추가", "가는 길에 들를 장소를 추가해보세요.", "좋아요. 경유지를 추가했어요.", "확인 가능"),
            Tutorial("주유소·충전소 검색", "가까운 충전소 찾아줘라고 말해보세요. 말한 문장만 확인하고 검색은 실행하지 않아요.", "좋아요. 충전소를 찾는 말을 했어요.", "부분 가능"),
        ),
        listOf(
            Tutorial("충전구 열었다가 닫기", "충전구를 열었다가 닫아보세요.", "충전구를 열었다가 닫았어요.", "자동 판정 어려움", listOf("충전구 열기 버튼을 눌러보세요", "충전구를 닫아보세요")),
            Tutorial("충전 상태 확인", "현재 배터리 잔량과 충전 상태를 확인하세요.", "현재 충전 상태를 확인했어요.", "확인 가능"),
        ),
    )
    val dailyQuests: List<List<DailyQ>> = listOf(
        listOf(
            DailyQ("셀프 주유소에서 직접 주유하기", "★★", false),
            DailyQ("자동 세차장 통과하기", "★★", false),
            DailyQ("셀프 세차장에서 손세차하기", "★★", false),
            DailyQ("타이어 공기압 넣고 오기", "★★", true),
            DailyQ("정비소에서 엔진오일 교체하고 오기", "★★", false),
            DailyQ("전기차 급속충전소에서 충전하기", "★★", false),
        ),
        listOf(
            DailyQ("패스트푸드 드라이브스루 주문하기", "★★", false),
            DailyQ("카페 드라이브스루 주문하기", "★★", false),
            DailyQ("대형마트 온라인 주문 차량 픽업하기", "★★", false),
            DailyQ("약국 드라이브스루 이용하기", "★★★", false),
        ),
        listOf(
            DailyQ("대형마트 지하주차장에 주차하고 장보기", "★★", false),
            DailyQ("백화점 나선형 주차장 오르내리기", "★★★", false),
            DailyQ("기계식 주차타워 이용하기", "★★★", true),
            DailyQ("노상 공영주차장에 주차하고 정산하기", "★★", true),
            DailyQ("아파트 지하주차장 방문 등록하고 주차하기", "★★", true),
        ),
        listOf(
            DailyQ("하이패스 톨게이트 통과하기", "★★★", false),
            DailyQ("일반 차로에서 통행료 직접 내기", "★★★", false),
            DailyQ("휴게소에서 쉬고 다시 출발하기", "★★★", false),
            DailyQ("졸음쉼터 들렀다 가기", "★★★", false),
            DailyQ("분기점에서 다른 고속도로로 갈아타기", "★★★★", false),
        ),
        listOf(
            DailyQ("등교 시간 어린이보호구역 통과하기", "★★", false),
            DailyQ("노인보호구역 통과하기", "★★", false),
            DailyQ("학원가 도로 지나가기", "★★★", false),
            DailyQ("전통시장 앞 혼잡한 도로 지나가기", "★★★", false),
        ),
        listOf(
            DailyQ("기차역·버스터미널에서 사람 태워 오기", "★★★", false),
            DailyQ("공항 출국장 앞에서 내려주기", "★★★", false),
            DailyQ("대형병원에서 환자 내려주고 주차하기", "★★★", false),
            DailyQ("아이 학원에서 데려오기", "★★", false),
            DailyQ("택배 보내러 우체국 다녀오기", "★★", false),
        ),
        listOf(
            DailyQ("한강 다리 건너기", "★★", false),
            DailyQ("긴 터널 통과하기", "★★★", false),
            DailyQ("산 고갯길 넘어가기", "★★★", false),
            DailyQ("해안도로 드라이브하기", "★★", false),
            DailyQ("도심 고가도로 달리기", "★★★", false),
        ),
        listOf(
            DailyQ("해 진 뒤 야간 드라이브하기", "★★★", false),
            DailyQ("비 오는 날 장보고 오기", "★★★", false),
            DailyQ("출근 시간 막히는 길 운전하기", "★★★", false),
            DailyQ("새벽 한적한 도로 달려보기", "★★", false),
        ),
        listOf(
            DailyQ("처음 가보는 도시 다녀오기", "★★★★", false),
            DailyQ("왕복 100km 드라이브하기", "★★★★", false),
            DailyQ("친구 태우고 맛집 다녀오기", "★★★", false),
            DailyQ("캠핑장 다녀오기", "★★★★", false),
        ),
    )
    val tips = mapOf(
        "운전석 문 열었다가 닫기" to "계기판의 문 열림 표시가 꺼졌는지 꼭 확인!",
        "브레이크 밟았다가 떼기" to "두 페달 중 넓은 쪽(왼쪽)이 브레이크, 오른발로 밟아요!",
        "왼쪽 방향지시등 켰다가 끄기" to "핸들을 돌리기 전에 먼저! 레버를 아래로 내리면 왼쪽이에요.",
        "오른쪽 방향지시등 켰다가 끄기" to "핸들을 돌리기 전에 먼저! 레버를 위로 올리면 오른쪽이에요.",
        "와이퍼 켰다가 끄기" to "비가 그치면 바로 OFF로! 마른 유리에서 돌리면 유리가 긁혀요.",
        "자동 와이퍼 켰다가 끄기" to "비 감지 센서가 알아서 속도를 맞춰줘요. 빗길에 가장 편해요!",
        "저속 와이퍼 켰다가 끄기" to "가랑비나 이슬비처럼 약한 비일 때 쓰는 단계!",
        "고속 와이퍼 켰다가 끄기" to "앞이 안 보일 만큼 센 비일 때만! 시야가 트이면 바로 낮추기!",
        "리어 와이퍼 켰다가 끄기" to "뒷유리에 비나 물방울이 맺혔을 때 켜요! 마른 유리에서는 오래 켜두지 않기!",
        "하향등 켰다가 끄기" to "터널과 밤길의 기본 불빛! 앞차에 눈부심을 주지 않아요.",
        "상향등 켰다가 끄기" to "레버를 앞으로 밀기! 앞차나 마주 오는 차가 없을 때만 써요.",
        "운전석 창문 열었다가 닫기" to "버튼을 누르고 있는 만큼 내려가요. 손이나 물건이 끼지 않게 확인!",
        "전체 문 풀었다가 잠그기" to "자물쇠 버튼 한 번이면 모든 문이 잠겨요. 출발 전 습관으로!",
        "트렁크 열었다가 닫기" to "열기 전에 뒤에 사람이나 물건이 없는지 확인! 닫을 땐 손 끼임 조심!",
        "프렁크 열었다가 닫기" to "앞쪽 짐칸이에요! 열기 전에 앞에 사람이나 물건이 없는지 확인하고, 닫을 땐 손 끼임 조심!",
        "에어컨 켰다가 끄기" to "A/C 버튼에 불이 켜지면 냉방과 습기 제거가 시작돼요!",
        "온도 조절" to "여름엔 24℃ 안팎이 쾌적해요. 너무 낮추면 오히려 피곤해요!",
        "송풍 방향" to "얼굴 쪽은 시원하게, 발 쪽은 따뜻하게, 앞유리 쪽은 김서림 제거!",
        "내기순환 켰다가 외기로" to "터널이나 매연 많은 길에서 쓰기! 오래 켜두면 졸리니 가끔 환기!",
        "앞유리 성에 제거 켰다가 끄기" to "앞유리에 김이 서려 안 보일 때 바로! 에어컨과 같이 켜면 더 빨라요.",
        "뒷유리 열선 켰다가 끄기" to "뒷유리에 김이 서리거나 성에가 꼈을 때 켜요! 다 녹으면 꺼서 전기를 아끼기!",
        "시트 통풍 켰다가 끄기" to "겨울엔 열선, 여름엔 통풍! 1단계부터 약하게 시작해요.",
        "운전대 왼쪽으로 돌렸다가 풀기" to "왼쪽으로 돌리면 차도 왼쪽으로! 한 번에 크게 말고 부드럽게!",
        "크루즈 켰다가 끄기" to "막히지 않는 길에서 속도를 유지해줘요. 브레이크를 밟으면 바로 해제돼요!",
        "충전구 열었다가 닫기" to "충전기 가까이 세우고 버튼 한 번! 충전구 앞은 비워두기!",
        "충전 상태 확인" to "남은 %와 완료까지 걸리는 시간을 함께 보기!",
        "목적지 음성 검색" to "장소 이름만 또박또박! 운전 중엔 손 대신 음성으로!",
        "경유지 추가" to "목적지는 그대로 두고, 가는 길에 들를 곳만 끼워 넣어요!",
        "주유소·충전소 검색" to "'가까운 충전소 찾아줘'처럼 말하면 돼요. 잔량 20% 전에 미리 찾기!",
    )
    /** 카테고리 맨 위 일상 퀘스트의 시나리오 흐름. query는 내비 앱 검색어. */
    class Flow(val ask: String, val find: String, val query: String)
    val flows = mapOf(
        0 to Flow("근처 주유소로 안내할까요?", "주유소", "주유소"),
        1 to Flow("근처 패스트푸드점으로 안내할까요?", "패스트푸드점", "맥도날드"),
        2 to Flow("근처 대형마트로 안내할까요?", "대형마트", "이마트"),
        3 to Flow("가까운 하이패스 톨게이트로 안내할까요?", "톨게이트", "톨게이트"),
        4 to Flow("근처 초등학교(보호구역)로 안내할까요?", "초등학교", "초등학교"),
        5 to Flow("근처 기차역으로 안내할까요?", "기차역", "기차역"),
        6 to Flow("한강 다리로 안내할까요?", "한강 다리", "한강대교"),
        7 to Flow("야간 드라이브하기 좋은 근처 공원으로 안내할까요?", "공원", "공원"),
        8 to Flow("다른 도시의 시청으로 안내할까요?", "시청", "시청"),
    )
    fun names(type: String, c: Int): List<String> = if (type == "vehicle") vehicleTutorials[c].map { it.name } else dailyQuests[c].map { it.name }

    fun quest(type: String, c: Int, i: Int): Quest {
        val isV = type == "vehicle"
        if (isV) {
            val t = vehicleTutorials[c][i]
            return Quest("$type-$c-$i", t.name, c, t.steps ?: listOf(t.guide), t.guide, t.done, t.avail, tips[t.name])
        }
        val d = dailyQuests[c][i]
        val steps = listOf("안내 내용 살펴보기", "필요한 동작 확인하기", "확인 내용 돌아보기")
        val desc = "난이도 ${d.stars} · " + (if (d.manual) "직접 완료 누르기" else "자동 확인")
        return Quest("$type-$c-$i", d.name, c, steps, desc)
    }
}

/** 뱃지 조건에서 빼는 퀘스트: 자동 판정을 보류 중이라 완료할 방법이 없는 퀘스트(예: 크루즈 켰다가 끄기). */
fun badgeExempt(q: Quest): Boolean = q.id.startsWith("vehicle-") && QuestSignals.find(q.name)?.pendingReason != null
/** 전기차 모드: 켜져 있을 때만 차량 퀘스트에 "전기차" 카테고리(맨 마지막)가 보인다. */
object EvMode {
    private fun prefs(c: Context) = c.getSharedPreferences("yeonsu_quest", Context.MODE_PRIVATE)
    fun isOn(c: Context) = prefs(c).getBoolean("evMode", false)
    fun set(c: Context, on: Boolean) { prefs(c).edit().putBoolean("evMode", on).apply() }
    /** 지금 보여줄 카테고리 이름들. */
    fun cats(c: Context, type: String): List<String> {
        val all = QuestData.cats.getValue(type)
        return if (type == "vehicle" && !isOn(c)) all.dropLast(1) else all
    }
}
/** 일상 퀘스트 시나리오 흐름의 현재 단계. */
object QuestFlow {
    const val INTRO = 0   // 처음 시나리오 보는 중
    const val ASK = 1     // 안내할까요?
    const val WAIT = 2    // 내비에서 장소 고르는 중
    const val ARRIVED = 3 // 도착. 시나리오 한 번 더?
    const val REPLAY = 4  // 시나리오 다시 보는 중
    var stage = INTRO
    var destName: String? = null
    fun start() { stage = INTRO; destName = null }
}
/** 화면 사이에서 "지금 어떤 종류/카테고리/퀘스트를 보고 있는지" 유지한다(HTML의 type, category, selected). */
object QuestSession {
    var type = "vehicle"
    var category = 0
    var questIndex = 0
    /** 왼쪽 카테고리 목록의 스크롤 위치(종류별). 카테고리를 눌러도, 화면을 다시 그려도 그 자리에 있게 한다. */
    val catScroll = HashMap<String, Int>()
    fun selected(): Quest = QuestData.quest(type, category, questIndex)
}

/**
 * 퀘스트 진행·완료 기록 저장(SharedPreferences "yeonsu_quest").
 * 지금은 화면 확인용 미리보기 기록이다. 차량 SDK 관찰값으로 판정한 기록이 아니다.
 */
class QuestStore(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("yeonsu_quest", Context.MODE_PRIVATE))
    init {
        // 저장 데이터를 지금 목록에 맞게 한 번만 옮긴다(삭제 없음). 옮긴 뒤 바뀐 뱃지 조건으로 이미 채운 카테고리 뱃지를 준다.
        if (QuestMigration.migrate(prefs) != null) grantEarnedBadges()
    }
    /**
     * status: 저장된 상태(new/doing/done). 완료한 퀘스트를 다시 해도 done 으로 남는다.
     * running: 지금 수행 중인지(처음 수행 doing, 또는 완료한 퀘스트의 재수행). step 은 수행 중인 단계.
     */
    class State(val status: String, val step: Int, val running: Boolean = status == "doing")
    /** verified=true: 차량 신호로 확인된 완료. false(또는 이전 기록): 버튼으로 완료한 기록. */
    class Record(val id: String, val name: String, val at: Long, val verified: Boolean = false)

    // 키 규칙: p:<id> = 저장된 진행/완료 상태, r:<id> = 완료한 퀘스트를 다시 하는 중인 단계(임시), records = 완료 기록.
    // 재수행은 p:<id>(done)를 건드리지 않고 r:<id> 로만 표시하므로, 도중에 중단해도 완료 상태·기록·뱃지가 그대로 남는다.
    fun state(q: Quest): State {
        val raw = prefs.getString("p:" + q.id, null) ?: return State("new", 0)
        val p = raw.split("|")
        val status = p.getOrNull(0) ?: "new"
        val rerun = prefs.getString("r:" + q.id, null)
        if (status == "done" && rerun != null) return State("done", rerun.toIntOrNull() ?: 0, running = true)
        return State(status, p.getOrNull(1)?.toIntOrNull() ?: 0)
    }
    fun start(q: Quest) {
        val s = state(q)
        if (s.running) return
        if (s.status == "done") prefs.edit().putString("r:" + q.id, "0").apply()   // 재수행: 완료 상태는 그대로
        else prefs.edit().putString("p:" + q.id, "doing|0").apply()
    }
    /** 지금 수행만 취소한다. 처음 수행이면 시작 전으로, 재수행이면 재수행 표시만 지운다(완료·기록·뱃지 유지). */
    fun stop(q: Quest) {
        val e = prefs.edit().remove("r:" + q.id)
        if (state(q).status == "doing") e.remove("p:" + q.id)
        e.apply()
    }

    /** 단계 진행과 상관없이 완료 처리하고 기록을 남긴다. */
    fun complete(q: Quest, verified: Boolean = false) {
        prefs.edit().putString("p:" + q.id, "done|" + q.steps.size).remove("r:" + q.id).apply()
        val arr = JSONArray(prefs.getString("records", "[]"))
        arr.put(JSONObject().put("id", q.id).put("name", q.name).put("at", System.currentTimeMillis()).put("verified", verified))
        prefs.edit().putString("records", arr.toString()).apply()
        checkBadge(q)
    }
    /** 다음 단계로 넘긴다. 마지막 단계면 완료 기록을 남기고 true. */
    fun advance(q: Quest, verified: Boolean = false): Boolean {
        val s = state(q)
        if (!s.running) return false   // 수행 중이 아니면(중단·완료 후 늦은 응답 등) 아무것도 남기지 않는다
        val next = s.step + 1
        if (next >= q.steps.size) {
            prefs.edit().putString("p:" + q.id, "done|$next").remove("r:" + q.id).apply()
            val arr = JSONArray(prefs.getString("records", "[]"))
            arr.put(JSONObject().put("id", q.id).put("name", q.name).put("at", System.currentTimeMillis()).put("verified", verified))
            prefs.edit().putString("records", arr.toString()).apply()
            checkBadge(q)
            return true
        }
        if (s.status == "done") prefs.edit().putString("r:" + q.id, next.toString()).apply()
        else prefs.edit().putString("p:" + q.id, "doing|$next").apply()
        return false
    }

    // ───── 뱃지: 한 카테고리의 퀘스트를 모두 완료하면 받는다 ─────
    private fun typeOf(q: Quest) = q.id.substringBefore("-")
    /** 카테고리의 뱃지 조건 퀘스트(자동 판정 보류 중인 퀘스트는 빼고)를 모두 완료했는지. */
    fun categoryDone(type: String, c: Int): Boolean {
        val required = QuestData.names(type, c).indices.filterNot { badgeExempt(QuestData.quest(type, c, it)) }
        return required.isNotEmpty() && required.all { state(QuestData.quest(type, c, it)).status == "done" }
    }
    /** 이미 조건을 채운 카테고리인데 뱃지가 없으면 준다(뱃지 조건이 바뀐 뒤 한 번). 팝업은 띄우지 않는다. */
    private fun grantEarnedBadges() {
        val e = prefs.edit(); var any = false
        QuestData.cats.forEach { (type, cats) -> cats.indices.forEach { c ->
            if (!hasBadge(type, c) && categoryDone(type, c)) { e.putLong("badge:$type:$c", System.currentTimeMillis()); any = true }
        } }
        if (any) e.apply()
    }
    fun hasBadge(type: String, c: Int): Boolean = prefs.contains("badge:$type:$c")
    /** 방금 완료한 퀘스트 때문에 카테고리가 모두 깨졌으면 뱃지를 주고 "새 뱃지"로 표시해 둔다. */
    private fun checkBadge(q: Quest) {
        val type = typeOf(q)
        if (hasBadge(type, q.c) || !categoryDone(type, q.c)) return
        prefs.edit().putLong("badge:$type:${q.c}", System.currentTimeMillis()).putString("pendingBadge", "$type|${q.c}").apply()
    }
    /** 아직 보여주지 않은 새 뱃지(종류, 카테고리 번호). 한 번 꺼내면 지워진다. */
    fun takeNewBadge(): Pair<String, Int>? {
        val raw = prefs.getString("pendingBadge", null) ?: return null
        prefs.edit().remove("pendingBadge").apply()
        val p = raw.split("|")
        return Pair(p[0], p[1].toIntOrNull() ?: return null)
    }
    fun count(status: String): Int = prefs.all.entries.count { it.key.startsWith("p:") && (it.value as? String)?.startsWith("$status|") == true }

    /** 최근 순. */
    fun records(): List<Record> {
        val arr = JSONArray(prefs.getString("records", "[]"))
        return (0 until arr.length()).map { val o = arr.getJSONObject(it); Record(o.getString("id"), o.getString("name"), o.getLong("at"), o.optBoolean("verified", false)) }.reversed()
    }
}
