package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

/**
 * 연저씨 · 퀘스트 상세/진행/완료 (HTML detail.js, success).
 * 아래쪽 "미리보기 조작"은 화면 확인용이다. 실제 차량 조작을 판정하지 않으며, 이후 SDK 관찰값으로 대체한다.
 */
class QuestDetailActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestListActivity::class.java
    private val store by lazy { QuestStore(this) }
    private var success = false

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val q = QuestSession.selected()
        val s = store.state(q)
        val cats = QuestData.cats.getValue(QuestSession.type)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#CCFFFFFF"), 36f, Color.WHITE, 3f)
            setPadding(dp(36f), dp(32f), dp(36f), dp(32f)); elevation = dp(4f).toFloat()
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
            actions.addView(button(label) { store.start(q); refresh() })
            actions.addView(button("중단", primary = false, enabled = s.status == "doing") {
                store.stop(q); refresh()
                Toast.makeText(this, "퀘스트를 중단했어요. 완료 기록은 추가하지 않았어요.", Toast.LENGTH_SHORT).show()
            }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
        }
        panel.addView(actions)

        if (!success && s.status == "doing") {
            panel.addView(tv("미리보기 조작 · 실제 차량 조작이 아닙니다", 16f, Color.parseColor("#758298")).apply { setPadding(0, dp(22f), 0, dp(8f)) })
            panel.addView(tv((if (q.tip != null) "동작을 마쳤다고 치기" else q.steps[s.step]) + "　→", 22f, ScreenActivity.NAVY).apply {
                background = rounded(Color.parseColor("#DFEBFA"), 16f); setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
                isClickable = true; isFocusable = true
                setOnClickListener { if (store.advance(q)) goTo(QuestDoneActivity::class.java) else refresh() }
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
        val poseView: android.view.View = if (photo) android.widget.ImageView(this).apply {
            setImageResource(imgId); scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            if (!compact) {
                adjustViewBounds = true; clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: android.view.View, o: android.graphics.Outline) { o.setRoundRect(0, 0, v.width, v.height, dp(30f).toFloat()) }
                }
            }
        } else pose(if (success) 2 else 0)
        if (compact) {
            root.addView(poseView, LinearLayout.LayoutParams(-1, 0, 0.28f))
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 0.72f))
        } else {
            root.addView(poseView, if (photo) LinearLayout.LayoutParams(0, -2, 0.46f) else LinearLayout.LayoutParams(0, -1, 0.40f))
            root.addView(scroll, LinearLayout.LayoutParams(0, -2, 0.60f).apply { leftMargin = dp(24f) })
        }
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }
}
