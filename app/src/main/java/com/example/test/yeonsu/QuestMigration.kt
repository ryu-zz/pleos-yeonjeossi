package com.example.test.yeonsu

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 저장된 진행 상태·기록·뱃지를 지금 차량 퀘스트 목록에 맞게 옮긴다(한 번만, 삭제 없음).
 *
 * 퀘스트 ID 는 목록 위치(vehicle-카테고리-순번)라서, main 에서 목록을 바꿀 때마다 같은 ID 가 다른 퀘스트를 가리키게 됐다.
 * 예: 2e9d92f 에서 "내비·음성"이 "전기차" 앞에 들어가 충전구가 vehicle-7-0 → vehicle-8-0 으로 옮겨졌고,
 *     예전 vehicle-7-0(충전구) 진행 상태가 새 vehicle-7-0(목적지 음성 검색)에 붙어 보였다.
 *
 * 판단 근거(추측하지 않는다):
 * - 진행 상태 p:<id>(와 재수행 표시 r:<id>): 그 ID 로 남은 가장 최근 기록의 퀘스트 이름을 지금 목록에서 찾아 그 ID 로 옮긴다.
 *   기록이 없으면, 과거 모든 목록에서 그 ID 가 지금과 같은 퀘스트였을 때만 그대로 둔다.
 *   어느 퀘스트였는지 알 수 없거나 지금 목록에 없는 퀘스트면 "legacy:" 키로 옮겨 보관한다(지우지 않는다).
 * - 수행 기록: 이름이 지금 목록에 있으면 ID 만 지금 ID 로 고치고, 예전 ID 는 legacyId 로 남긴다.
 *   지금 목록에 없는 퀘스트의 기록은 그대로 둔다(화면에는 저장된 이름으로 보인다).
 * - 뱃지 badge:vehicle:<카테고리 번호>: 뱃지를 받은 시각과 같은 때 남은 기록으로 그 때의 목록·카테고리 이름을 찾아 지금 번호로 옮긴다.
 *   근거가 없으면, 과거 모든 목록에서 그 번호가 지금과 같은 카테고리였을 때만 그대로 둔다. 아니면 legacy: 로 보관한다.
 * - 일상 퀘스트 ID 는 처음 생긴 뒤 바뀐 적이 없어 손대지 않는다.
 */
object QuestMigration {
    /** 저장 데이터 구조 번호. 이 번호가 KEY 에 저장돼 있으면 다시 옮기지 않는다. */
    const val SCHEMA = 2
    const val KEY = "schema"
    const val LEGACY = "legacy:"

    class Layout(val rev: String, val cats: List<String>, val quests: List<List<String>>) {
        fun nameOf(id: String): String? = parse(id)?.let { (c, i) -> quests.getOrNull(c)?.getOrNull(i) }
        fun catOf(c: Int): String? = cats.getOrNull(c)
    }

