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

    // ── 아래 6개는 팀장 브랜치에 없던 것. 에뮬레이터 콘솔로 값을 바꿔 확인한 뒤 추가 ──
    private val winLockRL = r<Boolean> { v, ok, f -> v.getWindow().getCurrentWindowLockState(WindowArea(WindowValue.ROW_1_LEFT, WindowValue.ROW_1_RIGHT, WindowValue.ROW_2_LEFT, WindowValue.ROW_2_RIGHT), ok, f) }
    /** 꺼짐(Not ready)일 때 SDK가 값 대신 오류를 주므로 OFF로 바꿔 읽는다. */
    private val cruise = r<Any> { v, ok, _ -> v.getDrivingMode().getCruiseControlState({ ok(it?.toString() ?: "OFF") }, { ok("OFF") }) }
    private val soc = r<Int> { v, ok, f -> v.getEvBattery().getEvBatteryStateOfCharge(ok, f) }
    /** 경고등 하나의 상태(OFF/ON/BLINK)만 꺼내 읽는다. 목록에 없으면 OFF로 본다. */
    private fun telltale(type: ai.pleos.playground.vehicle.constant.telltale.TelltaleType) = r<Any> { v, ok, f ->
        v.getSafety().getTelltalesInfo({ t -> ok(t?.telltales?.firstOrNull { it.type == type }?.state?.toString() ?: "OFF") }, f)
    }
    private val tpms = telltale(ai.pleos.playground.vehicle.constant.telltale.TelltaleType.TPMS)
    private val airbag = telltale(ai.pleos.playground.vehicle.constant.telltale.TelltaleType.AIRBAG)
    private const val RETRY = " (이미 그 상태면 한 번 바꿨다가 다시 해 주세요)"

    private val map: Map<String, Detector> = mapOf(
        "문 확인" to Detector("운전석 문을 열었다가 닫으면 자동으로 완료돼요", doorFL) { p, n -> p != null && n != null && num(p) > 0 && num(n) == 0.0 },
        "브레이크 위치" to Detector("브레이크 페달을 밟으면 자동으로 완료돼요$RETRY", brake, becomes { it == true }),
        "트렁크" to Detector("트렁크를 열면 자동으로 완료돼요$RETRY", trunk, becomes { num(it) > 0 }),
        "왼쪽 방향지시등" to Detector("왼쪽 방향지시등을 켜면 자동으로 완료돼요$RETRY", turn, becomes { str(it) == "LEFT" }),
        "오른쪽 방향지시등" to Detector("오른쪽 방향지시등을 켜면 자동으로 완료돼요$RETRY", turn, becomes { str(it) == "RIGHT" }),
        "방향지시등 끄기" to Detector("방향지시등을 켰다가 끄면 자동으로 완료돼요", turn, becomes { str(it) == "OFF" }),
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
        "뒷좌석 창문 잠금" to Detector("뒷좌석 창문 잠금을 켜면 자동으로 완료돼요$RETRY", winLockRL, becomes { it == true }),
        "크루즈 버튼" to Detector("크루즈 컨트롤을 준비(ENABLED) 상태로 켜면 자동으로 완료돼요$RETRY", cruise, becomes { str(it) == "ENABLED" || str(it) == "ACTIVATED" }),
        "충전 상태 확인" to Detector("배터리 충전율 값이 바뀌면 자동으로 완료돼요", soc) { p, n -> p != null && n != null && num(p) != num(n) },
        "타이어 경고등 연습" to Detector("타이어 경고등(TPMS)이 켜지면 자동으로 완료돼요$RETRY", tpms, becomes { str(it) != "OFF" }),
        "에어백 전개 연습" to Detector("에어백 경고등이 켜지면 자동으로 완료돼요$RETRY", airbag, becomes { str(it) != "OFF" }),
        "전자식 주차 브레이크" to Detector("주차 브레이크 표시등이 켜지면 자동으로 완료돼요$RETRY", telltale(ai.pleos.playground.vehicle.constant.telltale.TelltaleType.ELECTRONIC_PARKING_BRAKE), becomes { str(it) != "OFF" }),
        "충전구 열기" to Detector("충전구를 열면 자동으로 완료돼요$RETRY", chargePort, becomes { it == true }),
    )

    /**
     * "와이퍼 한 번": 레버를 살짝 올렸다가 내리면 와이퍼가 켜졌다 꺼진다. 꺼짐 → 켜짐 → 꺼짐 순서를 보면 완료.
     * (단발 위치 MIST는 에뮬레이터에서 만들 수 없어서 상태 변화로 판정한다.) 순서를 기억해야 해서 퀘스트에 들어올 때마다 새로 만든다.
     */
    private fun wiperOnce(): Detector {
        var stage = 0   // 0: 꺼진 상태를 기다림, 1: 켜짐을 기다림, 2: 다시 꺼짐을 기다림
        return Detector("와이퍼 레버를 살짝 올렸다가 내려 보세요 (켜졌다 꺼지면 자동으로 완료돼요)", wiperOn) { _, n ->
            val on = n != null && str(n) == "ON"
            when (stage) {
                0 -> { if (n != null && !on) stage = 1; false }
                1 -> { if (on) stage = 2; false }
                else -> if (n != null && !on) { stage = 0; true } else false
            }
        }
    }

    /** "차선 변경 깜빡이": 레버를 살짝 움직이면 몇 번 깜빡이고 저절로 꺼진다. 꺼짐 → 깜빡임 → 꺼짐 순서를 보면 완료. */
    private fun laneChange(): Detector {
        var stage = 0   // 0: 꺼진 상태를 기다림, 1: 깜빡임을 기다림, 2: 다시 꺼짐을 기다림
        return Detector("레버를 살짝 움직여 몇 번 깜빡이고 꺼지면 자동으로 완료돼요", turn) { _, n ->
            val on = n != null && str(n) != "OFF"
            when (stage) {
                0 -> { if (n != null && !on) stage = 1; false }
                1 -> { if (on) stage = 2; false }
                else -> if (n != null && !on) { stage = 0; true } else false
            }
        }
    }
    fun find(questName: String): Detector? = when (questName) {
        "와이퍼 한 번" -> wiperOnce()
        "차선 변경 깜빡이" -> laneChange()
        else -> map[questName]
    }
    val names: Set<String> get() = map.keys + "와이퍼 한 번" + "차선 변경 깜빡이"
}
