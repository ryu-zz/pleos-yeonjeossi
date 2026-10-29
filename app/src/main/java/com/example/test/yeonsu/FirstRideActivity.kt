package com.example.test.yeonsu

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
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

    private lateinit var bubble: TextView      // 아저씨 말 (본문)
    private lateinit var hint: TextView        // 상태 칩 텍스트
    private lateinit var statusChip: LinearLayout
    private lateinit var statusDot: View
    private lateinit var stepLabel: TextView   // STEP 2 / 8
    private lateinit var stepTitle: TextView   // 큰 제목
    private lateinit var progress: View
    private lateinit var progressTrack: FrameLayout
    private lateinit var listBox: LinearLayout
    private lateinit var mainBtn: Button
    private lateinit var simBtn: TextView
    private val rows = mutableListOf<LinearLayout>()

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
        stepLabel.text = "STEP ${i + 1} / ${steps.size}"
        stepTitle.text = s.title
        setProgress((i).toFloat() / (steps.size - 1))
        rows.forEachIndexed { k, row -> renderRow(row, k, i) }
        statusChip.visibility = View.GONE
        when (s.check) {
            Check.INFO -> { button("시작할게요") { showStep(i + 1) } }
            Check.SELF -> { chip("직접 확인 · 맞췄으면 아래 버튼을 눌러 주세요", false); button("됐어요") { pass("좋아요.") } }
            Check.SAVE_SEAT -> {
                chip("시트 위치를 읽는 중", true)
                mainBtn.visibility = View.GONE
                readSeatPosition {
                    prefs.edit()
                        .putInt(KEY_FORE, seatForeAft ?: Int.MIN_VALUE)
                        .putInt(KEY_HEIGHT, seatHeight ?: Int.MIN_VALUE)
                        .putInt(KEY_BACK, seatBack ?: Int.MIN_VALUE)
                        .apply()
                    chip("저장됨 · 앞뒤 ${fmt(seatForeAft)}  높이 ${fmt(seatHeight)}  등받이 ${fmt(seatBack)}", false)
                    button("다음") { showStep(i + 1) }
                }
            }
            Check.VEHICLE -> {
                mainBtn.visibility = View.GONE
                chip(vehicleHint(s.title), true)
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
        "안전벨트" -> "차가 확인 중 · 현재 ${fmtB(seatBelt, "체결됨", "미체결")}"
        "브레이크" -> "차가 확인 중 · 현재 ${fmtB(brake, "밟음", "안 밟음")}"
        else -> ""
    }

    private fun onVehicleChanged() {
        if (prefs.getBoolean(KEY_DONE, false) && autoMode) { renderAutoCheck(); return }
        if (index !in steps.indices) return
        val s = steps[index]
        if (s.check != Check.VEHICLE) return
        chip(vehicleHint(s.title), true)
        val ok = when (s.title) { "안전벨트" -> seatBelt == true; "브레이크" -> brake == true; else -> false }
        if (ok) pass(if (s.title == "안전벨트") "딸깍. 통과입니다." else "좋아요. 통과입니다.")
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
        rows.forEachIndexed { k, row -> renderRow(row, k, steps.size) }
        setProgress(1f)
        stepLabel.text = "COMPLETE"
        stepTitle.text = "준비 끝"
        say("앞으로는 아저씨가 알아서 확인할게요.")
        chip("첫 탑승 세팅 완료 · 다음부터는 자동 점검만 해요", false)
        button("다음 탑승 해보기 (자동 점검)") { startAutoCheck() }
    }

    // ---------------- 두 번째 탑승부터: 자동 점검 ----------------
    private var autoMode = false
    private fun startAutoCheck() {
        autoMode = true
        listBox.visibility = View.GONE
        stepLabel.text = "AUTO CHECK"
        stepTitle.text = "출발 전 점검"
        setProgress(1f)
        say("다시 왔네요. 조용히 확인해 볼게요.")
        readSeatPosition { renderAutoCheck() }
        button("처음부터 다시 (첫 탑승)") {
            prefs.edit().clear().apply(); autoMode = false
            listBox.visibility = View.VISIBLE; showStep(0)
        }
    }

    private fun renderAutoCheck() {
        val saved = prefs.getInt(KEY_FORE, Int.MIN_VALUE)
        val problems = mutableListOf<String>()
        if (seatBelt != true) problems += "벨트 안 맸어요."
        if (saved != Int.MIN_VALUE && seatForeAft != null && seatForeAft != saved)
            problems += "자리가 바뀌었네요. 미러도 다시 봐요."
        chip("벨트 ${fmtB(seatBelt, "체결", "미체결")}  ·  시트 ${fmt(saved.takeIf { it != Int.MIN_VALUE })} → ${fmt(seatForeAft)}", problems.isNotEmpty())
        val msg = if (problems.isEmpty()) "다 좋아요. 오늘은 어떤 미션을 해볼까요?" else problems.joinToString(" ")
        if (bubble.text != msg) say(msg)
    }

    // ---------------- UI (gray · white · silver) ----------------
    private fun say(text: String) { bubble.text = text; speak(text) }

    private fun button(label: String, onClick: () -> Unit) {
        mainBtn.text = label
        mainBtn.visibility = View.VISIBLE
        mainBtn.setOnClickListener { onClick() }
    }

    private fun chip(text: String, live: Boolean) {
        hint.text = text
        statusChip.visibility = View.VISIBLE
        (statusDot.background as GradientDrawable).setColor(if (live) INK else SILVER)
    }

    private fun setProgress(f: Float) {
        progressTrack.post {
            progress.layoutParams = (progress.layoutParams as FrameLayout.LayoutParams).apply {
                width = (progressTrack.width * f.coerceIn(0f, 1f)).toInt()
            }
            progress.requestLayout()
        }
    }

    private fun renderRow(row: LinearLayout, k: Int, current: Int) {
        val circle = row.getChildAt(0) as TextView
        val label = row.getChildAt(1) as TextView
        val tag = row.getChildAt(2) as TextView
        val st = steps[k]
        val bg = circle.background as GradientDrawable
        when {
            k < current -> { bg.setColor(SILVER_LIGHT); bg.setStroke(0, 0); circle.text = "✓"; circle.setTextColor(GRAY); label.setTextColor(GRAY); label.setTypeface(null, Typeface.NORMAL) }
            k == current -> { bg.setColor(INK); bg.setStroke(0, 0); circle.text = "${k + 1}"; circle.setTextColor(Color.WHITE); label.setTextColor(INK); label.setTypeface(null, Typeface.BOLD) }
            else -> { bg.setColor(Color.WHITE); bg.setStroke(dp(1.5f), SILVER); circle.text = "${k + 1}"; circle.setTextColor(SILVER); label.setTextColor(SILVER); label.setTypeface(null, Typeface.NORMAL) }
        }
        label.text = st.title
        tag.text = when (st.check) { Check.VEHICLE -> "차량 확인"; Check.SELF -> "직접 확인"; else -> "" }
        tag.setTextColor(if (k == current) GRAY else SILVER)
    }

    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()
    private fun round(color: Int, r: Float, stroke: Int = 0, strokeColor: Int = 0) =
        GradientDrawable().apply { setColor(color); cornerRadius = r; if (stroke > 0) setStroke(stroke, strokeColor) }

    private fun buildUi() {
        window.statusBarColor = BG
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BG)
            setPadding(dp(40f), dp(32f), dp(40f), dp(32f))
        }

        // ---- 왼쪽: 메인 카드 ----
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(56f), dp(44f), dp(56f), dp(44f))
            background = round(Color.WHITE, dp(28f).toFloat(), dp(1f), LINE)
        }
        // 브랜드 줄
        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        brand.addView(TextView(this).apply {
            text = "연"; textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(INK); gravity = Gravity.CENTER
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(Color.WHITE, SILVER)).apply { shape = GradientDrawable.OVAL; setStroke(dp(1f), SILVER) }
        }, LinearLayout.LayoutParams(dp(52f), dp(52f)))
        brand.addView(TextView(this).apply {
            text = "너의 연수 아저씨"; textSize = 22f; setTextColor(GRAY); letterSpacing = 0.02f; setPadding(dp(12f), 0, 0, 0)
        })
        card.addView(brand)

        // 진행 바
        progressTrack = FrameLayout(this).apply { background = round(LINE, dp(2f).toFloat()) }
        progress = View(this).apply { background = round(INK, dp(2f).toFloat()) }
        progressTrack.addView(progress, FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT))
        card.addView(progressTrack, LinearLayout.LayoutParams(-1, dp(4f)).apply { topMargin = dp(36f) })

        stepLabel = TextView(this).apply { textSize = 18f; setTextColor(GRAY); letterSpacing = 0.18f; setTypeface(null, Typeface.BOLD) }
        card.addView(stepLabel, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(44f) })
        stepTitle = TextView(this).apply { textSize = 76f; setTextColor(INK); setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); letterSpacing = -0.02f }
        card.addView(stepTitle, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6f) })

        bubble = TextView(this).apply { textSize = 34f; setTextColor(GRAY); setLineSpacing(0f, 1.3f) }
        card.addView(bubble, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(20f) })

        // 상태 칩
        statusChip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22f), dp(14f), dp(26f), dp(14f))
            background = round(SURFACE, dp(20f).toFloat(), dp(1f), LINE)
            visibility = View.GONE
        }
        statusDot = View(this).apply { background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(INK) } }
        statusChip.addView(statusDot, LinearLayout.LayoutParams(dp(12f), dp(12f)))
        hint = TextView(this).apply { textSize = 22f; setTextColor(GRAY); setPadding(dp(14f), 0, 0, 0) }
        statusChip.addView(hint)
        card.addView(statusChip, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(36f) })

        // 여백
        card.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))

        mainBtn = Button(this).apply {
            textSize = 28f; setTextColor(Color.WHITE); isAllCaps = false; letterSpacing = 0.02f
            stateListAnimator = null
            background = round(INK, dp(24f).toFloat())
        }
        card.addView(mainBtn, LinearLayout.LayoutParams(-1, dp(104f)))
        simBtn = TextView(this).apply {
            text = "차량 신호 시뮬레이션 (테스트용)"
            textSize = 17f; setTextColor(SILVER); gravity = Gravity.CENTER; setPadding(0, dp(18f), 0, 0)
            visibility = View.GONE
        }
        card.addView(simBtn)
        root.addView(card, LinearLayout.LayoutParams(0, -1, 1.55f))

        // ---- 오른쪽: 단계 목록 ----
        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(40f), dp(44f), dp(40f), dp(44f))
            background = round(SURFACE, dp(28f).toFloat(), dp(1f), LINE)
        }
        listBox.addView(TextView(this).apply { text = "첫 탑승 세팅"; textSize = 18f; setTextColor(GRAY); letterSpacing = 0.12f; setTypeface(null, Typeface.BOLD) })
        steps.forEachIndexed { k, _ ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(18f), 0, dp(18f)) }
            row.addView(TextView(this).apply {
                textSize = 18f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD)
                background = GradientDrawable().apply { shape = GradientDrawable.OVAL }
            }, LinearLayout.LayoutParams(dp(44f), dp(44f)))
            row.addView(TextView(this).apply { textSize = 26f; setPadding(dp(20f), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(TextView(this).apply { textSize = 17f })
            rows += row
            listBox.addView(row, LinearLayout.LayoutParams(-1, -2).apply { if (k == 0) topMargin = dp(22f) })
        }
        root.addView(listBox, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = dp(24f) })
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
        // 투톤: 그레이(잉크) + 화이트, 실버로 보조
        private val INK = Color.parseColor("#2C2C2E")          // 진한 그레이
        private val GRAY = Color.parseColor("#6E6E73")         // 본문 그레이
        private val SILVER = Color.parseColor("#B8BCC2")       // 실버
        private val SILVER_LIGHT = Color.parseColor("#E3E5E8") // 옅은 실버
        private val LINE = Color.parseColor("#E8E9EB")
        private val SURFACE = Color.parseColor("#F7F7F8")
        private val BG = Color.parseColor("#EFEFF1")
    }
}
