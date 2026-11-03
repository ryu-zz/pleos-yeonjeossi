package com.example.test.yeonsu
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.test.R
/** 연저씨 · 연습 주행 로딩. 일반 주행 카드를 누르면 먼저 나오고, 1.5초 뒤 일반 주행 화면으로 넘어간다. */
class DriveLoadingActivity : FadeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val goNext = Runnable { goTo(DriveActivity::class.java) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val img = ImageView(this).apply {
            setImageResource(R.drawable.drive_loading_bg)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#9DC3FE"))
            addView(img, FrameLayout.LayoutParams(-1, -1))
        })
        handler.postDelayed(goNext, 1500)
    }
    override fun onDestroy() {
        handler.removeCallbacks(goNext)
        super.onDestroy()
    }
}
