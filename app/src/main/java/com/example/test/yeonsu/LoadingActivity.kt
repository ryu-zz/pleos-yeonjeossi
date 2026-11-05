package com.example.test.yeonsu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.test.R

/** 연저씨 · 로딩(스플래시). 약 1초 보여준 뒤 시작 화면으로 이동한다. */
class LoadingActivity : FadeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goNext = Runnable { goTo(StartActivity::class.java) }

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
        handler.postDelayed(goNext, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
