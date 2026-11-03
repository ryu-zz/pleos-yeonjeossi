package com.example.test.yeonsu
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.test.R
/** 연저씨 · 일반 주행 로딩. 시작 전 로딩 페이지를 보여주고 1.5초 뒤 플레오스 내비를 켠다. */
class DriveLoadingActivity : FadeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goNext = Runnable {
        // 일반 주행 화면을 뒤에 깔고 그 위에 내비를 켠다. (손잡이를 끌면 이 앱이 오른쪽 칸에 나온다)
        startActivity(android.content.Intent(this, DriveActivity::class.java).putExtra(DriveActivity.EXTRA_OPEN_NAVI, true))
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(0, 0)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
