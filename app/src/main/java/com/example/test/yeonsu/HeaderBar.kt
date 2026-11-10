package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.test.R

/**
 * 연저씨 · 모든 화면이 함께 쓰는 상단 바.
 * 왼쪽: [뒤로] 마크 이름 / 오른쪽 끝: [(화면별 버튼)] [홈] [도움말]. 설정 버튼은 없다.
 * 도움말 버튼은 어느 화면에서든 같은 크기·같은 자리(오른쪽 끝에서 32, 위에서 24 단위)에 놓인다.
 * 단위(k)는 화면 폭 기준: 가로형은 폭/1672*1.6.
 */
object HeaderBar {
    const val BTN = 56f
    const val TOP = 24f      // 도움말 버튼 윗변 (바 높이 96의 가운데에 맞춤)
    const val RIGHT = 32f    // 도움말 버튼 오른쪽 여백
    const val BAR_H = 96f
    fun scale(width: Int) = width / 1672f * 1.6f

    private fun px(k: Float, v: Float) = (v * k).toInt()

    /** 둥근 네모 흰 버튼 (뒤로·홈·도움말 공통 모양) */
    fun iconButton(a: Activity, k: Float, icon: Int, onClick: () -> Unit) = ImageView(a).apply {
        setImageResource(icon)
        val p = px(k, BTN * 0.26f); setPadding(p, p, p, p)
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = px(k, 16f).toFloat() }
        elevation = px(k, 4f).toFloat()
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    /** 오른쪽 위에 따로 얹는 도움말 버튼용 배치값 (바 안에 넣지 않는 화면에서 쓴다) */
    fun helpParams(k: Float) = FrameLayout.LayoutParams(px(k, BTN), px(k, BTN), Gravity.TOP or Gravity.END).apply {
        topMargin = px(k, TOP); rightMargin = px(k, RIGHT)
    }

    /**
     * 상단 바. 높이는 호출하는 쪽에서 BAR_H*k 로 정한다.
     * backTo가 null이면 뒤로 버튼 없이 마크부터 시작한다.
     */
    fun build(a: FadeActivity, k: Float, backTo: Class<out Activity>?, showHome: Boolean,
              extra: View?, onHelp: () -> Unit): View {
        val row = LinearLayout(a).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(k, 48f), px(k, 8f), px(k, RIGHT), 0)
        }
        if (backTo != null) {
            row.addView(iconButton(a, k, R.drawable.ic_y_chev_left) { a.goTo(backTo) },
                LinearLayout.LayoutParams(px(k, BTN), px(k, BTN)).apply { rightMargin = px(k, 14f) })
        }
        row.addView(ImageView(a).apply { setImageResource(R.drawable.ic_y_wheel) },
            LinearLayout.LayoutParams(px(k, 40f), px(k, 40f)))
        row.addView(TextView(a).apply {
            text = "연저씨"; setTextSize(TypedValue.COMPLEX_UNIT_PX, 30f * k); setTextColor(ScreenActivity.NAVY)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            includeFontPadding = false; setPadding(px(k, 12f), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        extra?.let { row.addView(it, LinearLayout.LayoutParams(-2, px(k, BTN)).apply { leftMargin = px(k, 12f) }) }
        if (showHome) {
            row.addView(iconButton(a, k, R.drawable.ic_y_home) { a.goTo(HomeActivity::class.java) },
                LinearLayout.LayoutParams(px(k, BTN), px(k, BTN)).apply { leftMargin = px(k, 12f) })
        }
        row.addView(iconButton(a, k, R.drawable.ic_y_help) { onHelp() },
            LinearLayout.LayoutParams(px(k, BTN), px(k, BTN)).apply { leftMargin = px(k, 12f) })
        return row
    }
}
