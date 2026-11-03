package com.example.test.yeonsu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R

/** 연저씨 · 로딩(스플래시). 약 1초 보여준 뒤 시작 화면으로 이동한다. */
class LoadingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goNext = Runnable {
        startActivity(Intent(this, StartActivity::class.java))
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(R.anim.y_fade_in, R.anim.y_fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val img = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_loading_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
            alpha = 0f
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#EEF2FF"))
            addView(img, FrameLayout.LayoutParams(-1, -1))
        })
        // 처음에 서서히 나타남 (0.6초)
        img.animate().alpha(1f).setDuration(600).start()
        handler.postDelayed(goNext, 1500)
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
