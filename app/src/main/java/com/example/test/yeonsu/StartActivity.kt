package com.example.test.yeonsu
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.test.R
/**
 * 연저씨 · 시작 화면.
 * 디자인 시안(UI image/현대 운전 연수 시작하기 이미지.png)을 그대로 화면에 띄우고,
 * 그림 속 "시작하기" 버튼 위치에 투명한 터치 영역을 얹는다.
 */
class StartActivity : FadeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val img = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_start_screen)
            scaleType = ImageView.ScaleType.MATRIX   // 화면을 빈틈없이 채우고 아래쪽에 맞춘다
        }
        val hotspot = View(this).apply {
            isClickable = true; isFocusable = true
            contentDescription = "시작하기"
            setOnClickListener { goTo(HomeActivity::class.java) }
            // 누르는 동안 버튼 위에 살짝 어두운 표시
            background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
            setOnTouchListener { v, e ->
                val bg = v.background as GradientDrawable
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> bg.setColor(Color.parseColor("#22000000"))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> bg.setColor(Color.TRANSPARENT)
                }
                false
            }
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F3F6FD"))
            addView(img, FrameLayout.LayoutParams(-1, -1))
            addView(hotspot, FrameLayout.LayoutParams(0, 0))
        }
        setContentView(root)
        // 이미지가 실제로 그려진 위치를 구해 버튼 영역을 맞춘다
        img.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val d = img.drawable ?: return@addOnLayoutChangeListener
            if (img.width == 0 || img.height == 0) return@addOnLayoutChangeListener
            // 화면을 꽉 채우는 배율(남는 부분은 잘림). 가로는 가운데, 세로는 아래쪽 기준
            // → 위쪽은 하늘 배경만 잘리고 캐릭터·문구·버튼은 그대로 보인다
            val iw = d.intrinsicWidth.toFloat(); val ih = d.intrinsicHeight.toFloat()
            val s = maxOf(img.width / iw, img.height / ih)
            img.imageMatrix = android.graphics.Matrix().apply {
                setScale(s, s); postTranslate((img.width - iw * s) / 2f, img.height - ih * s)
            }
            val r = RectF(BTN)
            // BTN은 원본(1152x960) 픽셀 기준 → 드로어블 크기 기준으로 환산 후 화면 좌표로 변환
            val sx = d.intrinsicWidth / SRC_W; val sy = d.intrinsicHeight / SRC_H
            r.set(r.left * sx, r.top * sy, r.right * sx, r.bottom * sy)
            img.imageMatrix.mapRect(r)
            val lp = hotspot.layoutParams as FrameLayout.LayoutParams
            val nl = r.left.toInt(); val nt = r.top.toInt(); val nw = r.width().toInt(); val nh = r.height().toInt()
            if (lp.leftMargin != nl || lp.topMargin != nt || lp.width != nw || lp.height != nh) {
                lp.leftMargin = nl; lp.topMargin = nt; lp.width = nw; lp.height = nh
                (hotspot.background as GradientDrawable).cornerRadius = nh / 2f
                hotspot.post { hotspot.layoutParams = lp }
            }
        }
    }
    companion object {
        private const val SRC_W = 1152f
        private const val SRC_H = 960f
        /** 그림 속 "시작하기" 버튼 위치 (원본 픽셀) */
        private val BTN = RectF(686f, 707f, 1041f, 807f)
        fun skyBackground() = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#EEF2FF"), Color.parseColor("#D6E2FD"), Color.parseColor("#C2D6FA")))
    }
}
