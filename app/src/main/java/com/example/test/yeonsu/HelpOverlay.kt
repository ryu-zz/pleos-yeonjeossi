package com.example.test.yeonsu

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** 도움말에서 가리킬 대상 하나: 화면 위 사각형(rect), 제목, 설명. 글자는 대상 아래에 놓고 화살표로 대상을 가리킨다. */
enum class HelpPlace { OVER_MID, LEFT, RIGHT, ABOVE, BELOW }
/** frac: 글자를 놓을 세로 위치(대상 높이의 비율). */
class HelpTarget(val rect: RectF, val title: String, val body: String, val place: HelpPlace = HelpPlace.OVER_MID, val frac: Float = 0.5f)

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

    private class Arrow(val sx: Float, val sy: Float, val ex: Float, val ey: Float, val bendX: Float, val bendY: Float)
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
            val cx = t.rect.centerX(); val cy = t.rect.top + t.rect.height() * t.frac
            val maxW = if (t.place == HelpPlace.OVER_MID) (t.rect.width() - dp(24f)).toInt() else dp(420f)
            val textW = minOf(if (t.place == HelpPlace.OVER_MID) dp(520f) else dp(380f), maxW)
            val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
            box.addView(tv(t.title, 32f, Color.WHITE, true))
            box.addView(tv(t.body, 20f, Color.WHITE, false).apply { setPadding(0, dp(8f), 0, 0); setLineSpacing(0f, 1.25f) })
            box.measure(MeasureSpec.makeMeasureSpec(textW, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED))
            val bh = box.measuredHeight
            val bx: Float; val by: Float
            val len = dp(70f).toFloat()   // 모든 화살표 길이를 같게
            val gap = dp(6f).toFloat()    // 화살촉과 대상 사이 간격
            val pad = dp(10f).toFloat()   // 화살표 끝과 글자 사이 간격
            when (t.place) {
                HelpPlace.OVER_MID -> {   // 카드 위 중간에 글자, 글자 위에서 위쪽으로 화살표
                    bx = cx - textW / 2f; by = t.rect.top + t.rect.height() * t.frac
                    arrows.add(Arrow(cx, by - pad, cx, by - pad - len, 0f, 0f))
                }
                HelpPlace.LEFT -> {       // 카드 왼쪽에 글자, 오른쪽의 카드를 가리킨다
                    bx = t.rect.left - gap - len - pad - textW; by = cy - bh / 2f
                    arrows.add(Arrow(t.rect.left - gap - len, cy, t.rect.left - gap, cy, 0f, 0f))
                }
                HelpPlace.RIGHT -> {      // 카드 오른쪽에 글자, 왼쪽의 카드를 가리킨다
                    bx = t.rect.right + gap + len + pad; by = cy - bh / 2f
                    arrows.add(Arrow(t.rect.right + gap + len, cy, t.rect.right + gap, cy, 0f, 0f))
                }
                HelpPlace.BELOW -> {      // 카드 아래쪽에 글자, 위의 카드를 가리킨다
                    bx = cx - textW / 2f; by = t.rect.bottom + gap + len + pad
                    arrows.add(Arrow(cx, t.rect.bottom + gap + len, cx, t.rect.bottom + gap, 0f, 0f))
                }
                HelpPlace.ABOVE -> {      // 카드 위쪽에 글자, 아래의 카드를 가리킨다
                    bx = cx - textW / 2f; by = t.rect.top - gap - len - pad - bh
                    arrows.add(Arrow(cx, t.rect.top - gap - len, cx, t.rect.top - gap, 0f, 0f))
                }
            }
            addView(box, LayoutParams(textW, -2, Gravity.TOP or Gravity.START).apply {
                leftMargin = bx.toInt().coerceAtLeast(dp(8f)); topMargin = by.toInt().coerceAtLeast(dp(80f))
            })
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
            val p = Path().apply { moveTo(a.sx, a.sy); quadTo((a.sx + a.ex) / 2f + a.bendX, (a.sy + a.ey) / 2f + a.bendY, a.ex, a.ey) }
            canvas.drawPath(p, line)
            // 화살촉: 끝점에서 휜 방향의 반대로 두 갈래
            val cxp = (a.sx + a.ex) / 2f + a.bendX; val cyp = (a.sy + a.ey) / 2f + a.bendY
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
