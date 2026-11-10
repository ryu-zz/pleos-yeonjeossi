package com.example.test.yeonsu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.test.R

/**
 * 연저씨 · 로딩(스플래시).
 * 서서히 나타남(FADE_MS) → 그대로 보여줌 → 서서히 사라짐(FADE_MS) → 시작 화면(서서히 나타남).
 * 전체 보여주는 시간은 SHOW_MS 로 조절한다.
 */
class LoadingActivity : FadeActivity() {
    private val handler = Handler(Looper.getMainLooper())
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
        val img = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_loading_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#EEF2FF"))
            addView(img, FrameLayout.LayoutParams(-1, -1))
        })
        // 페이드아웃이 끝나는 시점이 SHOW_MS 가 되도록, 그만큼 일찍 사라지기 시작한다.
        handler.postDelayed(goNext, SHOW_MS - FADE_MS)
    }

    companion object {
        /** 로딩 화면 전체 시간(ms). 나타남 0.7초 + 보여줌 + 사라짐 0.7초. */
        private const val SHOW_MS = 3000L
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
