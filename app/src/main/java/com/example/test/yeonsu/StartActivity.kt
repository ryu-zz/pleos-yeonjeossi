package com.example.test.yeonsu

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R

/** 연저씨 · 시작 화면 (와이어프레임: 캐릭터 왼쪽, 문구 오른쪽, 하단 "시작하기") */
class StartActivity : AppCompatActivity() {

    private var k = 1f
    private fun dp(v: Float) = (v * k).toInt()
    private fun tv(text: String, sp: Float, color: Int, bold: Boolean = false, italic: Boolean = false) =
        TextView(this).apply {
            this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_PX, sp * k); setTextColor(color)
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            if (italic) paint.textSkewX = -0.18f
            includeFontPadding = false
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply { background = skyBackground() }
        setContentView(root)
        root.post { build(root, root.width, root.height) }
    }

    private fun build(root: FrameLayout, w: Int, h: Int) {
        k = w / 1672f * 1.6f

        // 캐릭터: 왼쪽 아래
        root.addView(ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_start)
            scaleType = ImageView.ScaleType.FIT_START
            adjustViewBounds = true
        }, FrameLayout.LayoutParams(0, -2, Gravity.BOTTOM or Gravity.START).also { lp ->
            lp.width = minOf(h * 0.95f, w * 0.52f).toInt()
            lp.leftMargin = -dp(40f)
            lp.bottomMargin = -dp(10f)
        })

        // 오른쪽 위: HYUNDAI | 연저씨
        val brand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        brand.addView(ImageView(this).apply { setImageResource(R.drawable.hyundai_logo) }, LinearLayout.LayoutParams(dp(130f), dp(24f)))
        brand.addView(View(this).apply { setBackgroundColor(Color.parseColor("#33000000")) },
            LinearLayout.LayoutParams(dp(1.5f), dp(28f)).apply { setMargins(dp(14f), 0, dp(14f), 0) })
        brand.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_wheel) }, LinearLayout.LayoutParams(dp(34f), dp(34f)))
        brand.addView(tv("연저씨", 26f, Color.parseColor("#0F1B3D"), bold = true).apply { setPadding(dp(8f), 0, 0, 0) })
        root.addView(brand, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply { topMargin = dp(28f); rightMargin = dp(40f) })

        // 오른쪽: 문구 + 버튼
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        right.addView(tv("자네, 오늘도", 50f, Color.parseColor("#0F1B3D"), bold = true, italic = true))
        right.addView(tv("나와 함께하겠나?", 50f, Color.parseColor("#1030E0"), bold = true, italic = true).apply { setPadding(0, dp(6f), 0, 0) })
        right.addView(tv("더 안전하고, 더 스마트한\n운전 연수의 시작", 25f, Color.parseColor("#33415C")).apply {
            setLineSpacing(0f, 1.25f); setPadding(0, dp(22f), 0, 0)
        })
        right.addView(startButton(), LinearLayout.LayoutParams(dp(390f), dp(108f)).apply { topMargin = dp(34f) })
        root.addView(right, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_VERTICAL or Gravity.END).apply {
            rightMargin = dp(105f); topMargin = dp(40f)
        })
    }

    private fun startButton(): View {
        val bg = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(Color.parseColor("#12A3A8"), Color.parseColor("#0B6AA8"))).apply {
            cornerRadius = dp(50f).toFloat()
            setStroke(dp(2f), Color.parseColor("#88FFFFFF"))
        }
        return LinearLayout(this).apply {
            background = bg; gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL
            elevation = dp(10f).toFloat(); isClickable = true; isFocusable = true
            addView(tv("시작하기", 36f, Color.WHITE, bold = true))
            addView(ImageView(context).apply { setImageResource(R.drawable.ic_y_chev_right); setColorFilter(Color.WHITE) },
                LinearLayout.LayoutParams(dp(38f), dp(38f)).apply { leftMargin = dp(20f) })
            setOnClickListener {
                startActivity(Intent(this@StartActivity, HomeActivity::class.java))
                finish()
                @Suppress("DEPRECATION") overridePendingTransition(R.anim.y_fade_in, R.anim.y_fade_out)
            }
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).start()
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                }
                false
            }
        }
    }

    companion object {
        fun skyBackground() = GradientDrawable(GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#EEF2FF"), Color.parseColor("#D6E2FD"), Color.parseColor("#C2D6FA")))
    }
}
