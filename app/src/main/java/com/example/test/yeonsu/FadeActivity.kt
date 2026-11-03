package com.example.test.yeonsu

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

/** 연저씨의 모든 화면이 상속한다. 화면이 열리면 항상 서서히 나타난다(페이드인). */
abstract class FadeActivity : AppCompatActivity() {
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        findViewById<View>(android.R.id.content).apply {
            alpha = 0f
            animate().alpha(1f).setDuration(FADE_MS).start()
        }
    }

    /** 다음 화면으로 이동. 시스템 기본 전환은 끄고, 다음 화면이 스스로 페이드인한다. */
    fun goTo(target: Class<out Activity>, closeThis: Boolean = true) {
        startActivity(Intent(this, target))
        if (closeThis) finish()
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }

    companion object { const val FADE_MS = 700L }
}
