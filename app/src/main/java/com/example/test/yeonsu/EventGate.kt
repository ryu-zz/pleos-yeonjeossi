package com.example.test.yeonsu
/**
 * 음성·내비 이벤트 한 번의 수신 구간. SDK 비의존(JVM 테스트 가능).
 * open 할 때마다 새 번호를 받고, close 하거나 다시 open 하면 이전 번호의 인식 결과·콜백은 모두 버린다.
 */
class EventGate {
    private var token = 0
    var active = false
        private set
    fun open(): Int { token++; active = true; return token }
    fun close() { token++; active = false }
    fun accept(t: Int) = active && t == token
}
/** 음성 인식 오류를 화면에 보여줄 안내로 바꾼다. SDK 오류 이름(ErrorType.name)만 받는다. */
object EventErrors {
    class Described(val message: String, /** true 면 조용히 다시 듣는다(말이 없어 끝난 경우 등) */ val silentRetry: Boolean)
    fun describe(errorName: String): Described = when (errorName) {
        "TIMEOUT" -> Described("", true)
        "PERMISSION_DENIED", "UNAUTHENTICATED" ->
            Described("마이크 또는 음성 인식 권한이 없어요. 권한을 허용한 뒤 '다시 시도'를 눌러 주세요.", false)
        "MIC_ERROR", "AUDIO_ERROR" ->
            Described("마이크를 사용할 수 없어요. 다른 앱이 마이크를 쓰고 있지 않은지 확인하고 '다시 시도'를 눌러 주세요.", false)
        "NETWORK_ERROR", "SERVER_INTERNAL_ERROR" ->
            Described("음성 인식 서비스에 연결하지 못했어요. 잠시 뒤 '다시 시도'를 눌러 주세요.", false)
        "NOT_SUPPORTED_API", "UNAVAILABLE_LANGUAGE", "ON_DEVICE_ENGINE_ERROR" ->
            Described("이 기기에서는 음성 인식을 사용할 수 없어요. 퀘스트를 중단하고 다른 퀘스트를 해 주세요.", false)
        else -> Described("음성 인식에 문제가 생겼어요($errorName). '다시 시도'를 눌러 주세요.", false)
    }
    /** 조용히 다시 듣는 횟수 상한(연속 오류가 이어지면 안내를 보여준다). */
    const val MAX_SILENT_RETRY = 3
}
