package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.listener.SeatForeAftPositionListener
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.listener.DoorPositionListener

/**
 * 연저씨 · 퀘스트 상세/진행/완료 (HTML detail.js, success).
 * 아래쪽 "미리보기 조작"은 화면 확인용이다. 실제 차량 조작을 판정하지 않으며, 이후 SDK 관찰값으로 대체한다.
 */
class QuestDetailActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestListActivity::class.java
    private val store by lazy { QuestStore(this) }
    private var success = false

    // ── 실제 차량 신호: "운전석 앞뒤 조절" 퀘스트는 운전석 앞뒤 위치가 바뀌면 자동 완료 ──
    private var vehicle: Vehicle? = null
    private val driver = SeatArea(SeatValue.ROW_1_LEFT)
    private var lastPos: Int? = null
    private var seatConnected = false
    private val seatListener = object : SeatForeAftPositionListener {
        override fun onSeatForeAftPositionUpdated(position: Int?) {
            android.util.Log.d("YeonsuSeat", "foreAft=$position last=$lastPos")
            runOnUiThread { onSeatPosition(position) }
        }
        override fun onFailed(e: Exception) { android.util.Log.e("YeonsuSeat", "구독 실패 $e") }
    }
    private fun onSeatPosition(pos: Int?) {
        val before = lastPos
        if (pos != null) lastPos = pos
        val q = QuestSession.selected()
        if (q.name != SEAT_QUEST || success || pos == null || before == null || pos == before) return
        if (store.state(q).status != "doing") return
        if (store.advance(q)) { success = true; refresh() }
    }
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val qn = QuestSession.selected().name
        if (qn == TRUNK_QUEST) { initTrunk(); return }
        if (qn != SEAT_QUEST) return
        try {
            val v = Vehicle(applicationContext).also { it.initialize() }
            vehicle = v
            val seat = v.getSeat()
            seat.getCurrentSeatForeAftPosition(driver, { lastPos = it; android.util.Log.d("YeonsuSeat", "초기 foreAft=$it"); seatConnected = true; runOnUiThread { refresh() } },
                { android.util.Log.e("YeonsuSeat", "읽기 실패 $it") })
            seat.registerSeatForeAftPosition(driver, seatListener)
            poll.postDelayed(pollTick, 1000)
        } catch (e: Throwable) { android.util.Log.e("YeonsuSeat", "차량 연결 실패 $e") }
    }
    // ── "트렁크" 퀘스트: 뒷문(트렁크) 위치 값이 닫힘(0) → 열림(0보다 큼)으로 바뀌면 자동 완료 ──
    private val trunk = DoorArea(DoorValue.REAR)
    private var lastTrunk: Int? = null
    private val trunkListener = object : DoorPositionListener {
        override fun onPositionUpdated(pos: Int?) {
            android.util.Log.d("YeonsuTrunk", "event pos=$pos last=$lastTrunk")
            runOnUiThread { onTrunk(pos) }
        }
        override fun onFailed(e: Exception) { android.util.Log.e("YeonsuTrunk", "구독 실패 $e") }
    }
    private fun onTrunk(pos: Int?) {
        val before = lastTrunk
        if (pos != null) lastTrunk = pos
        val q = QuestSession.selected()
        if (q.name != TRUNK_QUEST || success || pos == null || before == null) return
        if (before <= 0 && pos > 0 && store.state(q).status == "doing") {
            if (store.advance(q)) { success = true; refresh() }
        }
    }
    private fun initTrunk() {
        try {
            val v = Vehicle(applicationContext).also { it.initialize() }
            vehicle = v
            val door = v.getDoor()
            door.getCurrentDoorPosition(trunk, { lastTrunk = it; android.util.Log.d("YeonsuTrunk", "초기 pos=$it"); seatConnected = true; runOnUiThread { refresh() } },
                { android.util.Log.e("YeonsuTrunk", "읽기 실패 $it") })
            door.registerDoorPosition(trunk, trunkListener)
            poll.postDelayed(pollTick, 1000)
        } catch (e: Throwable) { android.util.Log.e("YeonsuTrunk", "차량 연결 실패 $e") }
    }
    // 구독 신호가 오지 않는 경우를 대비해 1초마다 현재 값을 직접 읽어 비교한다.
    private val poll = android.os.Handler(android.os.Looper.getMainLooper())
    private val pollTick = object : Runnable {
        override fun run() {
            if (success) return
            if (QuestSession.selected().name == TRUNK_QUEST) {
                try {
                    vehicle?.getDoor()?.getCurrentDoorPosition(trunk,
                        { p -> android.util.Log.d("YeonsuTrunk", "poll pos=$p"); runOnUiThread { onTrunk(p) } },
                        { android.util.Log.e("YeonsuTrunk", "poll 실패 $it") })
                } catch (_: Throwable) {}
                poll.postDelayed(this, 1000); return
            }
            try {
                vehicle?.getSeat()?.getCurrentSeatForeAftPosition(driver,
                    { p -> android.util.Log.d("YeonsuSeat", "poll foreAft=$p"); runOnUiThread { onSeatPosition(p) } },
                    { android.util.Log.e("YeonsuSeat", "poll 실패 $it") })
            } catch (_: Throwable) {}
            poll.postDelayed(this, 1000)
        }
    }
    override fun onDestroy() {
        poll.removeCallbacks(pollTick)
        try { vehicle?.getSeat()?.unregisterSeatForeAftPosition(driver, seatListener); vehicle?.getDoor()?.unregisterDoorPosition(trunk, trunkListener); vehicle?.release() } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun build(content: FrameLayout, w: Int, h: Int) {
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
        panel.addView(tv(if (success) (q.doneMsg ?: "작은 확인 하나가 익숙한 습관이 되는 거야.") else q.desc, 22f, ScreenActivity.GRAY).apply {
            setPadding(0, dp(12f), 0, dp(8f)); setLineSpacing(0f, 1.3f)
        })
        val steps = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (q.tip != null) {
            steps.addView(tv("핵심: " + q.tip, 20f, Color.parseColor("#7A4A00"), true).apply {
                background = rounded(Color.parseColor("#FFF1CC"), 18f); setPadding(dp(20f), dp(16f), dp(20f), dp(16f)); setLineSpacing(0f, 1.2f)
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })
        } else q.steps.forEachIndexed { i, name ->
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
            actions.addView(button(label) {
                if (s.status == "doing") Toast.makeText(this, "이미 진행 중이에요. ${q.desc}", Toast.LENGTH_SHORT).show()
                else { store.start(q); refresh() }
            })
            actions.addView(button("중단", primary = false, enabled = s.status == "doing") {
                store.stop(q); refresh()
                Toast.makeText(this, "퀘스트를 중단했어요. 완료 기록은 추가하지 않았어요.", Toast.LENGTH_SHORT).show()
            }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
        }
        panel.addView(actions)

        if (!success && s.status == "doing") {
            if (q.name == SEAT_QUEST || q.name == TRUNK_QUEST) panel.addView(tv(
                if (!seatConnected) "차량 연결 중…" else if (q.name == TRUNK_QUEST) "차량 연결됨 · 트렁크를 열면 자동으로 완료돼요 (이미 열려 있으면 닫았다가 다시 열어요)" else "차량 연결됨 · 운전석 시트를 앞뒤로 움직이면 자동으로 완료돼요", 18f, Color.parseColor("#2D7653"), true
            ).apply { setPadding(0, dp(18f), 0, 0) })
            panel.addView(tv("미리보기 조작 · 실제 차량 조작이 아닙니다", 16f, Color.parseColor("#758298")).apply { setPadding(0, dp(22f), 0, dp(8f)) })
            panel.addView(tv((if (q.tip != null) "동작을 마쳤다고 치기" else q.steps[s.step]) + "　→", 22f, ScreenActivity.NAVY).apply {
                background = rounded(Color.parseColor("#DFEBFA"), 16f); setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
                isClickable = true; isFocusable = true
                setOnClickListener { if (store.advance(q)) success = true; refresh() }
            }, LinearLayout.LayoutParams(-2, -2))
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
    companion object { private const val SEAT_QUEST = "운전석 앞뒤 조절"; private const val TRUNK_QUEST = "트렁크" }
}
