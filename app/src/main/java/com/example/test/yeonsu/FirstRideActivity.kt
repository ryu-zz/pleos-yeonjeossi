package com.example.test.yeonsu

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.SeatBeltBuckledStateListener
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit

/**
 * 너의 연수 아저씨 · 첫 탑승 세팅 미니 데모
 *
 * 차가 확인하는 단계(✔): 안전벨트, 브레이크
 * 사용자가 "됐어요"로 넘기는 단계(🗣): 시트, 등받이, 룸미러, 사이드미러
 * 시트 위치는 SDK로 읽어서 저장해 두고, 다음 탑승 때 바뀌었는지 비교한다.
 *
 * Virtual Console 조작: Seat Belt(Telltales) / Brake Pedal
 */
class FirstRideActivity : AppCompatActivity() {

    private enum class Check { VEHICLE, SELF, SAVE_SEAT, INFO }

    private data class Step(val title: String, val say: String, val check: Check)

    private val steps = listOf(
        Step("시작", "안녕하세요, 연수 아저씨예요. 출발 전에 하나씩 맞춰 볼게요.", Check.INFO),
        Step("안전벨트", "안전벨트부터 매요. 딸깍 소리 날 때까지요.", Check.VEHICLE),
        Step("시트 앞뒤", "브레이크를 끝까지 밟았을 때 무릎이 살짝 굽혀지게 시트를 맞춰요.", Check.SELF),
        Step("등받이", "등받이는 핸들 위에 손목을 올렸을 때 닿는 정도예요.", Check.SELF),
        Step("자리 기억", "좋아요. 지금 자리를 기억해 둘게요.", Check.SAVE_SEAT),
        Step("룸미러", "룸미러에 뒷유리가 꽉 차게 보이게 맞춰요.", Check.SELF),
        Step("사이드미러", "사이드미러는 내 차 옆구리가 4분의 1만 보이게, 하늘과 땅은 반반이요.", Check.SELF),
        Step("브레이크", "마지막이에요. 오른발로 브레이크를 꾹 밟아 보세요.", Check.VEHICLE),
    )

    private lateinit var vehicle: Vehicle
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var bubble: TextView
    private lateinit var hint: TextView
    private lateinit var listBox: LinearLayout
    private lateinit var mainBtn: Button
    private lateinit var simBtn: TextView
    private val rows = mutableListOf<TextView>()

    private var index = 0
    private var seatBelt: Boolean? = null
    private var brake: Boolean? = null
    private var seatForeAft: Int? = null
    private var seatHeight: Int? = null
    private var seatBack: Int? = null

    private val driver = SeatArea(SeatValue.ROW_1_LEFT)
    private val prefs by lazy { getSharedPreferences("yeonsu", Context.MODE_PRIVATE) }

    private val beltListener = object : SeatBeltBuckledStateListener {
        override fun onSeatBeltBuckledStateUpdated(p0: Boolean?) {
            Log.d(TAG, "belt=$p0"); seatBelt = p0; runOnUiThread { onVehicleChanged() }
        }
        override fun onFailed(p0: Exception) = logErr("벨트 구독", p0)
    }
    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(p0: Boolean?) {
            Log.d(TAG, "brake=$p0"); brake = p0; runOnUiThread { onVehicleChanged() }
        }
        override fun onFailed(p0: Exception) = logErr("브레이크 구독", p0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        initVehicle()
        initTts()
        if (prefs.getBoolean(KEY_DONE, false)) startAutoCheck() else showStep(0)
    }

    // ---------------- 차량 연결 ----------------
    private fun initVehicle() {
        vehicle = Vehicle(applicationContext)
        vehicle.initialize()
        val seat = vehicle.getSeat()
        seat.getCurrentSeatBeltBuckledState(driver, { seatBelt = it; runOnUiThread { onVehicleChanged() } }, { logErr("벨트", it) })
        seat.registerSeatBeltBuckledState(driver, beltListener)
        vehicle.getBrake().getCurrentBrakePedalStatus({ brake = it; runOnUiThread { onVehicleChanged() } }, { logErr("브레이크", it) })
        vehicle.getBrake().registerBrakePedalStatus(brakeListener)
        readSeatPosition()
    }

