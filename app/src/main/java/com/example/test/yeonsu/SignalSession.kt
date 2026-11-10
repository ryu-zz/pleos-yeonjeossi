package com.example.test.yeonsu

/**
 * 퀘스트 한 번의 "차량 신호 판정 세션" (SDK·화면 비의존, JVM 단위 테스트 가능).
 *
 * - [begin]: 화면이 보이고 퀘스트가 수행 중일 때 연다. 판정기와 이전 값을 새로 만든다(과거 값은 버린다).
 * - [end]: 중단·화면 이탈(onStop)·완료·화면 종료 때 닫는다. 이후 도착하는 응답은 모두 무시한다.
 * - [request]: 차량 값 조회를 보내도 되는지. 앞 조회의 응답이 오기 전에는 새 조회를 보내지 않는다(중첩 방지).
 * - [onResponse]: 응답 처리. 보낼 때 받은 세션 번호와 퀘스트 ID 가 지금 세션과 같을 때만 판정한다.
 */
class SignalSession(private val questId: String, private val make: () -> Detector?) {
    enum class Result { IGNORED, NONE, STAGE_DONE }
    /** 조회 요청 표(세션 번호, 퀘스트 ID). 응답과 함께 되돌려받는다. */
    data class Ticket(val token: Int, val questId: String, val seq: Int)

    private var token = 0
    var active = false
        private set
    var detector: Detector? = null
        private set
    private var last: Any? = null
    private var inFlight = false
    private var inFlightAt = 0L
    private var seq = 0

    fun begin() {
        token++
        detector = make()
        active = detector != null && detector?.pendingReason == null
        last = null
        inFlight = false
    }

    fun end() {
        token++
        active = false
        last = null
        inFlight = false
    }

    /** 조회를 보내도 되면 표를 준다. 수행 중이 아니거나 앞 조회가 아직 진행 중이면 null. */
    fun request(nowMs: Long): Ticket? {
        if (!active) return null
        if (inFlight && nowMs - inFlightAt < STUCK_MS) return null
        inFlight = true; inFlightAt = nowMs
        return Ticket(token, questId, ++seq)
    }

    /**
     * @param stage 지금 퀘스트 진행 단계(store 의 step)
     * @return STAGE_DONE 이면 이 응답으로 한 단계를 마친 것
     */
    fun onResponse(t: Ticket, stage: Int, value: Any?): Result {
        // 다른 세션·다른 퀘스트·이미 닫힌 세션의 응답, 그리고 더 새 조회가 나간 뒤 늦게 온 응답은 버린다
        if (t.token != token || t.questId != questId || !active || t.seq != seq) return Result.IGNORED
        inFlight = false
        if (value == null) return Result.NONE
        val d = detector ?: return Result.IGNORED
        val prev = last
        last = value
        return if (d.done(stage, prev, value)) Result.STAGE_DONE else Result.NONE
    }

    companion object {
        /** 응답이 이 시간(ms) 안에 안 오면 다음 조회를 허용한다(응답이 사라진 경우 멈추지 않게). */
        const val STUCK_MS = 3000L
    }
}
