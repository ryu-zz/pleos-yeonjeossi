package com.example.test.yeonsu

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** 도움말에서 가리킬 대상 하나: 화면 위 사각형(rect), 제목, 설명. 글자는 대상 아래에 놓고 화살표로 대상을 가리킨다. */
class HelpTarget(val rect: RectF, val title: String, val body: String)

/**
 * 도움말 화면. 화면 전체를 어둡게 덮고, 흰 글씨 설명과 흰 화살표로 대상을 가리킨다.
 * 화면 아무 곳이나 누르면 닫힌다. k는 화면 배율(각 화면의 dp 환산 값).
 */
class HelpOverlay(
    private val ctx: Context, private val k: Float, headline: String, hint: String,
    private val targets: List<HelpTarget>, private val onClose: () -> Unit,
) : FrameLayout(ctx) {
    private val dim = Paint().apply { color = Color.parseColor("#D00F1B3D") }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Color.WHITE; strokeWidth = 3.6f * k
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private fun dp(v: Float) = (v * k).toInt()
    private fun tv(text: String, px: Float, color: Int, bold: Boolean) = TextView(ctx).apply {
        this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_PX, px * k); setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false; gravity = Gravity.CENTER_HORIZONTAL
    }

    private class Arrow(val sx: Float, val sy: Float, val ex: Float, val ey: Float, val bend: Float)
    private val arrows = ArrayList<Arrow>()

    init {
        setWillNotDraw(false)
        isClickable = true; isFocusable = true
        setOnClickListener { close() }
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        head.addView(tv(headline, 36f, Color.WHITE, true))
        head.addView(tv(hint, 19f, Color.parseColor("#C9D3EC"), false).apply { setPadding(0, dp(8f), 0, 0) })
        addView(head, LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(24f) })

        targets.forEachIndexed { i, t ->
            val cx = t.rect.centerX()
            val textW = minOf(dp(380f), (t.rect.width() - dp(24f)).toInt())
            val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            box.addView(tv(t.title, 32f, Color.WHITE, true))
            box.addView(tv(t.body, 20f, Color.WHITE, false).apply { setPadding(0, dp(8f), 0, 0); setLineSpacing(0f, 1.25f) })
            // 카드 위, 중간쯤에 글자를 놓고, 글자 위에서 카드 제목 쪽으로 화살표를 올린다
            val textTop = t.rect.top + t.rect.height() * 0.50f
            addView(box, LayoutParams(textW, -2, Gravity.TOP or Gravity.START).apply {
                leftMargin = (cx - textW / 2f).toInt(); topMargin = textTop.toInt()
            })
            val bend = if (i % 2 == 0) -dp(40f).toFloat() else dp(40f).toFloat()
            arrows.add(Arrow(cx + bend * 0.3f, textTop - dp(14f), cx, t.rect.top + t.rect.height() * 0.30f, bend))
        }
        alpha = 0f
        animate().alpha(1f).setDuration(220).start()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        // 오른쪽 화면 밖으로 나간 글자 상자를 안쪽으로 당긴다
        for (j in 0 until childCount) {
            val c = getChildAt(j)
            if (j == 0) continue
            val lp = c.layoutParams as LayoutParams
            val over = lp.leftMargin + lp.width - (w - dp(8f))
            if (over > 0) lp.leftMargin -= over
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dim)
        arrows.forEach { a ->
            val p = Path().apply { moveTo(a.sx, a.sy); quadTo((a.sx + a.ex) / 2f + a.bend, (a.sy + a.ey) / 2f, a.ex, a.ey) }
            canvas.drawPath(p, line)
            // 화살촉: 끝점에서 휜 방향의 반대로 두 갈래
            val cxp = (a.sx + a.ex) / 2f + a.bend; val cyp = (a.sy + a.ey) / 2f
            val ang = Math.atan2((a.ey - cyp).toDouble(), (a.ex - cxp).toDouble())
            val len = dp(18f).toFloat(); val spread = Math.toRadians(28.0)
            for (s in listOf(-1, 1)) {
                val t = ang + Math.PI + s * spread
                canvas.drawLine(a.ex, a.ey, a.ex + (len * Math.cos(t)).toFloat(), a.ey + (len * Math.sin(t)).toFloat(), line)
            }
        }
    }

    fun close() {
        animate().alpha(0f).setDuration(180).withEndAction { (parent as? FrameLayout)?.removeView(this); onClose() }.start()
    }
}