    private fun readSeatPosition(done: (() -> Unit)? = null) {
        val seat = vehicle.getSeat()
        seat.getCurrentSeatForeAftPosition(driver, { seatForeAft = it; Log.d(TAG, "foreAft=$it"); done?.let { d -> runOnUiThread(d) } }, { logErr("시트 앞뒤", it); done?.let { d -> runOnUiThread(d) } })
        seat.getCurrentSeatHeightPosition(driver, { seatHeight = it; Log.d(TAG, "height=$it") }, { logErr("시트 높이", it) })
        seat.getCurrentSeatBackrestAnglePosition(driver, { seatBack = it; Log.d(TAG, "backrest=$it") }, { logErr("등받이", it) })
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(applicationContext, TtsMode.ON_DEVICE).also { t ->
                t.initialize(object : TtsInit {
                    override fun onSuccess() { ttsReady = true; Log.d(TAG, "TTS ready"); runOnUiThread { speak(bubble.text.toString()) } }
                    override fun onFailure(errorType: TtsError) { Log.e(TAG, "TTS fail $errorType") }
                })
            }
        } catch (e: Exception) { Log.e(TAG, "TTS 예외 ${e.message}") }
    }

    private fun speak(text: String) {
        if (!ttsReady) return
        try { tts?.speak(text, true) } catch (e: Exception) { Log.e(TAG, "speak ${e.message}") }
    }

    private fun logErr(what: String, e: Exception) { Log.e(TAG, "$what 실패: ${e.javaClass.simpleName} ${e.message}") }

    // ---------------- 첫 탑승 흐름 ----------------
    private fun showStep(i: Int) {
        index = i
        val s = steps[i]
        say(s.say)
        rows.forEachIndexed { k, tv ->
            val st = steps[k]
            val mark = when {
                k < i -> "✅"
                k == i -> "👉"
                else -> "⬜"
            }
            val tag = when (st.check) { Check.VEHICLE -> " · 차가 확인"; Check.SELF -> " · 직접 확인"; else -> "" }
            tv.text = "$mark  ${st.title}$tag"
            tv.setTypeface(null, if (k == i) Typeface.BOLD else Typeface.NORMAL)
            tv.setTextColor(if (k < i) GREEN else if (k == i) NAVY else Color.GRAY)
        }
        when (s.check) {
            Check.INFO -> { hint.text = ""; button("시작할게요") { showStep(i + 1) } }
            Check.SELF -> { hint.text = "맞췄으면 아래 버튼을 눌러 주세요 (차가 확인할 수 없는 단계)"; button("됐어요") { pass("좋아요!") } }
            Check.SAVE_SEAT -> {
                hint.text = "시트 위치를 읽는 중…"
                mainBtn.visibility = View.GONE
                readSeatPosition {
                    prefs.edit()
                        .putInt(KEY_FORE, seatForeAft ?: Int.MIN_VALUE)
                        .putInt(KEY_HEIGHT, seatHeight ?: Int.MIN_VALUE)
                        .putInt(KEY_BACK, seatBack ?: Int.MIN_VALUE)
                        .apply()
                    hint.text = "저장한 자리 · 앞뒤 ${fmt(seatForeAft)} / 높이 ${fmt(seatHeight)} / 등받이 ${fmt(seatBack)}"
                    button("다음") { showStep(i + 1) }
                }
            }
            Check.VEHICLE -> {
                mainBtn.visibility = View.GONE
                hint.text = vehicleHint(s.title)
                simBtn.visibility = View.VISIBLE
                simBtn.setOnClickListener {
                    Log.d(TAG, "시뮬레이션: ${s.title}")
                    if (s.title == "안전벨트") seatBelt = true else brake = true
                    onVehicleChanged()
                }
                onVehicleChanged()
            }
        }
        if (s.check != Check.VEHICLE) simBtn.visibility = View.GONE
    }

    private fun vehicleHint(title: String) = when (title) {
        "안전벨트" -> "Virtual Console › Safety › Telltales에서 안전벨트 경고를 Off로 (현재: ${fmtB(seatBelt, "맴", "안 맴")})"
        "브레이크" -> "Virtual Console › Motion › Brake Pedal을 올려 보세요 (현재: ${fmtB(brake, "밟음", "안 밟음")})"
        else -> ""
    }

    private fun onVehicleChanged() {
        if (prefs.getBoolean(KEY_DONE, false) && autoMode) { renderAutoCheck(); return }
        if (index !in steps.indices) return
        val s = steps[index]
        if (s.check != Check.VEHICLE) return
        hint.text = vehicleHint(s.title)
        val ok = when (s.title) { "안전벨트" -> seatBelt == true; "브레이크" -> brake == true; else -> false }
        if (ok) pass(if (s.title == "안전벨트") "딸깍! 통과 🎉" else "꾹! 통과 🎉")
    }

    private var passing = false
    private fun pass(msg: String) {
        if (passing) return
        passing = true
        say(msg)
        mainBtn.visibility = View.GONE
        simBtn.visibility = View.GONE
        bubble.postDelayed({
            passing = false
            if (index + 1 < steps.size) showStep(index + 1) else finishFirstRide()
        }, 1400)
    }

    private fun finishFirstRide() {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
        rows.forEach { it.setTextColor(GREEN) }
        say("준비 끝! 앞으로는 아저씨가 알아서 확인할게요.")
        hint.text = "첫 탑승 세팅 완료 · 다음부터는 자동 점검만 해요"
        button("다음 탑승 해보기 (자동 점검)") { startAutoCheck() }
    }

    // ---------------- 두 번째 탑승부터: 자동 점검 ----------------
    private var autoMode = false
    private fun startAutoCheck() {
        autoMode = true
        rows.forEach { it.visibility = View.GONE }
        say("다시 왔네요. 조용히 확인해 볼게요.")
        readSeatPosition { renderAutoCheck() }
        button("처음부터 다시 (첫 탑승)") {
            prefs.edit().clear().apply(); autoMode = false
            rows.forEach { it.visibility = View.VISIBLE }; showStep(0)
        }
    }

    private fun renderAutoCheck() {
        val saved = prefs.getInt(KEY_FORE, Int.MIN_VALUE)
        val problems = mutableListOf<String>()
        if (seatBelt != true) problems += "벨트 안 맸어요."
        if (saved != Int.MIN_VALUE && seatForeAft != null && seatForeAft != saved)
            problems += "자리가 바뀌었네요. 미러도 다시 봐요."
        hint.text = "벨트 ${fmtB(seatBelt, "✅", "❌")}  ·  시트 앞뒤 저장 ${fmt(saved.takeIf { it != Int.MIN_VALUE })} → 지금 ${fmt(seatForeAft)}"
        val msg = if (problems.isEmpty()) "다 좋아요. 오늘은 어떤 미션을 해볼까요?" else problems.joinToString(" ")
        if (bubble.text != msg) say(msg)
    }

    // ---------------- UI ----------------
    private fun say(text: String) { bubble.text = text; speak(text) }

    private fun button(label: String, onClick: () -> Unit) {
        mainBtn.text = label
        mainBtn.visibility = View.VISIBLE
        mainBtn.setOnClickListener { onClick() }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#F3F6FB"))
            setPadding(48, 40, 48, 40)
        }
        // 왼쪽: 아저씨 + 말풍선
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        left.addView(TextView(this).apply { text = "🧢👓"; textSize = 72f; gravity = Gravity.CENTER })
        left.addView(TextView(this).apply {
            text = "너의 연수 아저씨"; textSize = 30f; setTypeface(null, Typeface.BOLD); setTextColor(NAVY); gravity = Gravity.CENTER
        })
        bubble = TextView(this).apply {
            textSize = 34f; setTextColor(Color.parseColor("#1F2A3C")); setPadding(40, 32, 40, 32)
            background = GradientDrawable().apply { setColor(Color.parseColor("#FFF3C4")); cornerRadius = 36f; setStroke(4, Color.parseColor("#FFC93C")) }
        }
        left.addView(bubble, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 24 })
        hint = TextView(this).apply { textSize = 20f; setTextColor(Color.parseColor("#5B6B86")); setPadding(8, 20, 8, 0) }
        left.addView(hint)
        mainBtn = Button(this).apply {
            textSize = 28f; setTextColor(Color.WHITE); isAllCaps = false
            background = GradientDrawable().apply { setColor(NAVY); cornerRadius = 40f }
        }
        left.addView(mainBtn, LinearLayout.LayoutParams(-1, 140).apply { topMargin = 28 })
        simBtn = TextView(this).apply {
            text = "🧪 차량 신호 시뮬레이션 (Virtual Console에 해당 카드가 없을 때만)"
            textSize = 18f; setTextColor(Color.parseColor("#7A8AA6")); gravity = Gravity.CENTER; setPadding(0, 24, 0, 0)
            visibility = View.GONE
        }
        left.addView(simBtn)
        root.addView(left, LinearLayout.LayoutParams(0, -1, 1.4f))

        // 오른쪽: 체크리스트
        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(40, 32, 40, 32)
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 32f; setStroke(3, Color.parseColor("#D8E1EE")) }
        }
        listBox.addView(TextView(this).apply { text = "첫 탑승 세팅"; textSize = 26f; setTypeface(null, Typeface.BOLD); setTextColor(NAVY) })
        steps.forEach { _ ->
            val tv = TextView(this).apply { textSize = 24f; setPadding(0, 18, 0, 18) }
            rows += tv; listBox.addView(tv)
        }
        root.addView(listBox, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = 40 })
        setContentView(root)
    }

    private fun fmt(v: Int?) = v?.toString() ?: "-"
    private fun fmtB(v: Boolean?, t: String, f: String) = when (v) { true -> t; false -> f; null -> "읽는 중" }

    override fun onDestroy() {
        try {
            vehicle.getSeat().unregisterSeatBeltBuckledState(driver, beltListener)
            vehicle.getBrake().unregisterBrakePedalStatus(brakeListener)
        } catch (_: Exception) {}
        vehicle.release()
        tts?.release()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "Yeonsu"
        private const val KEY_DONE = "first_ride_done"
        private const val KEY_FORE = "seat_fore"
        private const val KEY_HEIGHT = "seat_height"
        private const val KEY_BACK = "seat_back"
        private val NAVY = Color.parseColor("#1F3A5F")
        private val GREEN = Color.parseColor("#2E9E5B")
    }
}
