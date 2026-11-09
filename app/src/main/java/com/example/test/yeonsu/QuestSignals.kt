package com.example.test.yeonsu

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.*
import ai.pleos.playground.vehicle.constant.area.*
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit

/**
 * 차량 퀘스트의 "실제 동작 인식".
 * 퀘스트 이름별로 (어떤 차량 값을 읽을지, 이전 값 → 지금 값이 어떻게 바뀌면 완료인지)를 정해 둔다.
 * 아래 항목은 에뮬레이터 가상 콘솔로 값을 바꿔서 앱이 실제로 읽는 것을 확인한 것만 넣었다.
 */
class Detector(val hint: String, val read: (Vehicle, (Any?) -> Unit) -> Unit, val done: (prev: Any?, now: Any?) -> Boolean)

object QuestSignals {
    private val row1 = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT)
    private val seatL = SeatArea(SeatValue.ROW_1_LEFT)
    private val hvacAll = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT, SeatValue.ROW_2_LEFT, SeatValue.ROW_2_CENTER, SeatValue.ROW_2_RIGHT)

    private fun <T> r(f: (Vehicle, (T?) -> Unit, (Exception) -> Unit) -> Unit): (Vehicle, (Any?) -> Unit) -> Unit =
        { v, cb -> try { f(v, { x -> cb(x) }, { }) } catch (_: Throwable) {} }

    /** 이전에는 조건이 아니었다가 지금 조건을 만족하게 되면 완료. */
    private fun becomes(p: (Any) -> Boolean): (Any?, Any?) -> Boolean = { prev, now -> prev != null && now != null && !p(prev) && p(now) }
    private fun num(x: Any) = (x as? Number)?.toDouble() ?: Double.NaN
    private fun str(x: Any) = x.toString()

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
    private val chargePort = r<Boolean> { v, ok, f -> v.getEvBattery().isEvChargePortOpen(ok, f) }

    private const val RETRY = " (이미 그 상태면 한 번 바꿨다가 다시 해 주세요)"

    private val map: Map<String, Detector> = mapOf(
        "문 확인" to Detector("운전석 문을 열었다가 닫으면 자동으로 완료돼요", doorFL) { p, n -> p != null && n != null && num(p) > 0 && num(n) == 0.0 },
        "브레이크 위치" to Detector("브레이크 페달을 밟으면 자동으로 완료돼요$RETRY", brake, becomes { it == true }),
        "트렁크" to Detector("트렁크를 열면 자동으로 완료돼요$RETRY", trunk, becomes { num(it) > 0 }),
        "왼쪽 방향지시등" to Detector("왼쪽 방향지시등을 켜면 자동으로 완료돼요$RETRY", turn, becomes { str(it) == "LEFT" }),
        "오른쪽 방향지시등" to Detector("오른쪽 방향지시등을 켜면 자동으로 완료돼요$RETRY", turn, becomes { str(it) == "RIGHT" }),
        "방향지시등 끄기" to Detector("방향지시등을 켰다가 끄면 자동으로 완료돼요", turn, becomes { str(it) == "OFF" }),
        // "한 번 작동"은 단발(MIST) 위치를 읽을 수 없어서, 꺼져 있던 와이퍼가 작동 상태(ON)가 되면 완료로 본다
        "와이퍼 한 번" to Detector("꺼진 와이퍼를 작동시키면 자동으로 완료돼요 (어떤 모드든 괜찮아요)", wiperOn, becomes { str(it) == "ON" }),
        "자동 와이퍼" to Detector("와이퍼를 AUTO에 놓으면 자동으로 완료돼요$RETRY", wiper, becomes { str(it) == "AUTO" }),
        "저속 와이퍼" to Detector("와이퍼를 LOW에 놓으면 자동으로 완료돼요$RETRY", wiper, becomes { str(it) == "CONTINUOUS_LEVEL_1" }),
        "고속 와이퍼" to Detector("와이퍼를 HIGH에 놓으면 자동으로 완료돼요$RETRY", wiper, becomes { str(it) == "CONTINUOUS_LEVEL_3" }),
        "와이퍼 끄기" to Detector("와이퍼를 켰다가 OFF로 돌리면 자동으로 완료돼요", wiper, becomes { str(it) == "OFF" }),
        "하향등" to Detector("전조등(하향등)을 켜면 자동으로 완료돼요$RETRY", low, becomes { str(it) == "ON" }),
        "상향등" to Detector("상향등을 켜면 자동으로 완료돼요$RETRY", high, becomes { str(it) == "ON" }),
        "상향등 끄기" to Detector("상향등을 켰다가 끄면 자동으로 완료돼요", high, becomes { str(it) == "OFF" }),
        "운전석 창문" to Detector("운전석 창문을 내리면 자동으로 완료돼요$RETRY", winFL, becomes { num(it) > 0 }),
        "창문 닫기" to Detector("운전석 창문을 열었다가 완전히 닫으면 자동으로 완료돼요", winFL, becomes { num(it) == 0.0 }),
        "전체 문 잠금" to Detector("전체 문 잠금을 하면 자동으로 완료돼요$RETRY", allLock, becomes { it == true }),
        "에어컨" to Detector("A/C를 켜면 자동으로 완료돼요$RETRY", ac, becomes { it == true }),
        "온도 조절" to Detector("운전석 온도를 22도로 맞추면 자동으로 완료돼요$RETRY", temp, becomes { num(it) == 22.0 }),
        "풍량 조절" to Detector("풍량을 3단계로 맞추면 자동으로 완료돼요$RETRY", fanSpeed, becomes { num(it) == 3.0 }),
        "송풍 방향" to Detector("바람 방향을 얼굴 쪽으로 바꾸면 자동으로 완료돼요$RETRY", fanDir, becomes { str(it).contains("FACE") }),
        "내기순환" to Detector("내기순환을 켜면 자동으로 완료돼요$RETRY", recirc, becomes { it == true }),
        "외기 모드" to Detector("내기순환을 껐다가 외기 모드로 돌리면 자동으로 완료돼요", recirc, becomes { it == false }),
        "앞유리 성에 제거" to Detector("앞유리 성에 제거를 켜면 자동으로 완료돼요$RETRY", defrost, becomes { it == true }),
        "시트 열선·통풍" to Detector("운전석 시트 통풍 1단계를 켜면 자동으로 완료돼요$RETRY", seatTemp, becomes { num(it) == -1.0 }),
        // SDK 정의: 왼쪽으로 조향하면 각도가 음수(에뮬레이터 콘솔은 부호가 반대로 전달됨)
        "운전대 방향" to Detector("운전대를 왼쪽으로 돌리면 자동으로 완료돼요$RETRY", wheel, becomes { num(it) < -5.0 }),
        "충전구 열기" to Detector("충전구를 열면 자동으로 완료돼요$RETRY", chargePort, becomes { it == true }),
    )

    fun find(questName: String): Detector? = map[questName]
    val names: Set<String> get() = map.keys
}
