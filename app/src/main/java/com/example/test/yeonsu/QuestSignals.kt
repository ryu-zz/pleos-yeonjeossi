package com.example.test.yeonsu

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.*
import ai.pleos.playground.vehicle.constant.area.*
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit

/**
 * 차량 퀘스트의 "실제 동작 인식".
 * 퀘스트 이름별로 (어떤 차량 값을 읽을지, 단계마다 값이 어떻게 바뀌면 그 단계를 마친 것인지)를 정해 둔다.
 * 켜고 끄는 퀘스트는 한 퀘스트 안에 두 단계(켜기 → 끄기)로 들어 있고, 단계 번호는 퀘스트 진행 단계(store의 step)와 같다.
 *
 * 판정 규칙(모든 단계 공통):
 * - 한 단계는 "목표가 아닌 값 → 목표 값"으로 바뀌는 것을 수행 중에 직접 본 경우에만 끝난다.
 * - 끄기 단계의 목표는 "켠 상태가 아니게 됨"이 아니라 실제 꺼짐 값이다(예: 방향지시등 OFF, 와이퍼 OFF, 시트 0).
 *   그래서 왼쪽→오른쪽, AUTO→저속, 통풍 1단계→2단계 같은 다른 모드·강도 변경은 끄기로 치지 않는다.
 * - 값을 읽지 못하면(null) 판정하지 않는다.
 *
 * 판정기는 수행(세션)마다 새로 만든다([find]). 운전대처럼 단계 안에서 기억할 값이 있는 판정기가 있기 때문이다.
 */
class Detector(
    /** 단계별 안내 문구(화면 아래 초록 글씨). 단계 수만큼. */
    val hints: List<String>,
    val read: (Vehicle, (Any?) -> Unit) -> Unit,
    /** null 이 아니면 자동 판정을 보류한다(신뢰할 수 있는 신호를 확인하지 못함). 화면에 이 사유를 보여준다. */
    val pendingReason: String? = null,
    /** stage 번째 단계가 끝났는지: (현재 단계, 이전 값, 지금 값) */
    val done: (stage: Int, prev: Any?, now: Any?) -> Boolean,
)

