package com.example.test.yeonsu.polling

/**
 * 폴링(주기 조회)으로만 판정 가능한 차량 퀘스트 정의 (SDK 비의존).
 *
 * 왜 폴링인가:
 * - 송풍 방향(getHvacFanDirection)과 와이퍼 단(getCurrentWiperSwitch)은 Pleos Vehicle SDK 에
 *   상태 변경 리스너(register...)가 없고 getter 만 있다(AAR 확인). 값이 바뀌어도 push 수신이 불가하다.
 * - 대신 getter 반환값이 모드별로 구분되는 것을 온디바이스 실측으로 확인했으므로(FACE/FLOOR/DEFROST,
 *   OFF/AUTO/CONTINUOUS_LEVEL_1/CONTINUOUS_LEVEL_3), 퀘스트 수행 중에만 짧게 반복 조회해서 판정한다.
 *
 * 각 퀘스트는 "현재 조회값이 목표 토큰인가(isTarget)"로 환원된다. 토큰은 SDK 열거형 이름(String)으로,
 * 이 파일은 SDK 에 의존하지 않도록 String 으로만 다룬다(수신원이 enum.name 을 넘긴다).
 *
 * 모든 폴링 퀘스트는 조작 연습형이다: 시작 시 이미 목표값이면 다른 값(상태)을 한 번 거친 뒤
 * 다시 목표값에 도달해야 완료한다.
 */
enum class PollQuest(
    val questId: String,
    val kind: PollKind,
    /** 목표로 인정하는 SDK 열거형 이름들(하나라도 일치하면 목표). */
    val targetTokens: Set<String>,
    val askGuide: String,
    val alreadyGuide: String,
    val doneGuide: String,
) {
    /** 송풍 방향: 얼굴 쪽(vehicle-5-3). getHvacFanDirection == FACE. */
    FAN_DIR_FACE(
        questId = "vehicle-5-3",
        kind = PollKind.FAN_DIRECTION,
        targetTokens = setOf("FACE", "FACE_AND_FLOOR", "FACE_AND_DEFROST", "FACE_AND_FLOOR_AND_DEFROST"),
        askGuide = "바람 방향을 얼굴 쪽으로 바꿔줘.",
        alreadyGuide = "이미 얼굴 쪽이야. 다른 방향으로 바꿨다가 다시 얼굴 쪽으로 해줘.",
        doneGuide = "좋아, 바람을 얼굴 쪽으로 바꿨어!",
    ),

    /** 자동 와이퍼(vehicle-2-1). getCurrentWiperSwitch == AUTO. */
    WIPER_AUTO(
        questId = "vehicle-2-1",
        kind = PollKind.WIPER_SWITCH,
        targetTokens = setOf("AUTO"),
        askGuide = "와이퍼 레버를 AUTO 위치에 놓아줘.",
        alreadyGuide = "이미 AUTO 야. 다른 위치로 옮겼다가 다시 AUTO 로 놓아줘.",
        doneGuide = "좋아, 자동 와이퍼를 켰어!",
    ),

    /** 저속 와이퍼(vehicle-2-2). getCurrentWiperSwitch == 연속 저단(CONTINUOUS_LEVEL_1). */
    WIPER_LOW(
        questId = "vehicle-2-2",
        kind = PollKind.WIPER_SWITCH,
        targetTokens = setOf("CONTINUOUS_LEVEL_1"),
        askGuide = "와이퍼 레버를 LOW 위치에 놓아줘.",
        alreadyGuide = "이미 저속이야. 다른 위치로 옮겼다가 다시 LOW 로 놓아줘.",
        doneGuide = "좋아, 저속 와이퍼를 켰어!",
    ),

    /** 고속 와이퍼(vehicle-2-3). getCurrentWiperSwitch == 연속 고단(CONTINUOUS_LEVEL_3). */
    WIPER_HIGH(
        questId = "vehicle-2-3",
        kind = PollKind.WIPER_SWITCH,
        targetTokens = setOf("CONTINUOUS_LEVEL_3"),
        askGuide = "와이퍼 레버를 HIGH 위치에 놓아줘.",
        alreadyGuide = "이미 고속이야. 다른 위치로 옮겼다가 다시 HIGH 로 놓아줘.",
        doneGuide = "좋아, 고속 와이퍼를 켰어!",
    );

    /** 조회값(SDK enum 이름)이 이 퀘스트의 목표인지. 모르면(null/빈값) false. */
    fun isTarget(token: String?): Boolean = token != null && token in targetTokens

    companion object {
        private val BY_ID = entries.associateBy { it.questId }
        fun isPollQuest(questId: String): Boolean = BY_ID.containsKey(questId)
        fun questKind(questId: String): PollQuest? = BY_ID[questId]
    }
}

/** 어떤 getter 를 조회할지. */
enum class PollKind { FAN_DIRECTION, WIPER_SWITCH }
