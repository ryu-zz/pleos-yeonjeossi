package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView

/** 연저씨 · 퀘스트 카테고리와 목록 (HTML list.js). */
class QuestListActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestTypesActivity::class.java
    private val store by lazy { QuestStore(this) }

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val type = QuestSession.type
        val green = type == "daily"
        val cats = QuestData.cats.getValue(type)

        // 카테고리 (동그라미 + 이름)
        val catBar = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        cats.forEachIndexed { i, name ->
            val sel = i == QuestSession.category
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
                isClickable = true; isFocusable = true
                setOnClickListener { QuestSession.category = i; refresh() }
            }
            val selColor = Color.parseColor(if (green) "#418368" else "#315E99")
            item.addView(tv(QuestData.symbols[i], 34f, if (sel) Color.WHITE else Color.parseColor("#7D8BA0")).apply {
                gravity = Gravity.CENTER
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(if (sel) selColor else Color.parseColor("#E6EDF5")); setStroke(dp(3f), Color.WHITE)
                }
                elevation = dp(3f).toFloat()
            }, LinearLayout.LayoutParams(dp(74f), dp(74f)))
            item.addView(tv(name, 17f, if (sel) Color.parseColor("#244A7F") else Color.parseColor("#7D8BA0"), sel).apply { setPadding(0, dp(8f), 0, 0) })
            catBar.addView(item, LinearLayout.LayoutParams(if (compact) dp(104f) else dp(120f), -2))
        }

        // 퀘스트 목록
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(tv(cats[QuestSession.category], 40f, ScreenActivity.NAVY, true), LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(tv(if (green) "일상 퀘스트" else "차량 퀘스트", 20f, Color.parseColor("#76859A")))
        right.addView(titleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18f); topMargin = dp(4f) })
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        QuestData.names(type, QuestSession.category).forEachIndexed { i, _ ->
            list.addView(questCard(i), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16f) })
        }
        right.addView(ScrollView(this).apply { addView(list); isVerticalScrollBarEnabled = false }, LinearLayout.LayoutParams(-1, 0, 1f))

        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            setPadding(dp(36f), 0, dp(36f), dp(24f))
        }
        if (compact) {
            root.addView(HorizontalScrollView(this).apply { addView(catBar); isHorizontalScrollBarEnabled = false },
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10f) })
            root.addView(right, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            root.addView(ScrollView(this).apply { addView(catBar); isVerticalScrollBarEnabled = false },
                LinearLayout.LayoutParams(dp(130f), -1).apply { rightMargin = dp(24f) })
            root.addView(right, LinearLayout.LayoutParams(0, -1, 1f))
        }
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    private fun questCard(i: Int): View {
        val q = QuestData.quest(QuestSession.type, QuestSession.category, i)
        val st = store.state(q).status
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(statusBg(st), 30f, Color.WHITE, 3f)
            setPadding(dp(24f), dp(20f), dp(24f), dp(20f))
            minimumHeight = dp(130f); elevation = dp(2f).toFloat()
            isClickable = true; isFocusable = true
            setOnClickListener { QuestSession.questIndex = i; goTo(QuestDetailActivity::class.java) }
        }
        row.addView(tv(QuestData.symbols[QuestSession.category], 40f, ScreenActivity.NAVY).apply {
            gravity = Gravity.CENTER; background = rounded(Color.parseColor("#99FFFFFF"), 22f)
        }, LinearLayout.LayoutParams(dp(84f), dp(84f)).apply { rightMargin = dp(20f) })
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(tv(q.name, 28f, ScreenActivity.NAVY, true))
        text.addView(tv(q.desc, 18f, ScreenActivity.GRAY).apply { setPadding(0, dp(8f), 0, 0) })
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(tv(statusLabel(st), 18f, statusColor(st), true).apply {
            background = rounded(Color.WHITE, 24f); setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(12f) })
        row.addView(tv("›", 44f, Color.parseColor("#4A5B7A")).apply { setPadding(dp(12f), 0, 0, 0) })
        return row
    }
}
