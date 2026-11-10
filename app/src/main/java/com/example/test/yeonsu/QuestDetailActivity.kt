package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import ai.pleos.playground.vehicle.Vehicle

/**
 * 연저씨 · 퀘스트 상세/진행/완료 (HTML detail.js, success).
 * 아래쪽 "미리보기 조작"은 화면 확인용이다. 실제 차량 조작을 판정하지 않으며, 이후 SDK 관찰값으로 대체한다.
 */
class QuestDetailActivity : ScreenActivity() {
    override fun screenBackground(): android.graphics.drawable.Drawable = questBackground()
    override val parentScreen: Class<out Activity> = QuestListActivity::class.java
    private val store by lazy { QuestStore(this) }
    private var success = false

    // ── 실제 차량 신호: QuestSignals에 정의된 퀘스트는 차량 값이 바뀌면 자동 완료 ──
    private var vehicle: Vehicle? = null
    private var detector: Detector? = null
    private var lastValue: Any? = null
    private var connected = false
    private val poll = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollTick = object : Runnable {
        override fun run() {
            if (success) return
            val v = vehicle; val d = detector
            if (v != null && d != null) d.read(v) { now -> runOnUiThread { onSignal(d, now) } }
            poll.postDelayed(this, 800)
        }
    }
    private fun onSignal(d: Detector, now: Any?) {
        if (success || now == null) return
        val prev = lastValue
        lastValue = now
        val q = QuestSession.selected()
        if (!connected) { connected = true; refresh() }
        val st = store.state(q)
        if (prev != null && st.status == "doing" && d.done(st.step, prev, now)) {
            android.util.Log.d("YeonsuSignal", "${q.name}: ${st.step + 1}단계 $prev -> $now")
            if (store.advance(q, verified = true)) { success = true; goTo(QuestDoneActivity::class.java) }
            else refresh()
        }
    }
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val d = QuestSignals.find(QuestSession.selected().name) ?: return
        try {
            detector = d
            vehicle = Vehicle(applicationContext).also { it.initialize() }
            poll.postDelayed(pollTick, 800)
        } catch (e: Throwable) { android.util.Log.e("YeonsuSignal", "차량 연결 실패 $e") }
    }
    override fun onDestroy() {
        poll.removeCallbacks(pollTick)
        try { vehicle?.release() } catch (_: Throwable) {}
        super.onDestroy()
    }
    private var primaryBtn: View? = null
    override fun helpHeadline() = "퀘스트 화면이에요"
    override fun helpItems(): List<HelpItem>? {
        val b = primaryBtn ?: return null
        if (success) return null
        val label = (b as? android.widget.TextView)?.text ?: "계속하기"
        return listOf(HelpItem(b, "퀘스트 하는 법", "$label 버튼을 누르고,\n퀘스트를 수행해보세요!", HelpPlace.ABOVE))
    }
    override fun build(content: FrameLayout, w: Int, h: Int) {
        primaryBtn = null
        val q = QuestSession.selected()
        val s = store.state(q)
        val cats = QuestData.cats.getValue(QuestSession.type)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#CCFFFFFF"), 36f, Color.WHITE, 3f)
            setPadding(dp(32f), dp(26f), dp(32f), dp(26f)); elevation = dp(4f).toFloat()
        }
        panel.addView(tv(if (success) "퀘스트 완료" else cats[q.c], 20f, Color.parseColor("#2E66AA"), true).apply {
            background = rounded(Color.parseColor("#DCEAFA"), 24f); setPadding(dp(18f), dp(8f), dp(18f), dp(8f))
        }, LinearLayout.LayoutParams(-2, -2))
        panel.addView(tv(if (success) "좋아, 잘 해냈어!" else q.name, 44f, ScreenActivity.NAVY, true).apply { setPadding(0, dp(16f), 0, 0) })
        // 제목 밑 부제목: 차량 퀘스트는 아래 단계 목록과 같은 말이라 보여주지 않는다(완료 화면의 칭찬 문구만 보여준다)
        if (success || QuestSession.type != "vehicle") panel.addView(tv(if (success) (q.doneMsg ?: "작은 확인 하나가 익숙한 습관이 되는 거야.") else q.desc, 22f, ScreenActivity.GRAY).apply {
            setPadding(0, dp(12f), 0, dp(8f)); setLineSpacing(0f, 1.3f)
        })
        val steps = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (q.tip != null) {
            steps.addView(tv("핵심: " + q.tip, 20f, Color.parseColor("#7A4A00"), true).apply {
                background = rounded(Color.parseColor("#FFF1CC"), 18f); setPadding(dp(20f), dp(16f), dp(20f), dp(16f)); setLineSpacing(0f, 1.2f)
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })
        }
        if (q.tip == null || q.steps.size > 1) q.steps.forEachIndexed { i, name ->
            val ok = i < s.step
            steps.addView(tv((if (ok) "✓ " else "○ ") + name, 22f, if (ok) Color.parseColor("#2D7653") else ScreenActivity.NAVY).apply {
                background = rounded(Color.parseColor(if (ok) "#D9EEE1" else "#EAF0F8"), 18f); setPadding(dp(20f), dp(16f), dp(20f), dp(16f))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })
        }
        panel.addView(steps, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f); bottomMargin = dp(18f) })

        val actions = LinearLayout(this).apply { gravity = Gravity.START }
        if (success) {
            actions.addView(button("다른 퀘스트") { goTo(QuestListActivity::class.java) })
            actions.addView(button("기록 보기", primary = false) { goTo(RecordsActivity::class.java) },
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
        } else {
            val label = when (s.status) { "doing" -> "계속하기"; "done" -> "다시 해보기"; else -> "퀘스트 시작" }
            primaryBtn = button(label) {
                if (s.status == "doing") Toast.makeText(this, "이미 진행 중이에요. ${q.desc}", Toast.LENGTH_SHORT).show()
                else { store.start(q); refresh() }
            }
            actions.addView(primaryBtn, LinearLayout.LayoutParams(0, -2, 1f))
            val stopBtn = button("중단", primary = false, enabled = s.status == "doing") {
                store.stop(q); refresh()
                Toast.makeText(this, "퀘스트를 중단했어요. 완료 기록은 추가하지 않았어요.", Toast.LENGTH_SHORT).show()
            }
            actions.addView(stopBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14f) })
            // 두 버튼이 위쪽 박스들과 같은 가로 폭을 반반씩 나눠 쓴다
        }
        panel.addView(actions)

        if (!success && s.status == "doing") {
            // 차량 퀘스트는 차량 신호로만 완료한다. 일상 퀘스트(차량 신호가 없는 임시 단계)만 버튼으로 넘긴다.
            if (QuestSession.type != "vehicle") {
                panel.addView(tv(q.steps[s.step.coerceAtMost(q.steps.size - 1)] + "　→", 22f, ScreenActivity.NAVY).apply {
                    background = rounded(Color.parseColor("#DFEBFA"), 16f); setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
                    isClickable = true; isFocusable = true
                    setOnClickListener { if (store.advance(q)) { success = true; goTo(QuestDoneActivity::class.java) } else refresh() }
                }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(18f) })
            }
        }

        val scroll = ScrollView(this).apply { addView(panel); isVerticalScrollBarEnabled = false }
        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(36f), 0, dp(36f), dp(24f))
        }
        val imgId = resources.getIdentifier("quest_img_${q.c}_${QuestSession.questIndex}", "drawable", packageName)
        val photo = !success && QuestSession.type == "vehicle" && imgId != 0
        // 사진은 원래 비율을 유지하고, 높이를 오른쪽 카드 높이와 똑같이 맞춘다.
        var photoH = (h * 0.66f).toInt()
        var photoW = 0
        if (photo && !compact) {
            val o = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeResource(resources, imgId, o)
            val aspect = (o.outWidth.toFloat() / o.outHeight).coerceAtMost(1.1f) // 거의 정사각형으로 보여준다(넓은 사진은 좌우를 조금 잘라냄)
            val availW = w - dp(72f) - dp(24f)
            // 카드 내용이 잘리지 않도록, 카드의 실제 필요 높이를 먼저 재서 사진 높이로 쓴다(카드 폭이 정해지면 다시 한 번 잰다).
            val maxW = (availW * 0.42f).toInt()
            var cardW = (availW * 0.58f).toInt()
            repeat(2) {
                panel.measure(android.view.View.MeasureSpec.makeMeasureSpec(cardW, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED))
                photoH = panel.measuredHeight.coerceIn((h * 0.5f).toInt(), (h * 0.9f).toInt())
                photoW = minOf((photoH * aspect).toInt(), maxW)
                cardW = availW - photoW
            }
        }
        val poseView: android.view.View = if (photo) android.widget.ImageView(this).apply {
            setImageResource(imgId)
            scaleType = if (compact) android.widget.ImageView.ScaleType.FIT_CENTER else android.widget.ImageView.ScaleType.CENTER_CROP
            if (!compact) {
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: android.view.View, o: android.graphics.Outline) { o.setRoundRect(0, 0, v.width, v.height, dp(30f).toFloat()) }
                }
            }
        } else pose(if (success) 2 else 0)
        if (compact) {
            root.addView(poseView, LinearLayout.LayoutParams(-1, 0, 0.28f))
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 0.72f))
        } else if (photo) {
            // 카드(scroll)도 같은 높이로 고정. 내용이 더 길면 카드 안에서 스크롤된다.
            scroll.isFillViewport = true
            panel.gravity = Gravity.CENTER_VERTICAL
            root.addView(poseView, LinearLayout.LayoutParams(photoW, photoH))
            root.addView(scroll, LinearLayout.LayoutParams(0, photoH, 1f).apply { leftMargin = dp(24f) })
        } else {
            root.addView(poseView, LinearLayout.LayoutParams(0, -1, 0.40f))
            root.addView(scroll, LinearLayout.LayoutParams(0, -2, 0.60f).apply { leftMargin = dp(24f) })
        }
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }
}