object QuestSignals {
    private val row1 = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT)
    private val seatL = SeatArea(SeatValue.ROW_1_LEFT)
    private val hvacAll = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT, SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT)
    private fun <T> r(f: (Vehicle, (T?) -> Unit, (Exception) -> Unit) -> Unit): (Vehicle, (Any?) -> Unit) -> Unit =
        { v, cb -> try { f(v, { x -> cb(x) }, { cb(null) }) } catch (_: Throwable) { cb(null) } }

    /** 이전에는 조건이 아니었다가 지금 조건을 만족하게 되면 true. */
    internal fun becomes(p: (Any) -> Boolean, prev: Any?, now: Any?) = prev != null && now != null && !p(prev) && p(now)
    private fun num(x: Any) = (x as? Number)?.toDouble() ?: Double.NaN
    private fun str(x: Any) = x.toString()
    private const val RETRY = " (이미 그 상태면 한 번 바꿨다가 다시 해 주세요)"

    /**
     * 켜기 → 끄기 두 단계. isOn 은 "켠 상태"(1단계 목표), isOff 는 "실제로 끈 상태"(2단계 목표)다.
     * 2단계는 isOff 가 아니던 값이 isOff 가 될 때만 끝난다(다른 모드로 바뀌는 것은 끄기가 아니다).
     */
    internal fun pair(onHint: String, offHint: String, read: (Vehicle, (Any?) -> Unit) -> Unit,
                      isOn: (Any) -> Boolean, isOff: (Any) -> Boolean) =
        Detector(listOf(onHint + RETRY, offHint), read) { stage, p, n ->
            if (stage == 0) becomes(isOn, p, n) else becomes(isOff, p, n)
        }

    /** 한 단계짜리(값을 맞추는 퀘스트). */
    private fun single(hint: String, read: (Vehicle, (Any?) -> Unit) -> Unit, isTarget: (Any) -> Boolean) =
        Detector(listOf(hint + RETRY), read) { _, p, n -> becomes(isTarget, p, n) }

    private val doorFL = r<Int> { v, ok, f -> v.getDoor().getCurrentDoorPosition(DoorArea(DoorValue.ROW_1_LEFT), ok, f) }
    private val trunk = r<Int> { v, ok, f -> v.getDoor().getCurrentDoorPosition(DoorArea(DoorValue.REAR), ok, f) }
    private val frunk = r<Int> { v, ok, f -> v.getDoor().getCurrentDoorPosition(DoorArea(DoorValue.HOOD), ok, f) }
    private val allLock = r<Boolean> { v, ok, f -> v.getDoor().getDoorLockState(DoorArea(DoorValue.ROW_1_LEFT, DoorValue.ROW_1_RIGHT, DoorValue.ROW_2_LEFT, DoorValue.ROW_2_RIGHT), ok, f) }
    private val winFL = r<Int> { v, ok, f -> v.getWindow().getCurrentWindowPosition(WindowArea(WindowValue.ROW_1_LEFT), ok, f) }
    private val brake = r<Boolean> { v, ok, f -> v.getBrake().getCurrentBrakePedalStatus(ok, f) }
    private val turn = r<Any> { v, ok, f -> v.getTurnSignal().getCurrentTurnSignalState(ok, f) }
    private val wiper = r<Any> { v, ok, f -> v.getWiper().getCurrentWiperSwitch(WindowArea(WindowValue.FRONT_WINDSHIELD), ok, f) }
    private val wiperOn = r<Any> { v, ok, f -> v.getWiper().getCurrentWiperState(WindowArea(WindowValue.FRONT_WINDSHIELD), ok, f) }
    private val rearWiperOn = r<Any> { v, ok, f -> v.getWiper().getCurrentWiperState(WindowArea(WindowValue.REAR_WINDSHIELD), ok, f) }
    private val low = r<Any> { v, ok, f -> v.getLight().getCurrentLowBeamLightState(ok, f) }
    private val high = r<Any> { v, ok, f -> v.getLight().getCurrentHighBeamLightState(ok, f) }
    private val ac = r<Boolean> { v, ok, f -> v.getHVAC().getHvacAcState(hvacAll, ok, f) }
    private val recirc = r<Boolean> { v, ok, f -> v.getHVAC().getHvacRecirculationState(hvacAll, ok, f) }
    private val fanDir = r<Any> { v, ok, f -> v.getHVAC().getHvacFanDirection(row1, ok, f) }
    private val temp = r<Float> { v, ok, f -> v.getHVAC().getHvacTemperature(seatL, TemperatureUnit.CELSIUS, ok, f) }
    private val defrost = r<Boolean> { v, ok, f -> v.getHVAC().getHvacDefrosterState(WindowArea(WindowValue.FRONT_WINDSHIELD), ok, f) }
    private val rearDefrost = r<Boolean> { v, ok, f -> v.getHVAC().getHvacDefrosterState(WindowArea(WindowValue.REAR_WINDSHIELD), ok, f) }
    private val seatTemp = r<Int> { v, ok, f -> v.getSeat().getHvacSeatTemperature(seatL, ok, f) }
    private val wheel = r<Float> { v, ok, f -> v.getSteeringWheel().getCurrentSteeringWheelAngle(ok, f) }
    private val soc = r<Int> { v, ok, f -> v.getEvBattery().getEvBatteryStateOfCharge(ok, f) }
    private val chargePort = r<Boolean> { v, ok, f -> v.getEvBattery().isEvChargePortOpen(ok, f) }

    /** 운전대: 가운데로 인정하는 범위(±도). */
    const val STEER_CENTER_TOLERANCE = 5.0
    /** 운전대: 왼쪽으로 돌렸다고 인정하는 최소 각도(도). SDK 정의상 왼쪽은 음수다. */
    const val STEER_LEFT_THRESHOLD = 15.0

    /**
     * 운전대 왼쪽으로 돌렸다가 가운데로 돌려놓기.
     * - 기준 각도는 가운데(0도)다. SDK 정의: 왼쪽으로 조향하면 각도가 음수(에뮬레이터 콘솔은 부호가 반대로 전달됨).
     * - 시작 상태: 이 수행에서 운전대가 가운데(±5도)에 있는 것을 먼저 봐야 왼쪽 판정을 시작한다.
     *   시작할 때 이미 돌아가 있으면 먼저 가운데로 돌려놓아야 한다.
     * - 1단계: 가운데를 본 뒤 -15도 이하(왼쪽)로 가면 끝난다.
     * - 2단계: 가운데가 아니던 각도가 ±5도 안으로 돌아오면 끝난다. 오른쪽으로 크게 돌린 것은 가운데가 아니므로 인정하지 않는다.
     */
    internal fun steering(read: (Vehicle, (Any?) -> Unit) -> Unit = wheel): Detector {
        var centeredSeen = false
        fun centered(x: Any) = kotlin.math.abs(num(x)) <= STEER_CENTER_TOLERANCE
        fun left(x: Any) = num(x) <= -STEER_LEFT_THRESHOLD
        return Detector(listOf(
            "운전대를 가운데에 둔 상태에서 왼쪽으로 돌리면 첫 단계가 끝나요 (돌아가 있으면 먼저 가운데로 돌려놓아 주세요)",
            "가운데(±${STEER_CENTER_TOLERANCE.toInt()}도 안)로 돌려놓으면 자동으로 완료돼요",
        ), read) { stage, p, n ->
            if (n == null) false
            else if (stage == 0) {
                if (centered(n)) centeredSeen = true
                centeredSeen && left(n) && (p == null || !left(p))
            } else becomes(::centered, p, n)
        }
    }

    /**
     * 크루즈: 자동 판정 보류.
     * SDK CruiseControl 정의에서 ENABLED 는 "켜짐(대기)", ACTIVATED 는 "작동"이다. 대기는 꺼짐이 아니다.
     * 에뮬레이터에서 꺼짐(Not ready)은 SDK 값이 null(불명)로 와서, 신뢰할 수 있는 꺼짐 값을 확인하지 못했다.
     * 확인되기 전까지 자동 완료하지 않고 화면에 보류 상태를 보여준다.
     */
    private fun cruisePending() = Detector(
        listOf("크루즈 꺼짐 신호를 확인할 수 없어 자동 판정을 보류했어요", "크루즈 꺼짐 신호를 확인할 수 없어 자동 판정을 보류했어요"),
        read = { _, cb -> cb(null) },
        pendingReason = "크루즈 '꺼짐'을 차량 신호로 확인할 수 없어 자동 완료를 보류하고 있어요. (대기 상태는 꺼짐이 아니에요)",
        done = { _, _, _ -> false },
    )

    /** 퀘스트 이름 → 판정기 만들기. 수행마다 새로 만든다. */
    private val makers: Map<String, () -> Detector> = mapOf(
        "운전석 문 열었다가 닫기" to { pair("운전석 문을 열면 첫 단계가 끝나요", "운전석 문을 닫으면 자동으로 완료돼요", doorFL, { num(it) > 0 }, { num(it) == 0.0 }) },
        "브레이크 밟았다가 떼기" to { pair("브레이크 페달을 밟으면 첫 단계가 끝나요", "발을 떼면 자동으로 완료돼요", brake, { it == true }, { it == false }) },
        "왼쪽 방향지시등 켰다가 끄기" to { pair("왼쪽 방향지시등을 켜면 첫 단계가 끝나요", "방향지시등을 끄면(OFF) 자동으로 완료돼요", turn, { str(it) == "LEFT" }, { str(it) == "OFF" }) },
        "오른쪽 방향지시등 켰다가 끄기" to { pair("오른쪽 방향지시등을 켜면 첫 단계가 끝나요", "방향지시등을 끄면(OFF) 자동으로 완료돼요", turn, { str(it) == "RIGHT" }, { str(it) == "OFF" }) },
        // 단발(MIST) 위치는 구분할 수 없어서, 와이퍼 작동 상태가 켜졌다(ON) 꺼지는(OFF) 것으로 본다(이름도 "켰다가 끄기")
        "와이퍼 켰다가 끄기" to { pair("와이퍼를 켜면 첫 단계가 끝나요", "와이퍼를 끄면(OFF) 자동으로 완료돼요", wiperOn, { str(it) == "ON" }, { str(it) == "OFF" }) },
        "자동 와이퍼 켰다가 끄기" to { pair("와이퍼를 AUTO에 놓으면 첫 단계가 끝나요", "와이퍼를 OFF로 돌리면 자동으로 완료돼요", wiper, { str(it) == "AUTO" }, { str(it) == "OFF" }) },
        "저속 와이퍼 켰다가 끄기" to { pair("와이퍼를 LOW에 놓으면 첫 단계가 끝나요", "와이퍼를 OFF로 돌리면 자동으로 완료돼요", wiper, { str(it) == "CONTINUOUS_LEVEL_1" }, { str(it) == "OFF" }) },
        "고속 와이퍼 켰다가 끄기" to { pair("와이퍼를 HIGH에 놓으면 첫 단계가 끝나요", "와이퍼를 OFF로 돌리면 자동으로 완료돼요", wiper, { str(it) == "CONTINUOUS_LEVEL_3" }, { str(it) == "OFF" }) },
        "리어 와이퍼 켰다가 끄기" to { pair("리어 와이퍼를 켜면 첫 단계가 끝나요", "리어 와이퍼를 끄면(OFF) 자동으로 완료돼요", rearWiperOn, { str(it) == "ON" }, { str(it) == "OFF" }) },
        "하향등 켰다가 끄기" to { pair("전조등(하향등)을 켜면 첫 단계가 끝나요", "전조등을 끄면 자동으로 완료돼요", low, { str(it) == "ON" }, { str(it) == "OFF" || str(it) == "DAYTIME_RUNNING" }) },
        "상향등 켰다가 끄기" to { pair("상향등을 켜면 첫 단계가 끝나요", "상향등을 끄면 자동으로 완료돼요", high, { str(it) == "ON" }, { str(it) == "OFF" }) },
        "운전석 창문 열었다가 닫기" to { pair("운전석 창문을 내리면 첫 단계가 끝나요", "창문을 완전히 닫으면 자동으로 완료돼요", winFL, { num(it) > 0 }, { num(it) == 0.0 }) },
        // 잠금은 처음에 잠겨 있으므로 "푼 상태"가 첫 단계
        "전체 문 풀었다가 잠그기" to { pair("문 잠금을 풀면 첫 단계가 끝나요", "다시 잠그면 자동으로 완료돼요", allLock, { it == false }, { it == true }) },
        "트렁크 열었다가 닫기" to { pair("트렁크를 열면 첫 단계가 끝나요", "트렁크를 닫으면 자동으로 완료돼요", trunk, { num(it) > 0 }, { num(it) == 0.0 }) },
        "프렁크 열었다가 닫기" to { pair("프렁크를 열면 첫 단계가 끝나요", "프렁크를 닫으면 자동으로 완료돼요", frunk, { num(it) > 0 }, { num(it) == 0.0 }) },
        "에어컨 켰다가 끄기" to { pair("A/C를 켜면 첫 단계가 끝나요", "A/C를 끄면 자동으로 완료돼요", ac, { it == true }, { it == false }) },
        "온도 조절" to { single("운전석 온도를 22도로 맞추면 자동으로 완료돼요", temp) { num(it) == 22.0 } },
        "송풍 방향" to { single("바람 방향을 얼굴 쪽으로 바꾸면 자동으로 완료돼요", fanDir) { str(it).contains("FACE") } },
        "내기순환 켰다가 외기로" to { pair("내기순환을 켜면 첫 단계가 끝나요", "외기 모드로 돌리면 자동으로 완료돼요", recirc, { it == true }, { it == false }) },
        "앞유리 성에 제거 켰다가 끄기" to { pair("성에 제거를 켜면 첫 단계가 끝나요", "성에 제거를 끄면 자동으로 완료돼요", defrost, { it == true }, { it == false }) },
        "뒷유리 열선 켰다가 끄기" to { pair("뒷유리 열선을 켜면 첫 단계가 끝나요", "열선을 끄면 자동으로 완료돼요", rearDefrost, { it == true }, { it == false }) },
        // 시트: 음수=통풍, 0=꺼짐, 양수=열선. 끄기는 0(꺼짐)만 인정한다(2단계 통풍·열선으로 바꾸는 것은 끄기가 아니다)
        "시트 통풍 켰다가 끄기" to { pair("시트 통풍 1단계를 켜면 첫 단계가 끝나요", "통풍을 끄면(0) 자동으로 완료돼요", seatTemp, { num(it) == -1.0 }, { num(it) == 0.0 }) },
        "운전대 왼쪽으로 돌렸다가 풀기" to { steering() },
        "크루즈 켰다가 끄기" to { cruisePending() },
        // 팀 추가(2e9d92f): 배터리 충전율(SOC) 값이 수행 중에 바뀌면 완료 (콘솔 Battery SOC)
        "충전 상태 확인" to { Detector(listOf("배터리 충전율 값이 바뀌면 자동으로 완료돼요"), soc) { _, p, n -> p != null && n != null && num(p) != num(n) } },
        "충전구 열었다가 닫기" to { pair("충전구를 열면 첫 단계가 끝나요", "충전구를 닫으면 자동으로 완료돼요", chargePort, { it == true }, { it == false }) },
    )

    /** 새 판정기(수행마다 새로 만든다). */
    fun find(questName: String): Detector? = makers[questName]?.invoke()
    val names: Set<String> get() = makers.keys
}
