package com.example.test.yeonsu
import android.graphics.Color
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.test.R
/**
 * 연저씨 · 시작 화면.
 * 디자인 시안(yeonsu_start_screen.png)을 조각 그림으로 나눠 다시 쌓고, 순서대로 튀어나오게 한다.
 *   배경 → 연저씨 → "자네, 오늘도 나와 함께하겠나?" → 작은 글씨·로고 → 시작하기 버튼
 * 조각 위치는 시안 원본(1152x960) 좌표로 적어 두고, 화면을 꽉 채우며 아래쪽에 맞춰 배치한다.
 * (시안 한 장짜리 그림 yeonsu_start_screen.png 은 비교·복구용으로 남겨 둔다)
 */
class StartActivity : FadeActivity() {
    private class Piece(val res: Int, val rect: RectF) { lateinit var view: ImageView }

    private val pieces = listOf(
        Piece(R.drawable.yeonsu_start_char, RectF(18f, 236f, 18f + 595f, 236f + 681f)),
        Piece(R.drawable.yeonsu_start_logo, RectF(793f, 165f, 793f + 311f, 165f + 37f)),
        Piece(R.drawable.yeonsu_start_title, RectF(592f, 362f, 592f + 533f, 362f + 201f)),
        Piece(R.drawable.yeonsu_start_sub, RectF(695f, 611f, 695f + 260f, 611f + 60f)),
        Piece(R.drawable.yeonsu_start_btn, RectF(682f, 709f, 682f + 352f, 709f + 104f)),
    )
    private val yeonsu get() = pieces[0].view
    private val logo get() = pieces[1].view
    private val title get() = pieces[2].view
    private val sub get() = pieces[3].view
    private val btn get() = pieces[4].view
    private var animated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bg = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_start_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val stage = FrameLayout(this).apply { clipChildren = false; clipToPadding = false }
        pieces.forEach { p ->
            p.view = ImageView(this).apply {
                setImageResource(p.res); scaleType = ImageView.ScaleType.FIT_XY
                alpha = 0f   // 처음엔 숨겨 두고 순서대로 나타나게 한다
            }
            stage.addView(p.view, FrameLayout.LayoutParams(0, 0))
        }
        btn.apply {
            isClickable = true; isFocusable = true
            contentDescription = "시작하기"
            setOnClickListener { goTo(HomeActivity::class.java) }
            // 누르는 동안 살짝 작아졌다가 돌아온다
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(90).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                }
                false
            }
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F3F6FD"))
            clipChildren = false
            addView(bg, FrameLayout.LayoutParams(-1, -1))
            addView(stage, FrameLayout.LayoutParams(-1, -1))
        }
        setContentView(root)
        // 화면 크기가 정해지면(창 크기가 바뀌어도) 조각 위치를 다시 맞춘다
        stage.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            val w = r - l; val h = b - t
            if (w == 0 || h == 0) return@addOnLayoutChangeListener
            if (w != or - ol || h != ob - ot || !animated) layoutPieces(w, h)
            if (!animated) { animated = true; stage.post { playIntro() } }
        }
    }

    /** 시안 좌표 → 화면 좌표. 화면을 꽉 채우는 배율(남는 부분은 잘림), 가로는 가운데, 세로는 아래쪽 기준. */
    private fun layoutPieces(w: Int, h: Int) {
        val s = maxOf(w / SRC_W, h / SRC_H)
        val tx = (w - SRC_W * s) / 2f; val ty = h - SRC_H * s
        pieces.forEach { p ->
            val lp = p.view.layoutParams as FrameLayout.LayoutParams
            lp.leftMargin = (tx + p.rect.left * s).toInt(); lp.topMargin = (ty + p.rect.top * s).toInt()
            lp.width = (p.rect.width() * s).toInt(); lp.height = (p.rect.height() * s).toInt()
            p.view.layoutParams = lp
        }
    }

    /** 배경 → 연저씨 → 문구 → 작은 글씨·로고 → 버튼 순서로 튀어나온다. */
    private fun playIntro() {
        val dp = resources.displayMetrics.density
        // 연저씨: 아래에서 살짝 튀어 올라온다
        yeonsu.apply {
            translationY = 60 * dp; scaleX = 0.92f; scaleY = 0.92f
            pivotX = width / 2f; pivotY = height.toFloat()
            animate().alpha(1f).translationY(0f).scaleX(1f).scaleY(1f)
                .setStartDelay(CHAR_AT).setDuration(550).setInterpolator(OvershootInterpolator(1.2f)).start()
        }
        // 문구: 작게 있다가 커지면서 톡 튀어나온다
        title.apply {
            scaleX = 0.6f; scaleY = 0.6f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(TITLE_AT).setDuration(450).setInterpolator(OvershootInterpolator(2.2f)).start()
        }
        // 작은 글씨·로고: 살짝 올라오며 나타난다
        listOf(sub, logo).forEach { v ->
            v.translationY = 14 * dp
            v.animate().alpha(1f).translationY(0f)
                .setStartDelay(SUB_AT).setDuration(400).setInterpolator(DecelerateInterpolator()).start()
        }
        // 시작하기 버튼: 통통 튀며 나타난다
        btn.apply {
            scaleX = 0.5f; scaleY = 0.5f
            animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(BTN_AT).setDuration(500).setInterpolator(OvershootInterpolator(2.6f)).start()
        }
    }

    companion object {
        private const val SRC_W = 1152f
        private const val SRC_H = 960f
        // 조각이 나타나기 시작하는 시각(ms). 화면 전체가 서서히 나타나는(FADE_MS) 동안 배경이 먼저 보인다.
        private const val CHAR_AT = 350L
        private const val TITLE_AT = 800L
        private const val SUB_AT = 1150L
        private const val BTN_AT = 1400L
        fun skyBackground() = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#EEF2FF"), Color.parseColor("#D6E2FD"), Color.parseColor("#C2D6FA")))
    }
}
