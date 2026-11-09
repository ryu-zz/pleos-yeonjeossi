package com.example.test.yeonsu

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.*
import ai.pleos.playground.vehicle.constant.area.*
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit

/**
 * 차량 퀘스트의 "실제 동작 인식".
 * 퀘스트 이름별로 (어떤 차량 값을 읽을지, 단계마다 값이 어떻게 바뀌면 그 단계를 마친 것인지)를 정해 둔다.
 * 켜고 끄는 퀘스트는 한 퀘스트 안에 두 단계(켜기 → 끄기)로 들어 있고, 단계 번호는 퀘스트 진행 단계(store의 step)와 같다.
 * 아래 항목은 에뮬레이터 가상 콘솔로 값을 바꿔서 앱이 실제로 읽는 것을 확인한 것만 넣었다.
 */
class Detector(
    /** 단계별 안내 문구(화면 아래 초록 글씨). 단계 수만큼. */
    val hints: List<String>,
    val read: (Vehicle, (Any?) -> Unit) -> Unit,
    /** stage 번째 단계가 끝났는지: (현재 단계, 이전 값, 지금 값) */
    val done: (stage: Int, prev: Any?, now: Any?) -> Boolean,
)

object QuestSignals {
    private val row1 = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT)
    private val seatL = SeatArea(SeatValue.ROW_1_LEFT)
    private val hvacAll = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT, SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT)

    private fun <T> r(f: (Vehicle, (T?) -> Unit, (Exception) -> Unit) -> Unit): (Vehicle, (Any?) -> Unit) -> Unit =
        { v, cb -> try { f(v, { x -> cb(x) }, { }) } catch (_: Throwable) {} }

    /** 이전에는 조건이 아니었다가 지금 조건을 만족하게 되면 true. */
    private fun becomes(p: (Any) -> Boolean, prev: Any?, now: Any?) = prev != null && now != null && !p(prev) && p(now)
    private fun num(x: Any) = (x as? Number)?.toDouble() ?: Double.NaN
    private fun str(x: Any) = x.toString()
    private const val RETRY = " (이미 그 상태면 한 번 바꿨다가 다시 해 주세요)"

    /** 켜기 → 끄기 두 단계. isOn 이 "켠 상태"를 뜻한다(끄기 단계는 isOn 이 아니게 되는 것). */
    private fun pair(onHint: String, offHint: String, read: (Vehicle, (Any?) -> Unit) -> Unit, isOn: (Any) -> Boolean) =
        Detector(listOf(onHint + RETRY, offHint), read) { stage, p, n ->
            if (stage == 0) becomes(isOn, p, n) else becomes({ !isOn(it) }, p, n)
        }
    /** 한 단계짜리(값을 맞추는 퀘스트). */
    private fun single(hint: String, read: (Vehicle, (Any?) -> Unit) -> Unit, isTarget: (Any) -> Boolean) =
        Detector(listOf(hint + RETRY), read) { _, p, n -> becomes(isTarget, p, n) }

    private val doorFL = r<Int> { v, ok, f -> v.getDoor().getCurrentDoorPosition(DoorArea(DoorValue.ROW_1_LEFT), ok, f) }
    private val trunk = r<Int> { v, ok, f -> v.getDoor().getCurrentDoorPosition(DoorArea(DoorValue.REAR), ok, f) }
    private val allLock = r<Boolean> { v, ok, f -> v.getDoor().getDoorLockState(DoorArea(DoorValue.ROW_1_LEFT, DoorValue.ROW_1_RIGHT, DoorValue.ROW_2_LEFT, DoorValue.ROW_2_RIGHT), ok, f) }
    private val winFL = r<Int> { v, ok, f -> v.getWindow().getCurrentWindowPosition(WindowArea(WindowValue.ROW_1_LEFT), ok, f) }
    private val brake = r<Boolean> { v, ok, f -> v.getBrake().getCurrentBrakePedalStatus(ok, f) }
    private val turn = r<Any> { v, ok, f -> v.getTurnSignal().getCurrentTurnSignalState(ok, f) }
    private val wiper = r<Any> { v, ok, f -> v.getWiper().getCurrentWiperSwitch(WindowArea(WindowValue.FRONT_WINDSHIELD), ok, f) }
    private val wiperOn = r<Any> { v, ok, f -> v.getWiper().getCurrentWiperState(WindowArea(WindowValue.FRONT_WINDSHIELD), ok, f) }
    private val low = r<Any> { v, ok, f -> v.getLight().getCurrentLowBeamLightState(ok, f) }
    private val high = r<Any> { v, ok, f -> v.getLight().getCurrentHighBeamLightState(ok, f) }
    private val ac = r<Boolean> { v, ok, f -> v.getHVAC().getHvacAcState(hvacAll, ok, f) }
    private val recirc = r<Boolean> { v, ok, f -> v.getHVAC().getHvacRecirculationState(hvacAll, ok, f) }
    private val fanSpeed = r<Int> { v, ok, f -> v.getHVAC().getHvacFanSpeed(row1, ok, f) }
    private val fanDir = r<Any> { v, ok, f -> v.getHVAC().getHvacFanDirection(row1, ok, f) }
    private val temp = r<Float> { v, ok, f -> v.getHVAC().getHvacTemperature(seatL, TemperatureUnit.CELSIUS, ok, f) }
    private val defrost = r<Boolean> { v, ok, f -> v.getHVAC().getHvacDefrosterState(WindowArea(WindowValue.FRONT_WINDSHIELD), ok, f) }
    private val seatTemp = r<Int> { v, ok, f -> v.getSeat().getHvacSeatTemperature(seatL, ok, f) }
    private val wheel = r<Float> { v, ok, f -> v.getSteeringWheel().getCurrentSteeringWheelAngle(ok, f) }
    private val cruise = r<Any> { v, ok, f -> v.getDrivingMode().getCruiseControlState(ok, f) }
    private val chargePort = r<Boolean> { v, ok, f -> v.getEvBattery().isEvChargePortOpen(ok, f) }

    private val map: Map<String, Detector> = mapOf(
        "운전석 문 열었다가 닫기" to pair("운전석 문을 열면 첫 단계가 끝나요", "운전석 문을 닫으면 자동으로 완료돼요", doorFL) { num(it) > 0 },
        "브레이크 밟았다가 떼기" to pair("브레이크 페달을 밟으면 첫 단계가 끝나요", "발을 떼면 자동으로 완료돼요", brake) { it == true },
        "왼쪽 방향지시등 켰다가 끄기" to pair("왼쪽 방향지시등을 켜면 첫 단계가 끝나요", "방향지시등을 끄면 자동으로 완료돼요", turn) { str(it) == "LEFT" },
        "오른쪽 방향지시등 켰다가 끄기" to pair("오른쪽 방향지시등을 켜면 첫 단계가 끝나요", "방향지시등을 끄면 자동으로 완료돼요", turn) { str(it) == "RIGHT" },
        // 단발(MIST) 위치는 에뮬레이터 콘솔에서 만들 수 없어서, 와이퍼가 켜졌다 꺼지는 것으로 본다
        "와이퍼 한 번" to pair("와이퍼를 켜면 첫 단계가 끝나요", "와이퍼를 끄면 자동으로 완료돼요", wiperOn) { str(it) == "ON" },
        "자동 와이퍼 켰다가 끄기" to pair("와이퍼를 AUTO에 놓으면 첫 단계가 끝나요", "와이퍼를 끄면 자동으로 완료돼요", wiper) { str(it) == "AUTO" },
        "저속 와이퍼 켰다가 끄기" to pair("와이퍼를 LOW에 놓으면 첫 단계가 끝나요", "와이퍼를 끄면 자동으로 완료돼요", wiper) { str(it) == "CONTINUOUS_LEVEL_1" },
        "고속 와이퍼 켰다가 끄기" to pair("와이퍼를 HIGH에 놓으면 첫 단계가 끝나요", "와이퍼를 끄면 자동으로 완료돼요", wiper) { str(it) == "CONTINUOUS_LEVEL_3" },
        "하향등 켰다가 끄기" to pair("전조등(하향등)을 켜면 첫 단계가 끝나요", "전조등을 끄면 자동으로 완료돼요", low) { str(it) == "ON" },
        "상향등 켰다가 끄기" to pair("상향등을 켜면 첫 단계가 끝나요", "상향등을 끄면 자동으로 완료돼요", high) { str(it) == "ON" },
        "운전석 창문 열었다가 닫기" to pair("운전석 창문을 내리면 첫 단계가 끝나요", "창문을 완전히 닫으면 자동으로 완료돼요", winFL) { num(it) > 0 },
        // 잠금은 처음에 잠겨 있으므로 "푼 상태"가 첫 단계
        "전체 문 풀었다가 잠그기" to pair("문 잠금을 풀면 첫 단계가 끝나요", "다시 잠그면 자동으로 완료돼요", allLock) { it == false },
        "트렁크 열었다가 닫기" to pair("트렁크를 열면 첫 단계가 끝나요", "트렁크를 닫으면 자동으로 완료돼요", trunk) { num(it) > 0 },
        "에어컨 켰다가 끄기" to pair("A/C를 켜면 첫 단계가 끝나요", "A/C를 끄면 자동으로 완료돼요", ac) { it == true },
        "온도 조절" to single("운전석 온도를 22도로 맞추면 자동으로 완료돼요", temp) { num(it) == 22.0 },
        "풍량 조절" to single("풍량을 3단계로 맞추면 자동으로 완료돼요", fanSpeed) { num(it) == 3.0 },
        "송풍 방향" to single("바람 방향을 얼굴 쪽으로 바꾸면 자동으로 완료돼요", fanDir) { str(it).contains("FACE") },
        "내기순환 켰다가 외기로" to pair("내기순환을 켜면 첫 단계가 끝나요", "외기 모드로 돌리면 자동으로 완료돼요", recirc) { it == true },
        "앞유리 성에 제거 켰다가 끄기" to pair("성에 제거를 켜면 첫 단계가 끝나요", "성에 제거를 끄면 자동으로 완료돼요", defrost) { it == true },
        "시트 통풍 켰다가 끄기" to pair("시트 통풍 1단계를 켜면 첫 단계가 끝나요", "통풍을 끄면 자동으로 완료돼요", seatTemp) { num(it) == -1.0 },
        // SDK 정의: 왼쪽으로 조향하면 각도가 음수(에뮬레이터 콘솔은 부호가 반대로 전달됨). 끄기 단계는 가운데로 돌아오는 것
        "운전대 왼쪽으로 돌렸다가 풀기" to pair("운전대를 왼쪽으로 돌리면 첫 단계가 끝나요", "가운데로 돌려놓으면 자동으로 완료돼요", wheel) { num(it) < -5.0 },
        // 에뮬레이터 콘솔에서는 Driving Mode의 "Full autonomous"가 크루즈 작동(ACTIVATED), "Ready"가 대기(ENABLED)
        "크루즈 켰다가 끄기" to pair("크루즈를 켜면 첫 단계가 끝나요", "크루즈를 끄면 자동으로 완료돼요", cruise) { str(it) == "ACTIVATED" },
        "충전구 열었다가 닫기" to pair("충전구를 열면 첫 단계가 끝나요", "충전구를 닫으면 자동으로 완료돼요", chargePort) { it == true },
    )

    fun find(questName: String): Detector? = map[questName]
    val names: Set<String> get() = map.keys
}
