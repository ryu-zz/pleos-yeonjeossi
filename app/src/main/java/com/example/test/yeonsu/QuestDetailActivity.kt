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
    // 판정은 화면이 보이고(onStart~onStop) 퀘스트가 수행 중일 때만 한다. 세션(SignalSession)이 다른 수행·다른 퀘스트의
    // 늦은 응답과 겹친 조회를 걸러 낸다. 화면이 열릴 때의 퀘스트를 고정해 두고, 그 퀘스트에만 결과를 반영한다.
    private val quest: Quest by lazy { QuestSession.selected() }
    private var vehicle: Vehicle? = null
    private var hasDetector = false
    private var pendingReason: String? = null
    private var connected = false
    // 내비·음성 퀘스트(팀 추가): 음성 인식·내비 신호가 오면 자동 완료. 화면이 보이고 수행 중일 때만 반영한다.
    private var events: QuestEvents? = null
    /** idle(안 듣는 중) / connecting / listening / error */
    private var eventsState = "idle"
    private var eventsError = ""
    private var visible = false
    private val session by lazy { SignalSession(quest.id) { QuestSignals.find(quest.name) } }
    private val poll = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollTick = object : Runnable {
        override fun run() {
            if (success || !visible) return
            val v = vehicle
            val d = session.detector
            val t = session.request(android.os.SystemClock.uptimeMillis())
            if (v != null && d != null && t != null) d.read(v) { now -> runOnUiThread { onSignal(t, now) } }
            poll.postDelayed(this, 800)
        }
    }
    /** 화면이 보이고 수행 중이면 판정 세션을 새로 연다(이전 값·이탈 중 변화는 버린다). */
    private fun openSession() {
        poll.removeCallbacks(pollTick)
        session.end()
        if (!hasDetector || pendingReason != null || success || !visible || !store.state(quest).running) return
        session.begin()
        poll.postDelayed(pollTick, 800)
    }
    private fun closeSession() {
        session.end()
        poll.removeCallbacks(pollTick)
    }
    private fun onSignal(t: SignalSession.Ticket, now: Any?) {
        if (success || !visible) return
        if (!connected && now != null) { connected = true; refresh() }
        val st = store.state(quest)
        if (!st.running) { closeSession(); return }
        if (session.onResponse(t, st.step, now) != SignalSession.Result.STAGE_DONE) return
        android.util.Log.d("YeonsuSignal", "${quest.name}: ${st.step + 1}단계 -> $now")
        if (store.advance(quest, verified = true)) {
            success = true; closeSession(); closeEvents("완료"); goTo(QuestDoneActivity::class.java)
        } else refresh()
    }
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        // 내비·음성 퀘스트: 여기서는 연결하지 않는다. "퀘스트 시작"을 누른 뒤(또는 수행 중인 퀘스트로 돌아온 뒤) 화면이 보일 때만 연다.
        events = QuestEvents.find(quest.name)
        if (events != null) return
        val d = QuestSignals.find(quest.name) ?: return
        hasDetector = true
        pendingReason = d.pendingReason
        if (pendingReason != null) return   // 자동 판정 보류: 차량 연결·조회를 하지 않는다
        try {
            vehicle = Vehicle(applicationContext).also { it.initialize() }
        } catch (e: Throwable) { android.util.Log.e("YeonsuSignal", "차량 연결 실패 $e") }
    }
    /** 음성·내비 수신 시작: 화면이 보이고, 수행 중이고, 아직 완료 전일 때만. 늦게 온 이전 결과는 EventGate 가 버린다. */
    private fun openEvents() {
        val e = events ?: return
        if (success || !store.state(quest).running) return
        if (!visible) return
        if (e.isOpen && e.keepWhileHidden) return   // 가려진 동안 유지하던 수신을 그대로 쓴다
        eventsState = "connecting"; eventsError = ""
        e.open(this, object : QuestEvents.Callbacks {
            override fun onReady() = runOnUiThread { if (e.isOpen) { eventsState = "listening"; refresh() } }
            override fun onDone(why: String) = runOnUiThread {
                // 중단·화면 이탈·완료 뒤에 온 사건은 무시한다(이 화면을 연 퀘스트에만 반영)
                if (!e.isOpen || success || (!visible && !e.keepWhileHidden) || !store.state(quest).running) return@runOnUiThread
                android.util.Log.d("YeonsuSignal", "${quest.name}: $why 완료")
                e.close("완료")
                while (store.state(quest).running) { if (store.advance(quest, verified = true)) { success = true; break } }
                // 화면이 가려진 동안 완료되면(경유지) 돌아왔을 때 완료 화면으로 보낸다
                if (success) { if (visible) goTo(QuestDoneActivity::class.java) } else refresh()
            }
            override fun onError(message: String) = runOnUiThread {
                eventsState = "error"; eventsError = message; refresh()
            }
        })
        refresh()
    }
    private fun closeEvents(reason: String) {
        events?.close(reason)
        eventsState = "idle"
    }
    override fun onStart() {
        super.onStart()
        visible = true
        if (success && events?.keepWhileHidden == true) { goTo(QuestDoneActivity::class.java); return }
        openSession()
        openEvents()
    }
    override fun onStop() {
        visible = false
        closeSession()   // 홈 이동 등으로 화면이 안 보이면 조회·판정을 멈춘다
        if (events?.keepWhileHidden != true) closeEvents("화면 이탈")   // 녹음은 멈춘다(경유지 수신은 내비 화면에서 추가하므로 유지)
        super.onStop()
    }
    override fun onDestroy() {
        closeSession()
        try { vehicle?.release() } catch (_: Throwable) {}
        closeEvents("화면 종료")
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
        val q = quest
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
            // 완료한 퀘스트를 다시 해도 저장된 status 는 done 으로 남고, 수행 중 여부는 running 으로 본다.
            val label = when { pendingReason != null -> "자동 판정 보류 중"; s.running -> "계속하기"; s.status == "done" -> "다시 해보기"; else -> "퀘스트 시작" }
            primaryBtn = button(label, enabled = pendingReason == null) {
                if (s.running) Toast.makeText(this, "이미 진행 중이에요. ${q.desc}", Toast.LENGTH_SHORT).show()
                else { store.start(q); openSession(); openEvents(); refresh() }
            }
            actions.addView(primaryBtn, LinearLayout.LayoutParams(0, -2, 1f))
            val stopBtn = button("중단", primary = false, enabled = s.running) {
                closeSession(); closeEvents("중단"); store.stop(q); refresh()
                Toast.makeText(this, "퀘스트를 중단했어요. 완료 기록은 추가하지 않았어요.", Toast.LENGTH_SHORT).show()
            }
            actions.addView(stopBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14f) })
            // 두 버튼이 위쪽 박스들과 같은 가로 폭을 반반씩 나눠 쓴다
        }
        panel.addView(actions)
        // 내비·음성 퀘스트는 무엇을 하면 완료되는지 한 줄 안내 (진행 중일 때만). 오류가 나면 안내와 "다시 시도"를 보여준다.
        if (!success && s.running) events?.let { e ->
            when (eventsState) {
                "error" -> {
                    panel.addView(tv(eventsError, 20f, Color.parseColor("#B3261E"), true).apply { setPadding(0, dp(16f), 0, 0); setLineSpacing(0f, 1.2f) })
                    panel.addView(button("다시 시도", primary = false) { openEvents() }, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(10f) })
                }
                "listening" -> panel.addView(tv(e.hint, 20f, Color.parseColor("#2D7653"), true).apply { setPadding(0, dp(16f), 0, 0) })
                else -> panel.addView(tv("연결 중…", 20f, Color.parseColor("#2D7653"), true).apply { setPadding(0, dp(16f), 0, 0) })
            }
        }
        // 자동 판정 보류(신뢰할 수 있는 신호 없음): 사유를 보여주고 완료시키지 않는다
        pendingReason?.let { r0 ->
            val r = r0 + "\n이 퀘스트는 카테고리 뱃지 조건에서 빼 두었어요."
            panel.addView(tv(r, 20f, Color.parseColor("#9A5B00"), true).apply {
                background = rounded(Color.parseColor("#FFF1CC"), 16f); setPadding(dp(20f), dp(14f), dp(20f), dp(14f)); setLineSpacing(0f, 1.2f)
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18f) })
        }
        if (!success && s.running) {
            // 차량 신호로 판정하는 퀘스트: 지금 단계의 판정 기준을 보여준다(예: 끄기는 실제 OFF 값, 운전대는 가운데 ±5도).
            session.detector?.let { d ->
                panel.addView(tv((if (connected) "차량 연결됨 · " else "차량 연결 중… · ") + d.hints[s.step.coerceIn(0, d.hints.size - 1)],
                    18f, Color.parseColor("#2D7653"), true).apply { setPadding(0, dp(18f), 0, 0); setLineSpacing(0f, 1.2f) })
            }
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
