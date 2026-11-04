package com.example.test.yeonsu

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 팀 HTML 시제품(app.js)의 퀘스트 데이터를 그대로 옮긴 것. 화면 확인용 예시 목록이다. */
class Quest(val id: String, val name: String, val c: Int, val steps: List<String>, val desc: String)

object QuestData {
    val cats = mapOf(
        "vehicle" to listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문·문", "공조·시야", "주행 기능", "내비게이션", "비상상황"),
        "daily" to listOf("차 관리", "드라이브스루", "주차", "고속도로", "보호구역", "생활 이동", "특수 도로", "시간·날씨", "도전"),
    )
    val symbols = listOf("◉", "↔", "☂", "☀", "▱", "♨", "◎", "⌖", "△")
    private val vehicle = listOf(
        listOf("운전석 문 확인", "동승석 문 확인", "출발 전 안전벨트 확인"),
        listOf("왼쪽 방향지시등 켜고 끄기", "오른쪽 방향지시등 켜고 끄기"),
        listOf("빗길 와이퍼 조작 익히기", "워셔액과 와이퍼 알아보기"),
        listOf("전조등 켜고 끄기", "상향등과 하향등 알아보기"),
        listOf("창문 열고 닫기", "트렁크 상태 확인하기"),
        listOf("앞유리 김서림 제거", "실내 온도 조절하기"),
        listOf("주차 기어 확인하기", "주행 모드 알아보기"),
        listOf("목적지 검색하기", "경로 안내 시작하기"),
        listOf("비상등 위치 알아보기", "도움 요청 방법 알아보기"),
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

    fun names(type: String, c: Int): List<String> = (if (type == "vehicle") vehicle else daily)[c]

    fun quest(type: String, c: Int, i: Int): Quest {
        val isV = type == "vehicle"
        val name = names(type, c)[i]
        val steps = when {
            isV && c == 0 && i < 2 -> listOf("문 상태 확인", "문이 닫힌 상태 확인")
            isV && c == 1 -> listOf("방향지시등 꺼짐 확인", (if (i == 0) "왼쪽" else "오른쪽") + " 방향지시등 켜짐 확인", "다시 꺼짐 확인")
            else -> listOf("안내 내용 살펴보기", "필요한 동작 확인하기", "확인 내용 돌아보기")
        }
        val desc = if (isV && c == 0 && i < 2) "출발 전에 문이 제대로 닫혔는지 확인해요." else "한 단계씩 익히고, 내 차에 조금 더 익숙해져요."
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
