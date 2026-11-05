package com.example.test.yeonsu

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** 도움말에서 강조할 영역 하나: 화면 위 사각형(rect), 제목, 설명. */
class HelpTarget(val rect: RectF, val title: String, val body: String)

/**
 * 도움말 화면. 화면 전체를 어둡게 덮고, 설명할 영역만 밝게 뚫어 흰 테두리로 강조한다.
 * 각 영역 안쪽 아래에 흰 설명 상자를 놓는다. 화면 아무 곳이나 누르면 닫힌다.
 * k는 화면 배율(각 화면의 dp 환산 값).
 */
class HelpOverlay(
    private val ctx: Context, private val k: Float, headline: String, hint: String,
    private val targets: List<HelpTarget>, private val onClose: () -> Unit,
) : FrameLayout(ctx) {
    private val dim = Paint().apply { color = Color.parseColor("#B20F1B3D") }
    private val clear = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = Color.WHITE; strokeWidth = 3f * k * 1.3f }
    private fun dp(v: Float) = (v * k).toInt()
    private fun tv(text: String, px: Float, color: Int, bold: Boolean) = TextView(ctx).apply {
        this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_PX, px * k); setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
    }

    init {
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        isClickable = true; isFocusable = true
        setOnClickListener { close() }
        // 위쪽 안내
        val head = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        head.addView(tv(headline, 34f, Color.WHITE, true))
        head.addView(tv(hint, 19f, Color.parseColor("#D5DDF2"), false).apply { setPadding(0, dp(8f), 0, 0) })
        addView(head, LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(26f) })
        // 영역별 설명 상자
        targets.forEach { t ->
            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = GradientDrawable().apply { setColor(Color.parseColor("#F5FFFFFF")); cornerRadius = dp(22f).toFloat() }
                setPadding(dp(18f), dp(14f), dp(18f), dp(14f)); elevation = dp(6f).toFloat()
            }
            box.addView(tv(t.title, 26f, Color.parseColor("#0F1B3D"), true))
            box.addView(tv(t.body, 17f, Color.parseColor("#3A4766"), false).apply { setPadding(0, dp(6f), 0, 0); setLineSpacing(0f, 1.2f) })
            val inset = dp(14f)
            addView(box, LayoutParams((t.rect.width() - 2 * inset).toInt(), -2, Gravity.TOP or Gravity.START).apply {
                leftMargin = (t.rect.left + inset).toInt()
                // 영역 안쪽 아래에 놓는다(높이를 모르므로 아래 기준 margin 대신 post에서 위치 보정)
                topMargin = (t.rect.bottom - inset).toInt()
            })
            box.post { (box.layoutParams as LayoutParams).topMargin = (t.rect.bottom - inset - box.height).toInt(); box.requestLayout() }
        }
        alpha = 0f
        animate().alpha(1f).setDuration(220).start()
    }

    override fun onDraw(canvas: Canvas) {
        val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dim)
        val r = dp(34f).toFloat()
        targets.forEach { canvas.drawRoundRect(it.rect, r, r, clear) }
        canvas.restoreToCount(sc)
        targets.forEach { canvas.drawRoundRect(it.rect, r, r, stroke) }
    }

    fun close() {
        animate().alpha(0f).setDuration(180).withEndAction { (parent as? FrameLayout)?.removeView(this); onClose() }.start()
    }
}
