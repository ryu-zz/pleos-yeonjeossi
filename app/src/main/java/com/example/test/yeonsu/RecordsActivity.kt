package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 연저씨 · 마이페이지 (HTML records.js). 완료/진행 개수와 최근 완료 기록. */
class RecordsActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = HomeActivity::class.java
    private val store by lazy { QuestStore(this) }

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv("마이페이지", 46f, ScreenActivity.NAVY, true))
        col.addView(tv("차근차근 쌓이는 나의 작은 성공", 22f, ScreenActivity.GRAY).apply { setPadding(0, dp(10f), 0, dp(20f)) })

        val stats = LinearLayout(this)
        stats.addView(statBox("완료한 퀘스트", store.count("done")), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(statBox("진행 중", store.count("doing")), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(16f) })
        col.addView(stats, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(26f) })

        val title = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        title.addView(tv("최근 완료 기록", 30f, ScreenActivity.NAVY, true), LinearLayout.LayoutParams(0, -2, 1f))
        title.addView(tv("미리보기 기록", 18f, Color.parseColor("#76859A")))
        col.addView(title, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14f) })

        val records = store.records()
        if (records.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = rounded(Color.parseColor("#99FFFFFF"), 28f); setPadding(dp(30f), dp(44f), dp(30f), dp(44f))
            }
            empty.addView(tv("아직 완료한 퀘스트가 없어요.", 26f, ScreenActivity.NAVY, true))
            empty.addView(tv("퀘스트를 하나 시작해 볼까요?", 21f, ScreenActivity.GRAY).apply { setPadding(0, dp(10f), 0, dp(24f)) })
            empty.addView(button("퀘스트 고르기", primary = false) { goTo(QuestTypesActivity::class.java) })
            col.addView(empty, LinearLayout.LayoutParams(-1, -2))
        } else {
            val fmt = SimpleDateFormat("yyyy. M. d. a h:mm", Locale.KOREA)
            records.forEach { r ->
                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(Color.parseColor("#B0FFFFFF"), 24f); setPadding(dp(26f), dp(22f), dp(26f), dp(22f))
                    orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                }
                val nameCol = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                nameCol.addView(tv("✓ " + r.name, 24f, ScreenActivity.NAVY, true))
                if (r.verified) nameCol.addView(tv("차량 신호로 확인됨", 16f, Color.parseColor("#2D7653")).apply {
                    background = rounded(Color.parseColor("#D9EEE1"), 14f); setPadding(dp(12f), dp(4f), dp(12f), dp(4f))
                    (layoutParams as? LinearLayout.LayoutParams)?.topMargin = dp(6f)
                }.also { it.layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(6f) } })
                row.addView(nameCol, if (compact) LinearLayout.LayoutParams(-2, -2) else LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(tv(fmt.format(Date(r.at)), 18f, Color.parseColor("#758298")).apply { if (compact) setPadding(0, dp(6f), 0, 0) })
                col.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12f) })
            }
        }
        col.addView(tv("이 기기에 저장된 화면 확인용 기록입니다. 차량 신호로 판정한 기록이 아닙니다.", 16f, Color.parseColor("#758298")).apply { setPadding(0, dp(18f), 0, 0) })

        content.addView(ScrollView(this).apply { addView(col); isVerticalScrollBarEnabled = false },
            FrameLayout.LayoutParams(-1, -1).apply { setMargins(dp(40f), 0, dp(40f), dp(24f)) })
    }

    private fun statBox(label: String, n: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(Color.parseColor("#BBFFFFFF"), 28f, Color.WHITE, 3f); setPadding(dp(28f), dp(24f), dp(28f), dp(24f))
        addView(tv(label, 20f, ScreenActivity.GRAY))
        addView(tv("${n}개", 48f, ScreenActivity.NAVY, true).apply { setPadding(0, dp(10f), 0, 0) })
    }
}
