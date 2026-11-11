package com.example.test.yeonsu
/**
 * 일상 퀘스트 도착 판정 (SDK 비의존, JVM 테스트 가능).
 * 내비가 알려주는 도착 정보(DestinationArrivedInfo)에는 장소 이름·주소만 있고 장소 종류(주유소·마트 등)는 없다.
 * 그래서 "경로를 시작할 때 고른 목적지"와 "도착한 곳"이 같은지, 그 이름이 퀘스트 장소 종류의 낱말을 담고 있는지만 비교할 수 있다.
 */
object DailyArrival {
    enum class Result {
        /** 고른 목적지에 도착했고 이름으로 장소 종류가 맞음 → 내비 도착 확인으로 완료 */
        MATCHED,
        /** 고른 목적지에 도착했지만 이름만으로는 장소 종류를 확정할 수 없음 → 사용자 확인 */
        UNCONFIRMED,
        /** 고른 목적지와 다른 곳에 도착 → 완료하지 않는다 */
        MISMATCH,
        /** 도착 정보에 이름이 없음 → 사용자 확인 */
        UNKNOWN,
    }
    private fun norm(s: String?) = s.orEmpty().replace(" ", "").lowercase()
    /**
     * @param keywords 퀘스트 장소 종류를 뜻하는 낱말(예: "주유소", "맥도날드")
     * @param routeDest 경로를 시작할 때(또는 내비에 설정된) 목적지 이름. 모르면 null
     * @param arrivedName 내비 도착 이벤트의 장소 이름
     */
    fun judge(keywords: List<String>, routeDest: String?, arrivedName: String?): Result {
        val a = norm(arrivedName)
        if (a.isEmpty()) return Result.UNKNOWN
        val r = norm(routeDest)
        if (r.isNotEmpty() && r != a) return Result.MISMATCH
        val kind = keywords.map(::norm).filter { it.isNotEmpty() }
        return if (r.isNotEmpty() && kind.any { a.contains(it) }) Result.MATCHED else Result.UNCONFIRMED
    }
}
