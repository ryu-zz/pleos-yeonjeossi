package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout

/** 연저씨 · 퀘스트 종류 선택 (HTML types.js). 차량 퀘스트 / 일상 퀘스트. */
class QuestTypesActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = HomeActivity::class.java

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val guide = LinearLayout(this).apply { orientation = if (compact) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        val bubble = bubble("자네, 퀘스트에", "도전해보겠나?", if (compact) 30f else 40f)
        val pose = pose(2)
        if (compact) {
            guide.addView(pose, LinearLayout.LayoutParams(0, -1, 0.9f))
            guide.addView(bubble, LinearLayout.LayoutParams(0, -2, 1.2f))
        } else {
            guide.addView(bubble, LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.START; leftMargin = dp(20f) })
            guide.addView(pose, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(12f) })
        }

        val cards = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        cards.addView(typeCard("차량 퀘스트", "차 기본 조작을 까먹었다고? 차근차근 따라해보자!", "#E5F2FF", "#CBDFF7", "vehicle"),
            LinearLayout.LayoutParams(-1, -2))
        cards.addView(typeCard("일상 퀘스트", "연저씨가 옆에서 하나하나 다 알려줄게", "#EFFBF2", "#D2ECDA", "daily"),
            LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(24f) })

        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            setPadding(dp(36f), 0, dp(36f), dp(28f))
        }
        if (compact) {
            root.addView(guide, LinearLayout.LayoutParams(-1, 0, 0.42f))
            root.addView(cards, LinearLayout.LayoutParams(-1, 0, 0.58f))
        } else {
            root.addView(guide, LinearLayout.LayoutParams(0, -1, 0.43f))
            root.addView(cards, LinearLayout.LayoutParams(0, -1, 0.57f).apply { leftMargin = dp(40f) })
        }
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    private fun typeCard(title: String, desc: String, c1: String, c2: String, type: String): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = gradientBox(c1, c2, 34f)
            setPadding(dp(30f), dp(34f), dp(30f), dp(34f))
            elevation = dp(4f).toFloat(); isClickable = true; isFocusable = true
            setOnClickListener {
                QuestSession.type = type; QuestSession.category = 0
                goTo(QuestListActivity::class.java)
            }
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv(title, 36f, ScreenActivity.NAVY, true))
        col.addView(tv(desc, 20f, ScreenActivity.GRAY).apply { setPadding(0, dp(10f), 0, 0) })
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(tv("›", 52f, Color.parseColor("#4A5B7A")))
        return row
    }
}