    /** main 에 있었던 차량 퀘스트 목록(카테고리, 퀘스트 이름 순서). git 기록에서 뽑았다. 지금 목록은 QuestData 에서 읽는다. */
    val history: List<Layout> = listOf(
        // 0eb8031 (2026-11-04)
        Layout("0eb8031", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문·문·트렁크", "공조·시야", "주행 기능", "전기차 기능", "내비·음성", "비상상황"), listOf(
            listOf("운전석 앞뒤 조절", "좌석 높이", "등받이", "안전벨트", "문 확인", "브레이크 위치", "액셀 위치"),
            listOf("왼쪽 방향지시등", "오른쪽 방향지시등", "방향지시등 끄기", "차선 변경 깜빡이"),
            listOf("와이퍼 한 번", "자동 와이퍼", "저속 와이퍼", "고속 와이퍼", "와이퍼 끄기"),
            listOf("자동 전조등", "하향등", "상향등", "상향등 끄기"),
            listOf("운전석 창문", "창문 닫기", "뒷좌석 창문 잠금", "전체 문 잠금", "트렁크"),
            listOf("에어컨", "온도 조절", "풍량 조절", "송풍 방향", "내기순환", "외기 모드", "앞유리 성에 제거", "사이드미러 열선", "시트 열선·통풍"),
            listOf("운전대 방향", "주행 모드", "크루즈 버튼", "전자식 주차 브레이크"),
            listOf("충전구 열기", "목표 충전량", "충전 상태 확인"),
            listOf("목적지 음성 검색", "경유지 추가", "주유소·충전소 검색"),
            listOf("타이어 경고등 연습", "엔진오일 경고 연습", "브레이크 경고 연습", "접촉사고 연습", "고속도로 고장 연습", "에어백 전개 연습"),
        )),
        // df92039 (2026-11-04)
        Layout("df92039", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "전기차", "내비", "비상상황"), listOf(
            listOf("운전석 앞뒤 조절", "좌석 높이", "등받이", "안전벨트", "문 확인", "브레이크 위치", "액셀 위치"),
            listOf("왼쪽 방향지시등", "오른쪽 방향지시등", "방향지시등 끄기", "차선 변경 깜빡이"),
            listOf("와이퍼 한 번", "자동 와이퍼", "저속 와이퍼", "고속 와이퍼", "와이퍼 끄기"),
            listOf("자동 전조등", "하향등", "상향등", "상향등 끄기"),
            listOf("운전석 창문", "창문 닫기", "뒷좌석 창문 잠금", "전체 문 잠금", "트렁크"),
            listOf("에어컨", "온도 조절", "풍량 조절", "송풍 방향", "내기순환", "외기 모드", "앞유리 성에 제거", "사이드미러 열선", "시트 열선·통풍"),
            listOf("운전대 방향", "주행 모드", "크루즈 버튼", "전자식 주차 브레이크"),
            listOf("충전구 열기", "목표 충전량", "충전 상태 확인"),
            listOf("목적지 음성 검색", "경유지 추가", "주유소·충전소 검색"),
            listOf("타이어 경고등 연습", "엔진오일 경고 연습", "브레이크 경고 연습", "접촉사고 연습", "고속도로 고장 연습", "에어백 전개 연습"),
        )),
        // 21ec8ca (2026-11-09)
        Layout("21ec8ca", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "전기차", "내비", "비상상황"), listOf(
            listOf("운전석 앞뒤 조절", "좌석 높이", "등받이", "안전벨트", "운전석 문 열었다가 닫기", "브레이크 밟았다가 떼기", "액셀 위치"),
            listOf("왼쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등 켰다가 끄기", "차선 변경 깜빡이"),
            listOf("와이퍼 한 번", "자동 와이퍼 켰다가 끄기", "저속 와이퍼 켰다가 끄기", "고속 와이퍼 켰다가 끄기"),
            listOf("자동 전조등", "하향등 켰다가 끄기", "상향등 켰다가 끄기"),
            listOf("운전석 창문 열었다가 닫기", "뒷좌석 창문 잠금", "전체 문 풀었다가 잠그기", "트렁크 열었다가 닫기"),
            listOf("에어컨 켰다가 끄기", "온도 조절", "풍량 조절", "송풍 방향", "내기순환 켰다가 외기로", "앞유리 성에 제거 켰다가 끄기", "사이드미러 열선", "시트 통풍 켰다가 끄기"),
            listOf("운전대 왼쪽으로 돌렸다가 풀기", "주행 모드", "크루즈 버튼", "전자식 주차 브레이크"),
            listOf("충전구 열었다가 닫기", "목표 충전량", "충전 상태 확인"),
            listOf("목적지 음성 검색", "경유지 추가", "주유소·충전소 검색"),
            listOf("타이어 경고등 연습", "엔진오일 경고 연습", "브레이크 경고 연습", "접촉사고 연습", "고속도로 고장 연습", "에어백 전개 연습"),
        )),
        // 9443cd7 (2026-11-09)
        Layout("9443cd7", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "전기차", "내비", "비상상황"), listOf(
            listOf("운전석 앞뒤 조절", "좌석 높이", "등받이", "안전벨트", "운전석 문 열었다가 닫기", "브레이크 밟았다가 떼기", "액셀 위치"),
            listOf("왼쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등 켰다가 끄기", "차선 변경 깜빡이"),
            listOf("와이퍼 한 번", "자동 와이퍼 켰다가 끄기", "저속 와이퍼 켰다가 끄기", "고속 와이퍼 켰다가 끄기"),
            listOf("자동 전조등", "하향등 켰다가 끄기", "상향등 켰다가 끄기"),
            listOf("운전석 창문 열었다가 닫기", "뒷좌석 창문 잠금", "전체 문 풀었다가 잠그기", "트렁크 열었다가 닫기"),
            listOf("에어컨 켰다가 끄기", "온도 조절", "풍량 조절", "송풍 방향", "내기순환 켰다가 외기로", "앞유리 성에 제거 켰다가 끄기", "사이드미러 열선", "시트 통풍 켰다가 끄기"),
            listOf("운전대 왼쪽으로 돌렸다가 풀기", "주행 모드", "크루즈 켰다가 끄기", "전자식 주차 브레이크"),
            listOf("충전구 열었다가 닫기", "목표 충전량", "충전 상태 확인"),
            listOf("목적지 음성 검색", "경유지 추가", "주유소·충전소 검색"),
            listOf("타이어 경고등 연습", "엔진오일 경고 연습", "브레이크 경고 연습", "접촉사고 연습", "고속도로 고장 연습", "에어백 전개 연습"),
        )),
        // 43b12bb (2026-11-09)
        Layout("43b12bb", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "전기차"), listOf(
            listOf("운전석 문 열었다가 닫기", "브레이크 밟았다가 떼기"),
            listOf("왼쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등 켰다가 끄기"),
            listOf("와이퍼 한 번", "자동 와이퍼 켰다가 끄기", "저속 와이퍼 켰다가 끄기", "고속 와이퍼 켰다가 끄기"),
            listOf("하향등 켰다가 끄기", "상향등 켰다가 끄기"),
            listOf("운전석 창문 열었다가 닫기", "전체 문 풀었다가 잠그기", "트렁크 열었다가 닫기"),
            listOf("에어컨 켰다가 끄기", "온도 조절", "송풍 방향", "내기순환 켰다가 외기로", "앞유리 성에 제거 켰다가 끄기", "시트 통풍 켰다가 끄기"),
            listOf("운전대 왼쪽으로 돌렸다가 풀기", "크루즈 켰다가 끄기"),
            listOf("충전구 열었다가 닫기"),
        )),
        // d092a57 (2026-11-09)
        Layout("d092a57", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "전기차"), listOf(
            listOf("운전석 문 열었다가 닫기", "브레이크 밟았다가 떼기"),
            listOf("왼쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등 켰다가 끄기"),
            listOf("와이퍼 한 번", "자동 와이퍼 켰다가 끄기", "저속 와이퍼 켰다가 끄기", "고속 와이퍼 켰다가 끄기"),
            listOf("하향등 켰다가 끄기", "상향등 켰다가 끄기"),
            listOf("운전석 창문 열었다가 닫기", "전체 문 풀었다가 잠그기", "트렁크 열었다가 닫기", "프렁크 열었다가 닫기"),
            listOf("에어컨 켰다가 끄기", "온도 조절", "송풍 방향", "내기순환 켰다가 외기로", "앞유리 성에 제거 켰다가 끄기", "시트 통풍 켰다가 끄기"),
            listOf("운전대 왼쪽으로 돌렸다가 풀기", "크루즈 켰다가 끄기"),
            listOf("충전구 열었다가 닫기"),
        )),
        // f486ab2 (2026-11-09)
        Layout("f486ab2", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "전기차"), listOf(
            listOf("운전석 문 열었다가 닫기", "브레이크 밟았다가 떼기"),
            listOf("왼쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등 켰다가 끄기"),
            listOf("와이퍼 한 번", "자동 와이퍼 켰다가 끄기", "저속 와이퍼 켰다가 끄기", "고속 와이퍼 켰다가 끄기"),
            listOf("하향등 켰다가 끄기", "상향등 켰다가 끄기"),
            listOf("운전석 창문 열었다가 닫기", "전체 문 풀었다가 잠그기", "트렁크 열었다가 닫기", "프렁크 열었다가 닫기"),
            listOf("에어컨 켰다가 끄기", "온도 조절", "송풍 방향", "내기순환 켰다가 외기로", "앞유리 성에 제거 켰다가 끄기", "뒷유리 열선 켰다가 끄기", "시트 통풍 켰다가 끄기"),
            listOf("운전대 왼쪽으로 돌렸다가 풀기", "크루즈 켰다가 끄기"),
            listOf("충전구 열었다가 닫기"),
        )),
        // cdffffb (2026-11-10)
        Layout("cdffffb", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "전기차"), listOf(
            listOf("운전석 문 열었다가 닫기", "브레이크 밟았다가 떼기"),
            listOf("왼쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등 켰다가 끄기"),
            listOf("와이퍼 한 번", "자동 와이퍼 켰다가 끄기", "저속 와이퍼 켰다가 끄기", "고속 와이퍼 켰다가 끄기", "리어 와이퍼 켰다가 끄기"),
            listOf("하향등 켰다가 끄기", "상향등 켰다가 끄기"),
            listOf("운전석 창문 열었다가 닫기", "전체 문 풀었다가 잠그기", "트렁크 열었다가 닫기", "프렁크 열었다가 닫기"),
            listOf("에어컨 켰다가 끄기", "온도 조절", "송풍 방향", "내기순환 켰다가 외기로", "앞유리 성에 제거 켰다가 끄기", "뒷유리 열선 켰다가 끄기", "시트 통풍 켰다가 끄기"),
            listOf("운전대 왼쪽으로 돌렸다가 풀기", "크루즈 켰다가 끄기"),
            listOf("충전구 열었다가 닫기"),
        )),
        // 2e9d92f (2026-11-10)
        Layout("2e9d92f", listOf("출발 준비", "방향지시등", "와이퍼", "전조등", "창문 기능", "공조 시야", "주행 기능", "내비·음성", "전기차"), listOf(
            listOf("운전석 문 열었다가 닫기", "브레이크 밟았다가 떼기"),
            listOf("왼쪽 방향지시등 켰다가 끄기", "오른쪽 방향지시등 켰다가 끄기"),
            listOf("와이퍼 한 번", "자동 와이퍼 켰다가 끄기", "저속 와이퍼 켰다가 끄기", "고속 와이퍼 켰다가 끄기", "리어 와이퍼 켰다가 끄기"),
            listOf("하향등 켰다가 끄기", "상향등 켰다가 끄기"),
            listOf("운전석 창문 열었다가 닫기", "전체 문 풀었다가 잠그기", "트렁크 열었다가 닫기", "프렁크 열었다가 닫기"),
            listOf("에어컨 켰다가 끄기", "온도 조절", "송풍 방향", "내기순환 켰다가 외기로", "앞유리 성에 제거 켰다가 끄기", "뒷유리 열선 켰다가 끄기", "시트 통풍 켰다가 끄기"),
            listOf("운전대 왼쪽으로 돌렸다가 풀기", "크루즈 켰다가 끄기"),
            listOf("목적지 음성 검색", "경유지 추가", "주유소·충전소 검색"),
            listOf("충전구 열었다가 닫기", "충전 상태 확인"),
        )),
    )

    /** 같은 퀘스트인데 이름만 바뀐 것(판정 의미가 같은 것만). */
    val nameAlias = mapOf("와이퍼 한 번" to "와이퍼 켰다가 끄기")
    /** 같은 카테고리인데 이름만 바뀐 것. */
    val catAlias = mapOf("창문·문·트렁크" to "창문 기능", "공조·시야" to "공조 시야", "전기차 기능" to "전기차")

    private fun parse(id: String): Pair<Int, Int>? {
        val p = id.split("-")
        if (p.size != 3 || p[0] != "vehicle") return null
        val c = p[1].toIntOrNull() ?: return null
        val i = p[2].toIntOrNull() ?: return null
        return c to i
    }
    private fun alias(name: String) = nameAlias[name] ?: name
    private fun catName(name: String) = catAlias[name] ?: name
    private val current: Layout by lazy {
        val cats = QuestData.cats.getValue("vehicle")
        Layout("current", cats, cats.indices.map { QuestData.names("vehicle", it) })
    }
    /** 지금 목록에서 이 이름의 퀘스트 ID. */
    fun currentIdOf(name: String): String? {
        val n = alias(name)
        current.quests.forEachIndexed { c, l -> l.forEachIndexed { i, q -> if (q == n) return "vehicle-$c-$i" } }
        return null
    }
    /** 과거 모든 목록에서 이 ID 가 지금과 같은 퀘스트였는지(기록이 없을 때 그대로 둬도 되는지). */
    private fun idStable(id: String): Boolean {
        val now = current.nameOf(id) ?: return false
        return history.all { l -> l.nameOf(id)?.let(::alias).let { it == null || it == now } }
    }
    private fun catStable(c: Int): Boolean {
        val now = current.catOf(c) ?: return false
        return history.all { l -> l.catOf(c)?.let(::catName).let { it == null || it == now } }
    }

    class Report(
        val moved: Map<String, String>, val kept: List<String>, val legacy: List<String>,
        val recordsRelinked: Int, val badgesMoved: Map<String, String>, val badgesLegacy: List<String>,
    )

    /** 한 번만 옮긴다. 이미 옮겼으면(또는 저장된 게 없는 새 설치면) null. */
    fun migrate(prefs: SharedPreferences): Report? {
        if (prefs.getInt(KEY, 0) >= SCHEMA) return null
        val all = prefs.all
        val records = JSONArray(prefs.getString("records", "[]"))
        // 1) ID 별 가장 최근 기록(기록은 뒤에 붙으므로 마지막 것이 가장 최근)
        val lastName = HashMap<String, String>()
        for (k in 0 until records.length()) {
            val o = records.getJSONObject(k)
            lastName[o.optString("id")] = o.optString("name")
        }
        // 2) 진행 상태 p:vehicle-*
        val moved = LinkedHashMap<String, String>(); val kept = ArrayList<String>(); val legacy = ArrayList<String>()
        val target = LinkedHashMap<String, String?>()   // 옛 ID → 새 ID(null 이면 보관)
        all.keys.filter { it.startsWith("p:vehicle-") }.sorted().forEach { key ->
            val id = key.removePrefix("p:")
            val name = lastName[id]
            target[id] = when {
                name != null -> currentIdOf(name)
                idStable(id) -> id
                else -> null
            }
        }
        // 같은 새 ID 로 둘 이상이 오면 완료 상태를 우선하고 나머지는 보관
        val byNew = target.entries.filter { it.value != null }.groupBy { it.value!! }
        val winner = HashMap<String, String>()
        byNew.forEach { (nid, list) ->
            winner[nid] = list.maxByOrNull { e -> rank(all["p:" + e.key] as? String) + (if (e.key == nid) 0.5 else 0.0) }!!.key
        }
        val e = prefs.edit()
        target.forEach { (old, nid) ->
            if (nid != null && winner[nid] == old) {
                if (nid == old) { kept += old; return@forEach }
                moved[old] = nid
            } else legacy += old
        }
        // 옮길 키를 먼저 모두 지운 뒤 다시 쓴다(7-0 ↔ 8-0 처럼 서로 자리를 바꾸는 경우를 위해)
        (moved.keys + legacy).forEach { old -> e.remove("p:$old"); e.remove("r:$old") }
        moved.forEach { (old, nid) ->
            (all["p:$old"] as? String)?.let { e.putString("p:$nid", it) }
            (all["r:$old"] as? String)?.let { e.putString("r:$nid", it) }
        }
        legacy.forEach { old ->
            (all["p:$old"] as? String)?.let { e.putString(LEGACY + "p:$old", it) }
            (all["r:$old"] as? String)?.let { e.putString(LEGACY + "r:$old", it) }
        }
        // 3) 수행 기록 ID 다시 잇기(이름이 지금 목록에 있을 때만)
        var relinked = 0
        val newRecords = JSONArray()
        for (k in 0 until records.length()) {
            val o = JSONObject(records.getJSONObject(k).toString())
            val id = o.optString("id")
            if (id.startsWith("vehicle-")) {
                val nid = currentIdOf(o.optString("name"))
                if (nid != null && nid != id) { o.put("legacyId", id); o.put("id", nid); relinked++ }
            }
            newRecords.put(o)
        }
        if (relinked > 0) e.putString("records", newRecords.toString())
        // 4) 뱃지
        val badgesMoved = LinkedHashMap<String, String>(); val badgesLegacy = ArrayList<String>()
        val badgeKeys = all.keys.filter { it.startsWith("badge:vehicle:") }
        val badgeTarget = LinkedHashMap<String, String?>()
        badgeKeys.forEach { key ->
            val c = key.removePrefix("badge:vehicle:").toIntOrNull()
            val at = (all[key] as? Number)?.toLong()
            val cat = if (c == null) null else badgeCategory(c, at, records)
            val nc = when {
                cat != null -> current.cats.indexOf(cat).takeIf { it >= 0 }
                c != null && catStable(c) -> c
                else -> null
            }
            badgeTarget[key] = nc?.let { "badge:vehicle:$it" }
        }
        val occupied = badgeTarget.filter { (k, v) -> v == k }.keys.toMutableSet()   // 그대로 두는 뱃지 자리
        val badgeMovers = badgeTarget.filter { (k, v) -> v != k }
        badgeMovers.keys.forEach { e.remove(it) }
        badgeMovers.forEach { (k, v) ->
            val value = (all[k] as? Number)?.toLong() ?: 0L
            if (v != null && occupied.add(v)) { e.putLong(v, value); badgesMoved[k] = v }
            else { e.putLong(LEGACY + k, value); badgesLegacy += k }
        }
        e.putString("migration:$SCHEMA", JSONObject()
            .put("moved", JSONObject(moved as Map<*, *>)).put("legacy", JSONArray(legacy))
            .put("recordsRelinked", relinked).put("badgesMoved", JSONObject(badgesMoved as Map<*, *>))
            .put("badgesLegacy", JSONArray(badgesLegacy)).toString())
        e.putInt(KEY, SCHEMA)
        e.apply()
        return Report(moved, kept, legacy, relinked, badgesMoved, badgesLegacy)
    }

    private fun rank(v: String?) = when (v?.substringBefore("|")) { "done" -> 3.0; "doing" -> 2.0; null -> 0.0; else -> 1.0 }

    /** 뱃지를 받은 시각(at)과 가장 가까운(10초 안) 같은 카테고리 번호의 기록으로, 그 때의 카테고리 이름을 찾는다. */
    private fun badgeCategory(c: Int, at: Long?, records: JSONArray): String? {
        if (at == null) return null
        var best: JSONObject? = null; var bestGap = Long.MAX_VALUE
        for (k in 0 until records.length()) {
            val o = records.getJSONObject(k)
            val p = parse(o.optString("id")) ?: continue
            if (p.first != c) continue
            val gap = kotlin.math.abs(o.optLong("at") - at)
            if (gap <= 10_000 && gap < bestGap) { best = o; bestGap = gap }
        }
        val o = best ?: return null
        val id = o.optString("id"); val name = o.optString("name")
        // 그 기록을 남긴 목록(ID 와 이름이 맞는 목록)에서 카테고리 이름을 읽는다
        val names = (history + current).filter { it.nameOf(id) == name }.mapNotNull { it.catOf(c)?.let(::catName) }.distinct()
        return names.singleOrNull()
    }
}
