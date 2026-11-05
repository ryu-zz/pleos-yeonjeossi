package com.example.test.yeonsu.hvac

/**
 * 공조·시야 퀘스트(cat5) 종류와 판정 규칙 정의 (SDK 비의존).
 *
 * 각 퀘스트는 "차량에서 읽은 하나의 값이 목표 조건을 만족하는가(isTarget)"로 환원된다.
 * - Boolean 값(에어컨/내기순환/외기/성에제거) 또는 Int 레벨(풍량/사이드미러열선/시트통풍) 또는 Float(온도).
 * - 판정 로직은 공통적으로 "목표 조건 충족 여부(Boolean)"만 쓰므로, 수신원이 각 값을 isTarget 으로 바꿔 올린다.
 *
 * 송풍 방향(5-3)은 SDK 에 상태 변경 리스너(register)가 없어(getter 만 존재) 자동 판정 대상에서 제외(보류)한다.
 */
enum class HvacQuest(val questId: String) {
    /** 에어컨 켜기(vehicle-5-0): HVAC_AC_ON == true. 조작 연습형(끔→켬). */
    AC_ON("vehicle-5-0"),
    /** 온도 22도(vehicle-5-1): HVAC_TEMPERATURE_SET == 22.0. 상태 확인형(목표값 도달). */
    TEMP_22("vehicle-5-1"),
    /** 풍량 3단계(vehicle-5-2): HVAC_FAN_SPEED == 3. 상태 확인형(목표값 도달). */
    FAN_3("vehicle-5-2"),
    /** 내기순환 켜기(vehicle-5-4): HVAC_RECIRC_ON == true. 조작 연습형(끔→켬). */
    RECIRC_ON("vehicle-5-4"),
    /** 외기 모드(vehicle-5-5): HVAC_RECIRC_ON == false. 조작 연습형(켬→끔). */
    RECIRC_OFF("vehicle-5-5"),
    /** 앞유리 성에 제거(vehicle-5-6): HVAC_DEFROSTER(앞유리) == true. 조작 연습형(끔→켬). */
    DEFROST_ON("vehicle-5-6"),
    /** 사이드미러 열선(vehicle-5-7): HVAC_SIDE_MIRROR_HEAT 레벨 > 0. 조작 연습형(0→>0). */
    MIRROR_HEAT_ON("vehicle-5-7"),
    /** 시트 통풍 1단계 이상(vehicle-5-8): HVAC_SEAT_VENTILATION 레벨 > 0. 조작 연습형(0→>0). */
    SEAT_VENT_ON("vehicle-5-8");

    /**
     * 시작 시 이미 목표 상태일 때, 바로 완료하지 않고 반대 상태를 한 번 거쳐야 하는지.
     * - 조작 연습형(버튼 눌러보기): true. 안내가 '조작'을 요구하므로 시작 상태만으로 완료하지 않는다.
     * - 상태 확인형(목표값 맞추기: 온도 22도, 풍량 3단계): false. 이미 목표면 바로 완료해도 된다.
     */
    val requiresTransition: Boolean
        get() = when (this) {
            TEMP_22, FAN_3 -> false
            else -> true
        }

    companion object {
        private val BY_ID = entries.associateBy { it.questId }
        fun isHvacQuest(questId: String): Boolean = BY_ID.containsKey(questId)
        fun questKind(questId: String): HvacQuest? = BY_ID[questId]
    }
}
