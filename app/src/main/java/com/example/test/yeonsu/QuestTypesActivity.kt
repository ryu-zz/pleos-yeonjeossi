package com.example.test.yeonsu
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import com.example.test.R
/**
 * 연저씨 · 퀘스트 종류 선택.
 * 디자인 시안(연저씨퀘스트안내_날카로운연저씨.png)을 화면에 꽉 채워 띄우고,
 * 그림 속 버튼 위치마다 투명한 터치 영역을 얹는다.
 */
class QuestTypesActivity : FadeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val img = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_quest_types)
            scaleType = ImageView.ScaleType.MATRIX
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#EAF1FD"))
            addView(img, FrameLayout.LayoutParams(-1, -1))
        }
        val spots = listOf(
            Triple(RectF(702f, 318f, 1483f, 582f), "차량 퀘스트") { open("vehicle") },
            Triple(RectF(702f, 634f, 1483f, 898f), "일상 퀘스트") { open("daily") },
        ).map { (rect, name, action) ->
            val v = View(this).apply {
                isClickable = true; isFocusable = true; contentDescription = name
                background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
                setOnClickListener { action() }
                setOnTouchListener { view, e ->
                    val bg = view.background as GradientDrawable
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> bg.setColor(Color.parseColor("#22000000"))
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> bg.setColor(Color.TRANSPARENT)
                    }
                    false
                }
            }
            root.addView(v, FrameLayout.LayoutParams(0, 0))
            rect to v
        }
        setContentView(root)
        // 위쪽 바(뒤로·연저씨·홈·설정·도움말)는 다른 화면(ScreenActivity)과 똑같이 코드로 그린다
        root.post { root.addView(header(root.width), FrameLayout.LayoutParams(-1, (96f * root.width / 1672f * 1.6f).toInt(), android.view.Gravity.TOP)) }
        img.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val d = img.drawable ?: return@addOnLayoutChangeListener
            if (img.width == 0 || img.height == 0) return@addOnLayoutChangeListener
            // 화면을 빈틈없이 채우는 배율(넘치는 부분은 가운데 기준으로 잘림)
            val iw = d.intrinsicWidth.toFloat(); val ih = d.intrinsicHeight.toFloat()
            val s = maxOf(img.width / iw, img.height / ih)
            val m = Matrix().apply { setScale(s, s); postTranslate((img.width - iw * s) / 2f, (img.height - ih * s) / 2f) }
            img.imageMatrix = m
            val sx = iw / SRC_W; val sy = ih / SRC_H
            spots.forEach { (src, v) ->
                val r = RectF(src.left * sx, src.top * sy, src.right * sx, src.bottom * sy)
                m.mapRect(r)
                val lp = v.layoutParams as FrameLayout.LayoutParams
                val nl = r.left.toInt(); val nt = r.top.toInt(); val nw = r.width().toInt(); val nh = r.height().toInt()
                if (lp.leftMargin != nl || lp.topMargin != nt || lp.width != nw || lp.height != nh) {
                    lp.leftMargin = nl; lp.topMargin = nt; lp.width = nw; lp.height = nh
                    (v.background as GradientDrawable).cornerRadius = minOf(nw, nh) * 0.2f
                    v.post { v.layoutParams = lp }
                }
            }
        }
    }
    private fun open(type: String) {
        QuestSession.type = type; QuestSession.category = 0; QuestListActivity.catScroll = 0
        goTo(QuestListActivity::class.java)
    }
    private fun header(w: Int): View {
        val k = w / 1672f * 1.6f
        fun dp(v: Float) = (v * k).toInt()
        fun iconBtn(icon: Int, onClick: () -> Unit) = ImageView(this).apply {
            setImageResource(icon)
            val p = dp(56f * 0.26f); setPadding(p, p, p, p)
            background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(16f).toFloat() }
            elevation = dp(4f).toFloat(); isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        val row = android.widget.LinearLayout(this).apply {
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(36f), dp(8f), dp(32f), 0)
        }
        row.addView(iconBtn(R.drawable.ic_y_chev_left) { goTo(HomeActivity::class.java) },
            android.widget.LinearLayout.LayoutParams(dp(56f), dp(56f)).apply { rightMargin = dp(14f) })
        row.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_wheel) }, android.widget.LinearLayout.LayoutParams(dp(40f), dp(40f)))
        row.addView(android.widget.TextView(this).apply {
            text = "연저씨"; setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 30f * k); setTextColor(ScreenActivity.NAVY)
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            includeFontPadding = false; setPadding(dp(12f), 0, 0, 0)
        }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
        listOf(R.drawable.ic_y_home to "홈", R.drawable.ic_y_settings to "설정", R.drawable.ic_y_help to "도움말").forEach { (ic, name) ->
            row.addView(iconBtn(ic) { if (name == "홈") goTo(HomeActivity::class.java) else soon(name) },
                android.widget.LinearLayout.LayoutParams(dp(56f), dp(56f)).apply { leftMargin = dp(12f) })
        }
        return row
    }
    private fun soon(name: String) = Toast.makeText(this, "$name 화면은 준비 중이에요", Toast.LENGTH_SHORT).show()
    companion object {
        /** 터치 영역 좌표의 기준 크기 (yeonsu_quest_types.png 원본 픽셀) */
        private const val SRC_W = 1536f
        private const val SRC_H = 1110f
    }
}
