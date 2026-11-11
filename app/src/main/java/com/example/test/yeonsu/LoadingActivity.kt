package com.example.test.yeonsu

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.R

/**
 * 연저씨 · 로딩(스플래시).
 * 일반 앱의 시작 화면처럼: 가운데 로고(핸들 + 연저씨)가 살짝 떠오르며 나타나고(핸들은 계속 좌우로 살짝 흔들림), 그 아래 우리 얼굴 4개,
 * 그 아래 로딩 점 3개가 차례로 깜빡인다. 맨 아래에는 "Team 연저씨"가 작게 들어간다.
 * 서서히 나타남(FADE_MS) → 그대로 보여줌 → 서서히 사라짐(FADE_MS) → 시작 화면(서서히 나타남).
 * 전체 보여주는 시간은 SHOW_MS 로 조절한다.
 */
class LoadingActivity : FadeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var dotsAnim: ValueAnimator? = null
    private val goNext = Runnable {
        // 화면을 서서히 지운 뒤 시작 화면으로 넘어간다.
        findViewById<android.view.View>(android.R.id.content).animate()
            .alpha(0f).setDuration(FADE_MS)
            .withEndAction { if (!isFinishing) goTo(StartActivity::class.java) }
            .start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 다른 앱을 갔다가 연저씨 아이콘으로 돌아온 경우: 처음부터 다시 시작하지 않고, 아래에 있던 화면을 그대로 보여준다.
        if (live > 0) {
            finish()
            @Suppress("DEPRECATION") overridePendingTransition(0, 0)
            return
        }
        val bg = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_loading_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        // 로고: 핸들(따로 흔들림) + "연저씨" 글씨
        val wheel = ImageView(this).apply { setImageResource(R.drawable.yeonsu_loading_wheel) }
        val name = ImageView(this).apply { setImageResource(R.drawable.yeonsu_loading_name) }
        val logo = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(wheel); addView(name)
        }
        val dots = LoadingDots(this)
        val team = ImageView(this).apply { setImageResource(R.drawable.yeonsu_loading_team); adjustViewBounds = true }
        val credit = TextView(this).apply {
            text = "Team 연저씨"; setTextColor(Color.parseColor("#7C8AA8")); gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
        }
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            addView(credit)
        }
        val center = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            addView(logo); addView(team); addView(dots)
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#EEF2FF"))
            addView(bg, FrameLayout.LayoutParams(-1, -1))
            addView(center, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
            addView(footer, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM))
        }
        setContentView(root)
        // 크기는 화면 높이에 맞춘다(화면마다 비율이 같게)
        root.post {
            val h = root.height.toFloat()
            val lh = h * 0.075f   // 로고 높이(그림 208px 기준)
            logo.layoutParams = LinearLayout.LayoutParams(-2, -2)
            wheel.layoutParams = LinearLayout.LayoutParams((lh * 223f / 208f).toInt(), lh.toInt())
            name.layoutParams = LinearLayout.LayoutParams((lh * 414f / 208f).toInt(), lh.toInt()).apply { leftMargin = (lh * 49f / 208f).toInt() }
            dots.layoutParams = LinearLayout.LayoutParams((h * 0.12f).toInt(), (h * 0.04f).toInt()).apply { topMargin = (h * 0.06f).toInt() }
            team.layoutParams = LinearLayout.LayoutParams((h * 0.2f * 661f / 202f).toInt(), (h * 0.2f).toInt()).apply { topMargin = (h * 0.075f).toInt() }
            credit.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, h * 0.022f)
            credit.layoutParams = LinearLayout.LayoutParams(-2, -2).apply { topMargin = (h * 0.012f).toInt() }
            (footer.layoutParams as FrameLayout.LayoutParams).bottomMargin = (h * 0.06f).toInt()
            root.requestLayout()
            // 로고는 살짝 아래에서 떠오르며 나타나고, 팀 표시는 조금 늦게 나타난다
            logo.alpha = 0f; logo.translationY = h * 0.03f
            logo.animate().alpha(1f).translationY(0f).setStartDelay(150).setDuration(700).setInterpolator(DecelerateInterpolator()).start()
            dots.alpha = 0f; dots.animate().alpha(1f).setStartDelay(500).setDuration(400).start()
            team.alpha = 0f; team.animate().alpha(1f).setStartDelay(350).setDuration(600).start()
            footer.alpha = 0f; footer.animate().alpha(1f).setStartDelay(700).setDuration(600).start()
        }
        dotsAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200; repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val f = it.animatedValue as Float
                dots.phase = f
                // 핸들: 왼쪽·오른쪽으로 살짝(±10도) 돌렸다가 가운데로. 한 바퀴(1.2초) 중 앞쪽에서만 움직이고 잠깐 쉰다
                wheel.rotation = if (f < 0.7f) (10f * Math.sin(f / 0.7 * 2 * Math.PI)).toFloat() else 0f
            }
            start()
        }
        // 페이드아웃이 끝나는 시점이 SHOW_MS 가 되도록, 그만큼 일찍 사라지기 시작한다.
        handler.postDelayed(goNext, SHOW_MS - FADE_MS)
    }

    companion object {
        /** 로딩 화면 전체 시간(ms). 나타남 0.7초 + 보여줌 + 사라짐 0.7초. */
        private const val SHOW_MS = 3000L
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        dotsAnim?.cancel()
        super.onDestroy()
    }
}

/** 로딩 점 3개: 왼쪽부터 차례로 커졌다 작아지며 진해진다. */
private class LoadingDots(ctx: Context) : View(ctx) {
    var phase = 0f
        set(v) { field = v; invalidate() }
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(c: Canvas) {
        val r = height * 0.32f; val gap = width / 3f
        for (i in 0..2) {
            // 점마다 1/3씩 늦게 튄다
            val t = ((phase - i / 3f) % 1f + 1f) % 1f
            val k = if (t < 0.5f) Math.sin(t / 0.5 * Math.PI).toFloat() else 0f
            p.color = Color.argb((90 + 165 * k).toInt(), 46, 102, 170)
            c.drawCircle(gap * (i + 0.5f), height / 2f, r * (0.65f + 0.35f * k), p)
        }
    }
}
