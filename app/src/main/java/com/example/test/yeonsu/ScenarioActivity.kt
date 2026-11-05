package com.example.test.yeonsu

import android.app.Activity
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Outline
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast

/**
 * 일상 퀘스트 시나리오 보기. 네 컷을 한 컷씩 넘겨 본다.
 * 마지막 컷에서 "끝"을 누르면, 처음 보는 중이면 "안내할까요?"로, 다시 보는 중이면 퀘스트 완료로 넘어간다.
 */
class ScenarioActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestListActivity::class.java
    private val store by lazy { QuestStore(this) }
    private var cut = 0

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val q = QuestSession.selected()
        val cat = QuestSession.category
        val replay = QuestFlow.stage == QuestFlow.REPLAY
        val imgId = resources.getIdentifier("scn_${cat}_$cut", "drawable", packageName)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(36f), 0, dp(36f), dp(24f))
        }
        // 제목 줄: 퀘스트 이름 + 컷 번호
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(tv(q.name, 30f, ScreenActivity.NAVY, true), LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(tv((if (replay) "다시 보기 " else "시나리오 ") + "${cut + 1}/4", 22f, Color.parseColor("#2E66AA"), true).apply {
            background = rounded(Color.parseColor("#DCEAFA"), 24f); setPadding(dp(18f), dp(8f), dp(18f), dp(8f))
        })
        root.addView(titleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14f) })

        // 사진: 원래 비율 그대로, 남는 공간에 맞춰 크게
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, imgId, o)
        val aspect = o.outWidth.toFloat() / o.outHeight.coerceAtLeast(1)
        val maxW = w - dp(72f)
        val maxH = h - dp(24f) - dp(60f) - dp(14f) - dp(118f) - dp(14f)
        var iw = minOf(maxW, (maxH * aspect).toInt())
        var ih = (iw / aspect).toInt()
        if (ih > maxH) { ih = maxH; iw = (ih * aspect).toInt() }
        root.addView(ImageView(this).apply {
            setImageResource(imgId); scaleType = ImageView.ScaleType.FIT_XY
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, out: Outline) { out.setRoundRect(0, 0, v.width, v.height, dp(28f).toFloat()) }
            }
            elevation = dp(4f).toFloat()
        }, LinearLayout.LayoutParams(iw, ih))

        // 이전 / 다음
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        actions.addView(button("‹ 이전", primary = false, enabled = cut > 0) { cut--; refresh() })
        val last = cut == 3
        actions.addView(button(if (last) "끝" else "다음 ›") {
            if (!last) { cut++; refresh() } else finishScenario(q)
        }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(20f) })
        root.addView(actions, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(14f) })
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    private fun finishScenario(q: Quest) {
        if (QuestFlow.stage == QuestFlow.REPLAY) {
            store.complete(q)
            Toast.makeText(this, "퀘스트를 완료했어요. 마이페이지에 기록됐어요.", Toast.LENGTH_SHORT).show()
            goTo(QuestListActivity::class.java)
        } else {
            QuestFlow.stage = QuestFlow.ASK
            goTo(DailyFlowActivity::class.java)
        }
    }
}
