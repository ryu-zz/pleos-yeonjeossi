package com.example.test.yeonsu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R

/** 연저씨 · 로딩(스플래시). 약 1초 보여준 뒤 시작 화면으로 이동한다. */
class LoadingActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goNext = Runnable {
        startActivity(Intent(this, StartActivity::class.java))
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_loading_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
        })
        handler.postDelayed(goNext, 1000)
    }

    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
