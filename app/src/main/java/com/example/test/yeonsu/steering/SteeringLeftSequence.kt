package com.example.test.yeonsu.steering

/**
 * '운전대 방향' 퀘스트(vehicle-6-0, "운전대를 왼쪽으로 조금 돌려보세요")의 순서 판정 로직
 * (SDK 비의존, 순수 Kotlin).
 *
 * 각도 부호 규칙(현재 빌드 SDK 정의를 그대로 따른다):
 * - Pleos Vehicle SDK 2.3.2 소스 KDoc(SteeringWheel.getCurrentSteeringWheelAngle,
 *   registerSteeringWheelAngle, SteeringWheelAngleListener.onSteeringWheelAngleUpdated)에
 *   "Leftward steering angles are negative." 로 명시됨. 즉 '앞바퀴 조향각'이며 왼쪽 = 음수(-).
 * - 따라서 왼쪽으로 돌리면 수신 각도가 기준보다 작아진다(음의 변화량).
 * - 이 값은 운전대 자체의 회전각이 아니라 앞바퀴 조향각이다(SDK 문구 그대로).
 * - 주의: 가상 콘솔의 steering_deg 스칼라/운전대 위젯(CSS rotate)은 같은 값으로 그려지므로
 *   물리 좌/우의 독립 근거가 아니다. 방향 판정은 오직 위 SDK 정의에 근거한다.
 *
 * 조작 연습형(움직임 감지):
 * - 시작 각도를 기준으로 삼고, 거기서 왼쪽으로(= 음의 방향으로) THRESHOLD(도) 이상 돌리면 완료한다.
 * - "조금 돌려보세요"이므로 완만한 임계값(기본 15도)을 쓴다.
 * - 시작 각도 자체는 완료 근거가 아니다(반드시 '시작 이후의 왼쪽 변화'로만 완료).
 * - null(불명)은 완료로 취급하지 않는다(기준/단계 유지).
 * - 완료는 수행 1회당 정확히 한 번만 true 를 돌려준다.
 *
 * SDK 연결/구독/오류 처리는 SteeringQuestController 가 담당한다.
 */
class SteeringLeftSequence(initial: Float?, private val thresholdDeg: Float = DEFAULT_THRESHOLD) {

    /** 왼쪽 회전 판정의 기준 각도. 시작 각도(없으면 첫 유효 수신)로 잡는다. */
    private var baseline: Float? = initial
    private var done = false

    val isDone: Boolean get() = done

    /**
     * 각도 하나를 받아 판정한다.
     * @return 이 입력으로 "새로" 완료되었으면 true.
     */
    fun onSignal(angle: Float?): Boolean {
        if (angle == null) return false
        if (done) return false
        val base = baseline
        if (base == null) {
            // 기준이 아직 없으면 이 값을 기준으로 삼는다(완료 아님).
            baseline = angle
            return false
        }
        // SDK 정의: 왼쪽 = 음수. 기준보다 THRESHOLD 이상 작아지면(왼쪽으로 돈 것) 완료.
        if (base - angle >= thresholdDeg) {
            done = true
            return true
        }
        return false
    }

    /** 현재 기준(테스트/디버그용). */
    fun baselineOrNull(): Float? = baseline

    /** 안내 문구. */
    fun guideText(): String = if (done) "좋아, 운전대를 왼쪽으로 돌렸어!" else "운전대를 왼쪽으로 조금 돌려줘."

    companion object {
        /** 왼쪽으로 "조금" 돌렸다고 인정하는 최소 각도(도). */
        const val DEFAULT_THRESHOLD = 15f
    }
}
