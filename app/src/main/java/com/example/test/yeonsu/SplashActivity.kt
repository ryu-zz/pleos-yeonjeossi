package com.example.test.yeonsu

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.test.R
import kotlin.math.min

/**
 * 연저씨 · 앱 시작 화면
 * 흰 배경 / 왼쪽 아래 캐릭터 / 오른쪽 말풍선 / 아래 남색 3D 버튼 "시작하기"
 * 스타일: 골드 메탈 글자 + 금빛 후광 + 번개 + 입체 그림자 (시네마틱 게임 타이틀 느낌)
 * 말풍선·버튼은 View.onDraw로 직접 그려서 글자가 가려지지 않는다.
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var aura: AuraView
    private lateinit var character: ImageView
    private lateinit var bubble: EpicBubble
    private lateinit var button: EpicButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.WHITE
        setContentView(buildUi())
        playEntrance()
    }

    // ---------------------------------------------------------------- UI
    private fun buildUi(): View {
        val root = FrameLayout(this).apply { setBackgroundColor(Color.WHITE); clipChildren = false }

        aura = AuraView(this)
        root.addView(aura, FrameLayout.LayoutParams(-1, -1))

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false }

        // 왼쪽: 캐릭터를 왼쪽 아래에 붙임
        val left = FrameLayout(this).apply { clipChildren = false }
        character = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_start)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_END
        }
        left.addView(character, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM or Gravity.START))
        row.addView(left, LinearLayout.LayoutParams(0, -1, 1.02f))

        // 오른쪽: 말풍선 + 버튼
        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(20f), dp(36f), dp(20f))
            clipChildren = false
        }
        bubble = EpicBubble(this, listOf("자네!,", "오늘도 나와", "함께하겠나?"))
        right.addView(bubble, LinearLayout.LayoutParams(-1, -2))

        button = EpicButton(this, "시작하기") { startMain() }
        right.addView(button, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(18f)
            leftMargin = bubble.bodyLeft.toInt() - button.padX.toInt()
            rightMargin = bubble.shadowPad.toInt() - button.padX.toInt()
        })
        row.addView(right, LinearLayout.LayoutParams(0, -1, 1f))

        root.addView(row, FrameLayout.LayoutParams(-1, -1))
        return root
    }

    private fun startMain() {
        startActivity(Intent(this, FirstRideActivity::class.java))
        finish()
        @Suppress("DEPRECATION") overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    // ------------------------------------------------------------ 등장 연출
    private fun playEntrance() {
        aura.alpha = 0f
        character.alpha = 0f; character.translationX = -dp(100f).toFloat()
        bubble.alpha = 0f; bubble.scaleX = 0.8f; bubble.scaleY = 0.8f
        button.alpha = 0f; button.translationY = dp(40f).toFloat()
        bubble.post {
            bubble.pivotX = 0f; bubble.pivotY = bubble.height * 0.8f
            aura.animate().alpha(1f).setDuration(500).start()
            character.animate().alpha(1f).translationX(0f).setDuration(560).start()
            bubble.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(300).setDuration(520)
                .setInterpolator(OvershootInterpolator(1.5f)).start()
            button.animate().alpha(1f).translationY(0f).setStartDelay(600).setDuration(440).start()
            aura.flash()
        }
    }

    // ------------------------------------------------------------ 공통 도구
    private fun dp(v: Float) = (v * resources.displayMetrics.density).toInt()
    private fun spPx(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    /** 골드 메탈 글자를 그린다: 금빛 글로우 → 어두운 외곽선 → 메탈 그라데이션 채움 */
    private fun drawGoldText(c: Canvas, text: String, cx: Float, baseline: Float, sizePx: Float, glow: Boolean = true) {
        val face = Typeface.create("sans-serif-black", Typeface.NORMAL)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = face; textSize = sizePx; textAlign = Paint.Align.CENTER }
        if (glow) {
            p.style = Paint.Style.FILL; p.color = Color.parseColor("#FFC93C")
            p.setShadowLayer(sizePx * 0.22f, 0f, 0f, Color.parseColor("#CCFFB300"))
            c.drawText(text, cx, baseline, p); p.clearShadowLayer()
        }
        p.style = Paint.Style.STROKE; p.strokeWidth = sizePx * 0.13f; p.strokeJoin = Paint.Join.ROUND
        p.color = Color.parseColor("#2B1700")
        c.drawText(text, cx, baseline, p)
        p.style = Paint.Style.FILL
        p.shader = LinearGradient(0f, baseline - sizePx * 0.85f, 0f, baseline + sizePx * 0.12f,
            intArrayOf(Color.parseColor("#FFFBE0"), Color.parseColor("#FFD84D"), Color.parseColor("#C98A0B"), Color.parseColor("#FFE58A")),
            floatArrayOf(0f, 0.42f, 0.7f, 1f), Shader.TileMode.CLAMP)
        c.drawText(text, cx, baseline, p)
    }

    // ------------------------------------------------------------ 배경: 금빛 후광 + 번개
    private inner class AuraView(ctx: Context) : View(ctx) {
        private var strength = 0.35f
        private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        private val boltCore = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
            strokeWidth = dp(2.4f).toFloat(); color = Color.parseColor("#FFF3B0")
        }
        private val boltHalo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
            strokeWidth = dp(11f).toFloat(); color = Color.parseColor("#FFC21A")
            maskFilter = BlurMaskFilter(dp(9f).toFloat(), BlurMaskFilter.Blur.NORMAL)
        }
        private val bolts = listOf(
            floatArrayOf(0.50f, 0.00f, 0.482f, 0.11f, 0.516f, 0.20f, 0.470f, 0.34f, 0.502f, 0.43f, 0.462f, 0.57f),
            floatArrayOf(0.585f, 0.00f, 0.566f, 0.09f, 0.594f, 0.16f, 0.552f, 0.26f),
            floatArrayOf(0.07f, 0.02f, 0.10f, 0.09f, 0.055f, 0.17f, 0.085f, 0.24f),
        )

        init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

        fun flash() {
            ValueAnimator.ofFloat(0f, 1f, 0.15f, 0.95f, 0.2f, 0.7f, 0.3f).apply {
                duration = 1000
                addUpdateListener { strength = it.animatedValue as Float; invalidate() }
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: android.animation.Animator) {
                        ValueAnimator.ofFloat(0.22f, 0.5f).apply {
                            duration = 1700; repeatCount = ValueAnimator.INFINITE; repeatMode = ValueAnimator.REVERSE
                            addUpdateListener { strength = it.animatedValue as Float; invalidate() }
                        }.start()
                    }
                })
            }.start()
        }

        override fun onDraw(c: Canvas) {
            val w = width.toFloat(); val h = height.toFloat()
            // 캐릭터 뒤 금빛 후광
            val cx = w * 0.26f; val cy = h * 0.62f; val r = h * 0.85f
            glow.shader = RadialGradient(cx, cy, r,
                intArrayOf(Color.parseColor("#99FFD36B"), Color.parseColor("#44FFE3A0"), Color.parseColor("#00FFFFFF")),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w, h, glow)
            // 번개
            boltHalo.alpha = (230 * strength).toInt(); boltCore.alpha = (255 * strength.coerceAtLeast(0.2f)).toInt()
            for (b in bolts) {
                val path = Path().apply {
                    moveTo(b[0] * w, b[1] * h)
                    var i = 2
                    while (i < b.size) { lineTo(b[i] * w, b[i + 1] * h); i += 2 }
                }
                c.drawPath(path, boltHalo); c.drawPath(path, boltCore)
            }
        }
    }

    // ------------------------------------------------------------ 말풍선: 남색 다크 + 골드 테두리 + 골드 메탈 글자
    private inner class EpicBubble(ctx: Context, private val lines: List<String>) : View(ctx) {
        val shadowPad = dp(24f).toFloat()
        private val tail = dp(38f).toFloat()
        val bodyLeft = tail + shadowPad
        private val radius = dp(40f).toFloat()
        private val padV = dp(34f).toFloat()
        private val padH = dp(26f).toFloat()

        private var textPx = 0f
        private val body = RectF()
        private val path = Path()
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val gloss = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(3.5f).toFloat() }
        private val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = dp(1.3f).toFloat(); color = Color.parseColor("#77FFD966")
        }

        init { setLayerType(LAYER_TYPE_SOFTWARE, null) }

        override fun onMeasure(w: Int, h: Int) {
            val width = MeasureSpec.getSize(w)
            val avail = width - bodyLeft - shadowPad - padH * 2
            val probe = Paint().apply { typeface = Typeface.create("sans-serif-black", Typeface.NORMAL); textSize = 100f }
            val widest = lines.maxOf { probe.measureText(it) }
            textPx = min(spPx(66f), 100f * avail / widest)
            val lh = textPx * 1.2f
            val height = shadowPad * 0.5f + padV * 2 + lh * lines.size + shadowPad * 1.3f
            setMeasuredDimension(width, height.toInt())
        }

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            body.set(bodyLeft, shadowPad * 0.5f, w - shadowPad, h - shadowPad * 1.3f)
            path.reset()
            path.addRoundRect(body, radius, radius, Path.Direction.CW)
            val t = Path().apply {
                moveTo(body.left + 3f, body.top + body.height() * 0.56f)
                quadTo(shadowPad + tail * 0.35f, body.top + body.height() * 0.80f, shadowPad, body.top + body.height() * 0.93f)
                quadTo(shadowPad + tail * 0.85f, body.top + body.height() * 0.90f, body.left + 3f, body.top + body.height() * 0.76f)
                close()
            }
            path.op(t, Path.Op.UNION)
            fill.shader = LinearGradient(0f, body.top, 0f, body.bottom,
                Color.parseColor("#1F2D66"), Color.parseColor("#0A1030"), Shader.TileMode.CLAMP)
            gloss.shader = LinearGradient(0f, body.top, 0f, body.top + body.height() * 0.5f,
                Color.parseColor("#33FFFFFF"), Color.parseColor("#00FFFFFF"), Shader.TileMode.CLAMP)
            rim.shader = LinearGradient(0f, body.top, 0f, body.bottom,
                intArrayOf(Color.parseColor("#FFF1A8"), Color.parseColor("#E1A720"), Color.parseColor("#8A5A00"), Color.parseColor("#FFD966")),
                floatArrayOf(0f, 0.4f, 0.75f, 1f), Shader.TileMode.CLAMP)
        }

        override fun onDraw(c: Canvas) {
            // 바닥 그림자 + 면
            fill.setShadowLayer(dp(22f).toFloat(), 0f, dp(13f).toFloat(), Color.parseColor("#66000020"))
            c.drawPath(path, fill); fill.clearShadowLayer()
            // 윗면 광택
            c.save(); c.clipPath(path); c.drawRect(body.left, body.top, body.right, body.top + body.height() * 0.5f, gloss); c.restore()
            // 골드 테두리 (겹선)
            c.drawPath(path, rim)
            val ins = RectF(body).apply { inset(dp(11f).toFloat(), dp(11f).toFloat()) }
            c.drawRoundRect(ins, radius - dp(10f), radius - dp(10f), inner)
            // 글자
            val lh = textPx * 1.2f
            val top = body.top + (body.height() - lh * lines.size) / 2f
            val cx = body.centerX()
            lines.forEachIndexed { i, s -> drawGoldText(c, s, cx, top + lh * i + textPx * 0.9f, textPx) }
        }
    }

    // ------------------------------------------------------------ 남색 3D 버튼 + 골드 글자
    private inner class EpicButton(ctx: Context, private val label: String, private val onClick: () -> Unit) : View(ctx) {
        val padX = dp(8f).toFloat()
        private val faceH = dp(112f).toFloat()
        private val depth = dp(16f).toFloat()
        private val bottomPad = dp(26f).toFloat()
        private var press = 0f

        private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#050A20") }
        private val face = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = dp(3f).toFloat() }
        private val gloss = Paint(Paint.ANTI_ALIAS_FLAG)
        private val r = dp(34f).toFloat()

        init { setLayerType(LAYER_TYPE_SOFTWARE, null); isClickable = true }

        override fun onMeasure(w: Int, h: Int) =
            setMeasuredDimension(MeasureSpec.getSize(w), (faceH + depth + bottomPad).toInt())

        private fun animatePress(to: Float, ms: Long) {
            ValueAnimator.ofFloat(press, to).apply {
                duration = ms
                addUpdateListener { press = it.animatedValue as Float; invalidate() }
            }.start()
        }

        override fun onDraw(c: Canvas) {
            val l = padX; val rr = width - padX
            // 두께(아래 어두운 면) + 바닥 그림자
            val e = RectF(l, depth, rr, depth + faceH)
            edge.setShadowLayer(dp(20f).toFloat(), 0f, dp(12f).toFloat(), Color.parseColor("#80000020"))
            c.drawRoundRect(e, r, r, edge); edge.clearShadowLayer()
            // 윗면
            val top = press
            val f = RectF(l, top, rr, top + faceH)
            face.shader = LinearGradient(0f, f.top, 0f, f.bottom,
                Color.parseColor("#3D5CC8"), Color.parseColor("#17225F"), Shader.TileMode.CLAMP)
            c.drawRoundRect(f, r, r, face)
            // 광택
            c.save()
            val clip = Path().apply { addRoundRect(f, r, r, Path.Direction.CW) }
            c.clipPath(clip)
            gloss.shader = LinearGradient(0f, f.top, 0f, f.top + faceH * 0.55f,
                Color.parseColor("#55FFFFFF"), Color.parseColor("#00FFFFFF"), Shader.TileMode.CLAMP)
            c.drawRect(f.left, f.top, f.right, f.top + faceH * 0.55f, gloss)
            c.restore()
            // 골드 테두리
            rim.shader = LinearGradient(0f, f.top, 0f, f.bottom,
                intArrayOf(Color.parseColor("#FFF1A8"), Color.parseColor("#E1A720"), Color.parseColor("#8A5A00"), Color.parseColor("#FFD966")),
                floatArrayOf(0f, 0.4f, 0.75f, 1f), Shader.TileMode.CLAMP)
            val rimRect = RectF(f).apply { inset(rim.strokeWidth / 2, rim.strokeWidth / 2) }
            c.drawRoundRect(rimRect, r, r, rim)
            // 글자 (항상 면 위에 그려지므로 가려지지 않음)
            val size = spPx(40f)
            drawGoldText(c, label, f.centerX(), f.centerY() + size * 0.34f, size)
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> animatePress(depth * 0.78f, 70)
                MotionEvent.ACTION_UP -> {
                    animatePress(0f, 120)
                    if (e.x in 0f..width.toFloat() && e.y in 0f..height.toFloat()) { performClick(); onClick() }
                }
                MotionEvent.ACTION_CANCEL -> animatePress(0f, 120)
            }
            return true
        }

        override fun performClick(): Boolean { super.performClick(); return true }
    }
}
