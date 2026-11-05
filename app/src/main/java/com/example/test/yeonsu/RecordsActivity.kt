package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import com.example.test.R
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

    private var statsView: View? = null
    private var badgesView: View? = null
    private var recordsView: View? = null
    override fun helpHeadline() = "마이페이지 화면이에요"
    override fun helpItems(): List<HelpItem>? {
        val a = statsView ?: return null; val b = badgesView ?: return null; val r = recordsView ?: return null
        return listOf(
            HelpItem(a, "완료한 퀘스트 / 진행 중", "깬 퀘스트와 하고 있는 퀘스트의\n개수를 보여줘요", HelpPlace.OVER_MID, 0.5f, arrow = false),
            HelpItem(b, "나의 뱃지", "카테고리를 모두 깨면 뱃지를 받아요\n받은 뱃지는 여기에 모여요", HelpPlace.OVER_MID, 0.5f, arrow = false),
            HelpItem(r, "최근 완료 기록", "최근에 깬 퀘스트가\n시간 순서대로 쌓여요", HelpPlace.OVER_MID, 0.3f, arrow = false),
        )
    }

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(tv("마이페이지", 46f, ScreenActivity.NAVY, true))
        col.addView(tv("차근차근 쌓이는 나의 작은 성공", 22f, ScreenActivity.GRAY).apply { setPadding(0, dp(10f), 0, dp(20f)) })

        val stats = LinearLayout(this)
        stats.addView(statBox("완료한 퀘스트", store.count("done")), LinearLayout.LayoutParams(0, -2, 1f))
        stats.addView(statBox("진행 중", store.count("doing")), LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(16f) })
        col.addView(stats, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(26f) })
        statsView = stats

        // 나의 뱃지: 차량/일상 카테고리마다 한 칸. 받았으면 금색, 아직이면 흐리게
        val badgeBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val earnedN = listOf("vehicle", "daily").sumOf { t -> QuestData.cats.getValue(t).indices.count { store.hasBadge(t, it) } }
        val totalN = QuestData.cats.values.sumOf { it.size }
        badgeBox.addView(LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            addView(tv("나의 뱃지", 30f, ScreenActivity.NAVY, true), LinearLayout.LayoutParams(0, -2, 1f))
            addView(tv("$earnedN / $totalN", 20f, Color.parseColor("#76859A")))
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12f) })
        listOf("vehicle" to "차량 퀘스트", "daily" to "일상 퀘스트").forEach { (t, label) ->
            badgeBox.addView(tv(label, 18f, Color.parseColor("#76859A")).apply { setPadding(0, dp(6f), 0, dp(6f)) })
            val names = QuestData.cats.getValue(t)
            val perRow = if (compact) 3 else 5
            names.indices.chunked(perRow).forEach { rowIdx ->
                val row = LinearLayout(this)
                rowIdx.forEach { i ->
                    val got = store.hasBadge(t, i)
                    val cell = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(0, dp(6f), 0, dp(6f)) }
                    cell.addView(ImageView(this).apply {
                        setImageResource(R.drawable.ic_badge); alpha = if (got) 1f else 0.22f
                        if (!got) colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix().apply { setSaturation(0f) })
                    }, LinearLayout.LayoutParams(dp(64f), dp(64f)))
                    cell.addView(tv(names[i], 15f, if (got) ScreenActivity.NAVY else Color.parseColor("#9AA6B8"), got).apply { maxLines = 1; setPadding(0, dp(4f), 0, 0) })
                    row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f))
                }
                repeat(perRow - rowIdx.size) { row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
                badgeBox.addView(row, LinearLayout.LayoutParams(-1, -2))
            }
        }
        col.addView(badgeBox, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(26f) })
        badgesView = badgeBox

        val recordsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        recordsView = recordsBox
        col.addView(recordsBox, LinearLayout.LayoutParams(-1, -2))

        val title = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        title.addView(tv("최근 완료 기록", 30f, ScreenActivity.NAVY, true), LinearLayout.LayoutParams(0, -2, 1f))
        title.addView(tv("미리보기 기록", 18f, Color.parseColor("#76859A")))
        recordsBox.addView(title, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14f) })

        val records = store.records()
        if (records.isEmpty()) {
            val empty = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                background = rounded(Color.parseColor("#99FFFFFF"), 28f); setPadding(dp(30f), dp(44f), dp(30f), dp(44f))
            }
            empty.addView(tv("아직 완료한 퀘스트가 없어요.", 26f, ScreenActivity.NAVY, true))
            empty.addView(tv("퀘스트를 하나 시작해 볼까요?", 21f, ScreenActivity.GRAY).apply { setPadding(0, dp(10f), 0, dp(24f)) })
            empty.addView(button("퀘스트 고르기", primary = false) { goTo(QuestTypesActivity::class.java) })
            recordsBox.addView(empty, LinearLayout.LayoutParams(-1, -2))
        } else {
            val fmt = SimpleDateFormat("yyyy. M. d. a h:mm", Locale.KOREA)
            records.forEach { r ->
                val row = LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    background = rounded(Color.parseColor("#B0FFFFFF"), 24f); setPadding(dp(26f), dp(22f), dp(26f), dp(22f))
                    orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
                }
                row.addView(tv("✓ " + r.name, 24f, ScreenActivity.NAVY, true), if (compact) LinearLayout.LayoutParams(-2, -2) else LinearLayout.LayoutParams(0, -2, 1f))
                row.addView(tv(fmt.format(Date(r.at)), 18f, Color.parseColor("#758298")).apply { if (compact) setPadding(0, dp(6f), 0, 0) })
                recordsBox.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12f) })
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
