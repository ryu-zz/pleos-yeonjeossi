package com.example.test.yeonsu

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.test.R

/**
 * 연저씨 · 일반 주행 화면 (세로형 와이어프레임: 오른쪽 1/3 영역에 맞춘 세로 레이아웃).
 * 1) 대기: "주행 시작하기" 버튼  2) 주행 중: 말풍선으로 안내 문구를 보여주고 음성으로도 읽어준다.
 * 이 화면은 가로가 넓어도 세로 비율(약 9:16) 열을 가운데에 두고 그린다.
 */
class DriveActivity : FadeActivity() {
    private var s = 1f
    private fun dp(v: Float) = (v * s).toInt()
    private fun tv(text: String, px: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_PX, px * s); setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingSpeak: String? = null

    private lateinit var idleLayer: FrameLayout
    private lateinit var activeLayer: FrameLayout
    private lateinit var bubbleChip: TextView
    private lateinit var bubbleText: TextView
    private var testIndex = 0
    private var driving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#9DC3FE"), Color.parseColor("#C9DDFD"), Color.parseColor("#A5C9FD")))
        }
        setContentView(root)
        initTts()
        // 일반 주행 카드에서 왔으면 먼저 이 화면을 만들어 두고 그 위에 내비를 연다.
        // 사용자가 내비 오른쪽 손잡이(⋮)를 왼쪽으로 끌면 이 앱이 오른쪽 칸에 나타난다.
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_OPEN_NAVI, false)) {
            root.postDelayed({ openNavi() }, 400)
        }
        // 창 크기가 바뀌면(분할 화면 등) 현재 크기에 맞게 다시 그린다.
        root.addOnLayoutChangeListener { _, l, tp, r, b, _, _, _, _ ->
            val w = r - l; val h = b - tp
            if (w > 0 && h > 0 && (w != builtW || h != builtH)) { builtW = w; builtH = h; build(root, w, h) }
        }
    }

    private fun openNavi() {
        try {
            startActivity(android.content.Intent().setClassName(NAVI_PKG, "$NAVI_PKG.MainActivity")
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) { Log.e(TAG, "내비 열기 실패 ${e.message}") }
    }

    private var builtW = 0
    private var builtH = 0
    private var lastChip = ""
    private var lastText = ""

    private fun build(root: FrameLayout, w: Int, h: Int) {
        root.removeAllViews()
        val colW = minOf(w.toFloat(), h * 941f / 1670f).toInt()
        s = colW / 941f
        val col = FrameLayout(this)
        root.addView(col, FrameLayout.LayoutParams(colW, h, Gravity.CENTER_HORIZONTAL))

        idleLayer = FrameLayout(this)
        activeLayer = FrameLayout(this).apply { visibility = View.GONE; alpha = 0f }
        col.addView(idleLayer, FrameLayout.LayoutParams(-1, -1))
        col.addView(activeLayer, FrameLayout.LayoutParams(-1, -1))
        buildIdle(idleLayer)
        buildActive(activeLayer)
        // 헤더는 두 상태에서 공통
        // 헤더(연저씨 마크 / 홈·설정·도움말)는 세로 열이 아니라 창 전체 폭 기준: 마크는 맨 왼쪽, 버튼은 맨 오른쪽
        root.addView(buildHeader(), FrameLayout.LayoutParams(-1, -2, Gravity.TOP).apply { topMargin = dp(36f) })
        // 홈·도움말은 다른 화면과 같은 크기·같은 자리(HeaderBar 기준, 화면 폭에 맞춘 단위)에 둔다. 설정은 없다.
        val hk = HeaderBar.scale(w)
        root.addView(HeaderBar.iconButton(this, hk, R.drawable.ic_y_help) {
            Toast.makeText(this, "이 화면은 도움말이 아직 없어요", Toast.LENGTH_SHORT).show()
        }, HeaderBar.helpParams(hk))
        root.addView(HeaderBar.iconButton(this, hk, R.drawable.ic_y_home) { goTo(HomeActivity::class.java) },
            HeaderBar.helpParams(hk).apply { rightMargin += ((HeaderBar.BTN + 12f) * hk).toInt() })
        if (driving) {
            idleLayer.visibility = View.GONE; activeLayer.visibility = View.VISIBLE; activeLayer.alpha = 1f
            bubbleChip.visibility = if (lastChip.isEmpty()) View.GONE else View.VISIBLE
            bubbleChip.text = lastChip; bubbleText.text = lastText.replace("! ", "!\n")
        }
    }

    private fun buildHeader(): View {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_wheel) }, LinearLayout.LayoutParams(dp(78f), dp(78f)))
        row.addView(tv("연저씨", 54f, Color.parseColor("#0F1B3D"), true).apply { setPadding(dp(14f), 0, 0, 0) },
            LinearLayout.LayoutParams(0, -2, 1f))
        return FrameLayout(this).apply {
            setPadding(dp(46f), 0, dp(40f), 0)
            addView(row, FrameLayout.LayoutParams(-1, -2))
        }
    }

    private fun squareButton(icon: Int, size: Float, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(icon)
        val p = dp(size * 0.24f); setPadding(p, p, p, p)
        background = GradientDrawable().apply { setColor(Color.parseColor("#E6FFFFFF")); cornerRadius = dp(26f).toFloat() }
        elevation = dp(5f).toFloat()
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    // ───── 1) 대기 화면 ─────
    private fun buildIdle(layer: FrameLayout) {
        // 캐릭터 (아래에서 위로 올라가는 속도선 배경 포함)
        val charH = (810f * s).toInt()
        layer.addView(FadeEdgeImageView(this, 0.14f, 0.16f, 0.14f).apply {
            setImageResource(R.drawable.drive_char1); scaleType = ImageView.ScaleType.FIT_XY
        }, FrameLayout.LayoutParams(-1, charH, Gravity.TOP).apply { topMargin = dp(430f) })

        // "일반 주행" 알약
        val pill = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL; orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B8FFFFFF")); cornerRadius = dp(75f).toFloat(); setStroke(dp(3f), Color.parseColor("#CCFFFFFF"))
            }
            setPadding(dp(40f), 0, dp(40f), 0); elevation = dp(6f).toFloat()
        }
        pill.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_wheel) }, LinearLayout.LayoutParams(dp(98f), dp(98f)))
        pill.addView(tv("일반 주행", 62f, Color.parseColor("#0F1B3D"), true).apply { setPadding(dp(26f), 0, dp(26f), 0) })
        pill.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_chev_right); rotation = 90f },
            LinearLayout.LayoutParams(dp(50f), dp(50f)))
        layer.addView(pill, FrameLayout.LayoutParams(dp(560f), dp(140f), Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(205f) })
        layer.addView(tv("자유롭게 연습 주행을 시작해요", 38f, Color.parseColor("#1A2A5A")),
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(372f) })

        // 주행 시작하기 버튼
        val btn = LinearLayout(this).apply {
            gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#12A3A8"), Color.parseColor("#0B6AA8"))).apply { cornerRadius = dp(75f).toFloat() }
            elevation = dp(10f).toFloat(); isClickable = true; isFocusable = true
            setOnClickListener { startDriving() }
        }
        btn.addView(tv("▶", 54f, Color.WHITE))
        btn.addView(tv("주행 시작하기", 56f, Color.WHITE, true).apply { setPadding(dp(26f), 0, 0, 0) })
        layer.addView(btn, FrameLayout.LayoutParams(dp(790f), dp(150f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = dp(150f) })
    }

    // ───── 2) 주행 중 화면 ─────
    private fun buildActive(layer: FrameLayout) {
        val charH = (731f * s).toInt()
        layer.addView(FadeEdgeImageView(this, 0.14f, 0f, 0.10f).apply {
            setImageResource(R.drawable.drive_char2); scaleType = ImageView.ScaleType.FIT_XY
        }, FrameLayout.LayoutParams(-1, charH, Gravity.BOTTOM))

        // 말풍선
        val wrap = FrameLayout(this).apply { clipChildren = false; clipToPadding = false }
        wrap.addView(View(this).apply {
            background = GradientDrawable().apply { setColor(Color.parseColor("#F2F6FF")); cornerRadius = dp(10f).toFloat() }
            rotation = 45f
        }, FrameLayout.LayoutParams(dp(60f), dp(60f), Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply { bottomMargin = -dp(34f) })
        val bubble = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply { setColor(Color.parseColor("#F2F6FF")); cornerRadius = dp(90f).toFloat() }
            setPadding(dp(56f), dp(56f), dp(56f), dp(60f)); elevation = dp(8f).toFloat()
        }
        bubbleChip = tv("", 34f, Color.parseColor("#0B6AA8"), true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.parseColor("#DCEBFF")); cornerRadius = dp(40f).toFloat() }
            setPadding(dp(30f), dp(12f), dp(30f), dp(12f))
        }
        bubble.addView(bubbleChip, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(26f) })
        bubbleText = tv("", 66f, Color.parseColor("#14204A"), true).apply { gravity = Gravity.CENTER; setLineSpacing(0f, 1.2f) }
        bubble.addView(bubbleText, LinearLayout.LayoutParams(-1, -2))
        wrap.addView(bubble, FrameLayout.LayoutParams(-1, -2))
        layer.addView(wrap, FrameLayout.LayoutParams(dp(690f), -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply { topMargin = dp(320f) })

        // 하단 버튼 (테스트용 다음 안내 / 주행 종료)
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER }
        bar.addView(smallPill("다음 안내 (테스트)") { nextTest() }, LinearLayout.LayoutParams(-2, dp(96f)).apply { rightMargin = dp(20f) })
        bar.addView(smallPill("주행 종료") { stopDriving() }, LinearLayout.LayoutParams(-2, dp(96f)))
        layer.addView(bar, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = dp(36f) })
    }

    private fun smallPill(label: String, onClick: () -> Unit) = tv(label, 36f, Color.parseColor("#0F1B3D"), true).apply {
        gravity = Gravity.CENTER; setPadding(dp(40f), 0, dp(40f), 0)
        background = GradientDrawable().apply { setColor(Color.parseColor("#E6FFFFFF")); cornerRadius = dp(48f).toFloat() }
        elevation = dp(6f).toFloat(); isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    // ───── 동작 ─────
    private fun startDriving() {
        if (driving) return
        driving = true
        show("", "좋아!\n지금부터\n연습 주행을\n시작해볼까?")
        activeLayer.visibility = View.VISIBLE
        activeLayer.animate().alpha(1f).setDuration(500).start()
        idleLayer.animate().alpha(0f).setDuration(500).withEndAction { idleLayer.visibility = View.GONE }.start()
    }

    private fun stopDriving() {
        driving = false
        tts?.let { try { it.stop() } catch (_: Exception) {} }
        idleLayer.visibility = View.VISIBLE
        idleLayer.animate().alpha(1f).setDuration(500).start()
        activeLayer.animate().alpha(0f).setDuration(500).withEndAction { activeLayer.visibility = View.GONE }.start()
    }

    private fun nextTest() {
        // 회전·감속·합류 지시는 실제 경로와 연결돼 있지 않아서 테스트 순환에서도 뺀다
        val list = DriveCoach.routeFree
        val c = list[testIndex % list.size]
        testIndex++
        show(c.category, c.text)
    }

    /** 말풍선에 문구를 보여주고 같은 내용을 음성으로도 읽는다. */
    private fun show(category: String, text: String) {
        lastChip = category; lastText = text
        bubbleChip.visibility = if (category.isEmpty()) View.GONE else View.VISIBLE
        bubbleChip.text = category
        bubbleText.text = text.replace("! ", "!\n")
        speak(text.replace("\n", " "))
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(applicationContext, TtsMode.ON_DEVICE).also { t ->
                t.initialize(object : TtsInit {
                    override fun onSuccess() {
                        try { t.setLocale(java.util.Locale.KOREA); t.setSpeakerType(VOICE) } catch (e: Exception) { Log.e(TAG, "speaker ${e.message}") }
                        ttsReady = true
                        pendingSpeak?.let { p -> pendingSpeak = null; runOnUiThread { speak(p) } }
                    }
                    override fun onFailure(errorType: TtsError) { Log.e(TAG, "TTS fail $errorType") }
                })
            }
        } catch (e: Exception) { Log.e(TAG, "TTS 예외 ${e.message}") }
    }

    private fun speak(text: String) {
        if (!ttsReady) { pendingSpeak = text; return }
        try { tts?.speak(text, true) } catch (e: Exception) { Log.e(TAG, "speak ${e.message}") }
    }

    override fun onDestroy() {
        try { tts?.release() } catch (_: Exception) {}
        super.onDestroy()
    }

    /** 이미지의 위·아래·좌우 가장자리를 투명하게 흐려서 배경에 자연스럽게 섞는다. */
    private class FadeEdgeImageView(ctx: android.content.Context, val top: Float, val bottom: Float, val side: Float) :
        ImageView(ctx) {
        private val mask = android.graphics.Paint().apply { xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN) }
        override fun onDraw(canvas: android.graphics.Canvas) {
            val sc = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
            super.onDraw(canvas)
            fun fade(x0: Float, y0: Float, x1: Float, y1: Float) {
                mask.shader = android.graphics.LinearGradient(x0, y0, x1, y1, intArrayOf(0, -1), null, android.graphics.Shader.TileMode.CLAMP)
            }
            val w = width.toFloat(); val h = height.toFloat()
            if (side > 0f) {
                fade(0f, 0f, w * side, 0f); canvas.drawRect(0f, 0f, w * side, h, clearOutside(mask))
                fade(w, 0f, w * (1 - side), 0f); canvas.drawRect(w * (1 - side), 0f, w, h, clearOutside(mask))
            }
            if (top > 0f) { fade(0f, 0f, 0f, h * top); canvas.drawRect(0f, 0f, w, h * top, clearOutside(mask)) }
            if (bottom > 0f) { fade(0f, h, 0f, h * (1 - bottom)); canvas.drawRect(0f, h * (1 - bottom), w, h, clearOutside(mask)) }
            mask.shader = null
            canvas.restoreToCount(sc)
        }
        private fun clearOutside(p: android.graphics.Paint) = p
    }

    companion object {
        private const val TAG = "DriveActivity"
        private const val VOICE = "EUNWOO"
        const val EXTRA_OPEN_NAVI = "open_navi"
        private const val NAVI_PKG = "ai.umos.maps.android.navigation.app"
    }
}
