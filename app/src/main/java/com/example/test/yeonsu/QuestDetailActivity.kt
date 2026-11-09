package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.example.test.R

/**
 * 연저씨 · 퀘스트 상세/진행 화면.
 * 디자인 시안(UI image/운전석 앞뒤 조절 안내 화면.png): 큰 흰 카드 안에 왼쪽 퀘스트 사진, 오른쪽 카테고리·제목·설명·핵심·버튼.
 * 사진(quest_img_카테고리_번호)과 글자는 퀘스트마다 QuestData에서 가져온다.
 * 아래쪽 "미리보기 조작"은 화면 확인용이다. 실제 차량 조작을 판정하지 않으며, 이후 SDK 관찰값으로 대체한다.
 */
class QuestDetailActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestListActivity::class.java
    private val store by lazy { QuestStore(this) }

    /** 출발 준비(퀘스트 목록) 화면과 같은 배경 */
    override fun screenBackground(): android.graphics.drawable.Drawable =
        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.yeonsu_list_bg)!!

    /** 시안 픽셀(1672 폭 기준) → 화면 픽셀 */
    private fun px(v: Float) = dp(v / 1.6f)
    private fun t(text: CharSequence, size: Float, color: Int, bold: Boolean = false) = tv(text, size / 1.6f, color, bold)

    /** 카드 둘레의 부드러운 그림자 폭(시안 px) */
    private val SH = 18f

    /** 시안처럼 살짝 비치는 흰 카드 + 넓게 번지는 옅은 파란 그림자 */
    private fun softCard(): android.graphics.drawable.Drawable {
        val r = px(50f).toFloat(); val sh = px(SH)
        val layers = mutableListOf<android.graphics.drawable.Drawable>()
        val n = 8
        for (i in 0 until n) {
            val a = 2 + 3 * (i + 1) / n   // 바깥쪽은 아주 옅게, 안쪽으로 갈수록 조금 진하게
            layers += GradientDrawable().apply { setColor(Color.argb(a, 70, 110, 200)); cornerRadius = r + sh * (n - i) / n }
        }
        layers += GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#B8FFFFFF"), Color.parseColor("#A6F4F8FF"))).apply {
            cornerRadius = r; setStroke(px(2.5f), Color.parseColor("#B3FFFFFF"))
        }
        return android.graphics.drawable.LayerDrawable(layers.toTypedArray()).apply {
            for (i in 0 until n) {
                val ins = sh * i / n
                setLayerInset(i, ins, ins + sh / 4, ins, (ins - sh / 4).coerceAtLeast(0))
            }
            setLayerInset(n, sh, sh, sh, sh)
        }
    }

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val q = QuestSession.selected()
        val s = store.state(q)
        val cats = QuestData.cats.getValue(QuestSession.type)
        val vehicle = QuestSession.type == "vehicle"

        // ── 큰 카드 ──
        val card = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            background = softCard()
            setPadding(px(51f + SH), px(50f + SH), px(40f + SH), px(50f + SH))
            minimumHeight = px(700f + SH * 2f)
            gravity = Gravity.CENTER_VERTICAL
        }

        // 왼쪽: 퀘스트 사진 (없으면 연저씨 포즈)
        val imgId = resources.getIdentifier("quest_img_${q.c}_${QuestSession.questIndex}", "drawable", packageName)
        // 새 3:2 사진(출발 준비·방향지시등·와이퍼)은 칸을 꽉 채우고, 나머지 옛 사진은 잘리지 않게 맞춘다.
        val fullPhoto = true
        val photo: View = if (vehicle && imgId != 0) ImageView(this).apply {
            setImageResource(imgId)
            if (fullPhoto) scaleType = ImageView.ScaleType.CENTER_CROP
            else { scaleType = ImageView.ScaleType.FIT_CENTER; adjustViewBounds = true }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(v: View, o: android.graphics.Outline) { o.setRoundRect(0, 0, v.width, v.height, px(40f).toFloat()) }
            }
        } else pose(0)
        if (compact) card.addView(photo, LinearLayout.LayoutParams(-1, px(420f)))
        else card.addView(photo, LinearLayout.LayoutParams(px(691f), if (vehicle && fullPhoto) px(527f) else -2))

        // 오른쪽: 글자와 버튼
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val chip = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.parseColor("#DCE6FA"), 40f / 1.6f)
            setPadding(px(24f), px(12f), px(28f), px(12f))
        }
        chip.addView(ImageView(this).apply {
            setImageResource(if (vehicle) R.drawable.ic_y_car else R.drawable.ic_y_quest)
            setColorFilter(Color.parseColor("#1F3F86"))
        }, LinearLayout.LayoutParams(px(52f), px(52f)))
        chip.addView(t(cats[q.c], 30f, Color.parseColor("#1F3F86"), true).apply { setPadding(px(18f), 0, 0, 0) })
        col.addView(chip, LinearLayout.LayoutParams(-2, -2))

        col.addView(t(q.name, 72f, Color.parseColor("#14245A"), true).apply { setPadding(0, px(30f), 0, 0); alignInk(this, px(24f)) })
        col.addView(t(q.desc, 32f, Color.parseColor("#3C4864")).apply { setPadding(0, px(26f), 0, 0); setLineSpacing(0f, 1.25f); alignInk(this, px(24f)) })

        // 핵심 한 줄 (노란 알약). 핵심이 없으면 단계 목록을 보여준다.
        if (q.tip != null) {
            val tip = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(Color.parseColor("#FCEFCB"), 45f / 1.6f)
                setPadding(px(18f), px(22f), px(26f), px(22f))
            }
            tip.addView(t("💡", 40f, Color.BLACK).apply { setPadding(0, 0, px(14f), 0) })
            // 핵심 문구는 항상 한 줄. 길면 글자 크기를 줄여서 한 줄에 맞춘다.
            tip.addView(t("핵심: " + q.tip, 27f, Color.parseColor("#8A4300"), true).apply {
                maxLines = 1
                androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(this,
                    px(16f), px(27f), 1, android.util.TypedValue.COMPLEX_UNIT_PX)
                gravity = Gravity.CENTER_VERTICAL
            },
                LinearLayout.LayoutParams(0, px(40f), 1f))
            col.addView(tip, LinearLayout.LayoutParams(-1, -2).apply { topMargin = px(36f) })
        } else q.steps.forEachIndexed { i, name ->
            val ok = i < s.step
            col.addView(t((if (ok) "✓ " else "○ ") + name, 30f, if (ok) Color.parseColor("#2D7653") else ScreenActivity.NAVY).apply {
                background = rounded(Color.parseColor(if (ok) "#D9EEE1" else "#EAF0F8"), 28f / 1.6f)
                setPadding(px(30f), px(20f), px(30f), px(20f))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = px(if (i == 0) 36f else 12f) })
        }

        // 버튼: [시작/계속하기 ›] [중단]
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val label = when (s.status) { "doing" -> "계속하기"; "done" -> "다시 해보기"; else -> "퀘스트 시작" }
        val primary = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#4BA6F7"), Color.parseColor("#1E5ED3"))).apply { cornerRadius = px(54f).toFloat() }
            elevation = px(6f).toFloat(); isClickable = true; isFocusable = true; contentDescription = label
            setOnClickListener { store.start(q); refresh() }
        }
        primary.addView(t(label, 40f, Color.WHITE, true).apply { gravity = Gravity.CENTER },
            FrameLayout.LayoutParams(-1, -1))
        primary.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_y_chev_right); setColorFilter(Color.WHITE)
        }, FrameLayout.LayoutParams(px(44f), px(44f), Gravity.END or Gravity.CENTER_VERTICAL).apply { rightMargin = px(40f) })
        actions.addView(primary, LinearLayout.LayoutParams(0, px(108f), 1.25f))

        val stopOn = s.status == "doing"
        val stop = t("중단", 40f, Color.parseColor("#14245A"), true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#F7FAFF"), 54f / 1.6f, Color.parseColor("#B9CCEC"), 3f / 1.6f)
            alpha = if (stopOn) 1f else 0.45f
            isClickable = stopOn; isFocusable = stopOn
            if (stopOn) setOnClickListener {
                store.stop(q); refresh()
                Toast.makeText(this@QuestDetailActivity, "퀘스트를 중단했어요. 완료 기록은 추가하지 않았어요.", Toast.LENGTH_SHORT).show()
            }
        }
        actions.addView(stop, LinearLayout.LayoutParams(0, px(108f), 1f).apply { leftMargin = px(26f) })
        col.addView(actions, LinearLayout.LayoutParams(-1, -2).apply { topMargin = px(36f) })

        // 미리보기 조작 (진행 중일 때만)
        if (s.status == "doing") {
            col.addView(t("미리보기 조작 · 실제 차량 조작이 아닙니다", 22f, Color.parseColor("#758298")).apply { setPadding(0, px(22f), 0, px(8f)) })
            col.addView(t((if (q.tip != null) "동작을 마쳤다고 치기" else q.steps[s.step]) + "　→", 28f, ScreenActivity.NAVY).apply {
                background = rounded(Color.parseColor("#DFEBFA"), 24f / 1.6f); setPadding(px(28f), px(16f), px(28f), px(16f))
                isClickable = true; isFocusable = true
                setOnClickListener { if (store.advance(q)) goTo(QuestDoneActivity::class.java) else refresh() }
            }, LinearLayout.LayoutParams(-2, -2))
        }

        val scroll = ScrollView(this).apply { addView(col); isVerticalScrollBarEnabled = false; isFillViewport = false }
        if (compact) card.addView(scroll, LinearLayout.LayoutParams(-1, -2).apply { topMargin = px(30f) })
        else card.addView(scroll, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = px(50f) })

        val outer = ScrollView(this).apply { isVerticalScrollBarEnabled = false; isFillViewport = true }
        val wrap = FrameLayout(this).apply { setPadding(px(48f - SH), px(10f), px(42f - SH), px(30f)) }
        wrap.addView(card, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_VERTICAL))
        outer.addView(wrap)
        content.addView(outer, FrameLayout.LayoutParams(-1, -1))
    }
    /** 글자 첫 획이 정확히 inset 위치(칸 안쪽 아이콘과 같은 세로줄)에서 시작하도록 옮긴다. */
    private fun alignInk(v: android.widget.TextView, inset: Int = 0) {
        val r = android.graphics.Rect()
        val first = v.text.toString().lineSequence().first()
        v.paint.getTextBounds(first, 0, first.length, r)
        if (inset >= r.left) v.setPadding(inset - r.left, v.paddingTop, v.paddingRight, v.paddingBottom) else v.translationX = (inset - r.left).toFloat()
    }
}
