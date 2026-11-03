package com.example.test.yeonsu

import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R

/** 연저씨 · 메인 화면. 카드 3장: 마이페이지 / 일반 주행 / 퀘스트 (좌우 화살표로 넘김). */
class HomeActivity : AppCompatActivity() {

    private class Item(val title: String, val desc: String, val icon: Int, val photo: Int, val bg: String)

    private val items = listOf(
        Item("마이페이지", "나의 연습 현황과\n기록을 확인해요", R.drawable.ic_y_person, R.drawable.home_card_my, "#DEE7FC"),
        Item("일반 주행", "자유롭게 연습 주행을 시작해요", R.drawable.ic_y_wheel, R.drawable.home_card_drive, "#FCF6F6"),
        Item("퀘스트", "다양한 미션으로\n실력을 키워요", R.drawable.ic_y_quest, R.drawable.home_card_quest, "#E5F0F1"),
    )
    private val cards = mutableListOf<View>()
    private val dots = mutableListOf<View>()
    private var selected = 1
    private var screenW = 0f
    private lateinit var prev: View
    private lateinit var next: View

    private var k = 1f
    private fun dp(v: Float) = (v * k).toInt()
    private fun tv(text: String, sp: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_PX, sp * k); setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply { background = StartActivity.skyBackground() }
        setContentView(root)
        root.post { build(root, root.width, root.height) }
    }

    private fun build(root: FrameLayout, w: Int, h: Int) {
        screenW = w.toFloat()
        k = w / 1672f * 1.6f
        val navy = Color.parseColor("#0F1B3D")

        // 헤더: 로고 / 홈·설정·도움말
        val logo = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        logo.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_car); alpha = 0.85f }, LinearLayout.LayoutParams(dp(36f), dp(36f)))
        logo.addView(tv("연저씨", 30f, Color.parseColor("#14204A"), true).apply { setPadding(dp(12f), 0, 0, 0) })
        root.addView(logo, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply { topMargin = dp(24f); leftMargin = dp(36f) })

        val tools = LinearLayout(this)
        listOf(R.drawable.ic_y_home to "홈", R.drawable.ic_y_settings to "설정", R.drawable.ic_y_help to "도움말").forEach { (ic, name) ->
            tools.addView(roundButton(ic, 56f) { soon(name) }, LinearLayout.LayoutParams(dp(56f), dp(56f)).apply { leftMargin = dp(12f) })
        }
        root.addView(tools, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { topMargin = dp(20f); rightMargin = dp(32f) })

        // 카드
        val cardW = (w * 0.40f).toInt()
        val cardH = (h * 0.70f).toInt()
        items.forEachIndexed { i, it ->
            val c = buildCard(it, cardW, cardH)
            c.setOnClickListener { _ -> if (i == selected) soon(it.title) else select(i) }
            root.addView(c, FrameLayout.LayoutParams(cardW, cardH, Gravity.CENTER).apply { topMargin = dp(8f) })
            cards.add(c)
        }

        // 화살표
        prev = roundButton(R.drawable.ic_y_chev_left, 56f) { select(selected - 1) }
        next = roundButton(R.drawable.ic_y_chev_right, 56f) { select(selected + 1) }
        root.addView(prev, FrameLayout.LayoutParams(dp(56f), dp(56f), Gravity.CENTER_VERTICAL or Gravity.START).apply { leftMargin = dp(24f); topMargin = dp(8f) })
        root.addView(next, FrameLayout.LayoutParams(dp(56f), dp(56f), Gravity.CENTER_VERTICAL or Gravity.END).apply { rightMargin = dp(24f); topMargin = dp(8f) })

        // 점 표시
        val dotRow = LinearLayout(this)
        repeat(3) {
            val d = View(this)
            dots.add(d)
            dotRow.addView(d, LinearLayout.LayoutParams(dp(14f), dp(14f)).apply { setMargins(dp(8f), 0, dp(8f), 0) })
        }
        root.addView(dotRow, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(18f) })

        layoutCards(false)
    }

    private fun roundButton(icon: Int, size: Float, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(icon)
        val p = dp(size * 0.26f); setPadding(p, p, p, p)
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.WHITE) }
        if (size > 0) elevation = dp(4f).toFloat()
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }.also { v ->
        // 홈/설정/도움말은 사각 라운드, 화살표는 원형: 크기 56dp 이상은 모두 둥근 사각형으로 통일
        (v.background as GradientDrawable).apply { shape = GradientDrawable.RECTANGLE; cornerRadius = dp(16f).toFloat() }
    }

    private fun buildCard(item: Item, w: Int, h: Int): View {
        val card = FrameLayout(this).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor(item.bg)); cornerRadius = dp(34f).toFloat(); setStroke(dp(3f), Color.WHITE) }
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(v: View, o: Outline) = o.setRoundRect(0, 0, v.width, v.height, dp(34f).toFloat())
            }
            isClickable = true
        }
        val photoBox = FrameLayout(this)
        photoBox.addView(ImageView(this).apply {
            setImageResource(item.photo); scaleType = ImageView.ScaleType.FIT_XY; adjustViewBounds = true
        }, FrameLayout.LayoutParams(-1, -2))
        val base = Color.parseColor(item.bg)
        photoBox.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(base, base and 0x00FFFFFF))
        }, FrameLayout.LayoutParams(-1, dp(70f), Gravity.TOP))
        card.addView(photoBox, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
        head.addView(ImageView(this).apply { setImageResource(item.icon) }, LinearLayout.LayoutParams(dp(40f), dp(40f)).apply { topMargin = dp(2f) })
        val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12f), 0, 0, 0) }
        tx.addView(tv(item.title, 30f, Color.parseColor("#0F1B3D"), true))
        tx.addView(tv(item.desc, 17f, Color.parseColor("#3A4766"), false).apply { setPadding(0, dp(8f), 0, 0); setLineSpacing(0f, 1.2f) })
        head.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_chev_right) }, LinearLayout.LayoutParams(dp(30f), dp(30f)).apply { topMargin = dp(6f) })
        card.addView(head, FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { setMargins(dp(20f), dp(20f), dp(14f), 0) })
        return card
    }

    private fun select(i: Int) {
        if (i !in items.indices || i == selected) return
        selected = i
        layoutCards(true)
    }

    private fun layoutCards(animate: Boolean) {
        cards.forEachIndexed { i, c ->
            val off = i - selected
            val tx = off * screenW * 0.345f
            val s = if (off == 0) 1f else 0.82f
            c.elevation = if (off == 0) dp(18f).toFloat() else dp(4f).toFloat()
            c.translationZ = if (off == 0) 1f else 0f
            if (off == 0) c.bringToFront()
            if (animate) c.animate().translationX(tx).scaleX(s).scaleY(s).setDuration(260).start()
            else { c.translationX = tx; c.scaleX = s; c.scaleY = s }
        }
        // bringToFront 후 화살표가 카드에 가려지지 않게
        prev.bringToFront(); next.bringToFront()
        prev.alpha = if (selected == 0) 0.35f else 1f
        next.alpha = if (selected == items.lastIndex) 0.35f else 1f
        dots.forEachIndexed { i, d ->
            d.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == selected) Color.parseColor("#14204A") else Color.parseColor("#9DB0E0"))
            }
        }
    }

    private fun soon(name: String) =
        Toast.makeText(this, "$name 화면은 준비 중이에요", Toast.LENGTH_SHORT).show()
}
