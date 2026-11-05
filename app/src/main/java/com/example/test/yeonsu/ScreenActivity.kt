package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.test.R

/**
 * 연저씨 · 앱 화면의 공통 틀 (퀘스트/마이페이지 등 HTML에서 옮긴 화면이 상속한다).
 * - 상단: 왼쪽 끝 [뒤로 + 연저씨 마크], 오른쪽 끝 [홈·설정·도움말] (창 전체 폭 기준)
 * - 창이 넓으면(2/3) 가로형, 좁으면(1/3) compact=true. 창 크기가 바뀌면 자동으로 다시 그린다.
 * - FadeActivity를 상속하므로 화면이 열릴 때 페이드인된다.
 */
abstract class ScreenActivity : FadeActivity() {
    protected var k = 1f
    protected var compact = false
    protected fun dp(v: Float) = (v * k).toInt()

    /** 뒤로 가기(‹)로 돌아갈 화면 */
    protected abstract val parentScreen: Class<out Activity>
    /** 헤더 아래 영역(content)에 화면을 그린다. w, h는 content의 크기. */
    protected abstract fun build(content: FrameLayout, w: Int, h: Int)

    private lateinit var root: FrameLayout
    private var bw = 0
    private var bh = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this).apply { background = StartActivity.skyBackground() }
        setContentView(root)
        root.addOnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
            val w = r - l; val h = b - t
            if (w > 0 && h > 0 && (w != bw || h != bh)) { bw = w; bh = h; rebuild() }
        }
    }

    /** 상태(카테고리, 진행 단계 등)가 바뀌었을 때 화면을 다시 그린다. */
    protected fun refresh() { if (bw > 0) rebuild() }

    private fun rebuild() {
        compact = bw < bh * 0.9f
        k = if (compact) bw / 841f * 1.3f else bw / 1672f * 1.6f
        root.removeAllViews()
        val headerH = dp(96f)
        val content = FrameLayout(this)
        // 화면 가장자리에서 그림자·모서리가 잘리지 않도록 좌우를 안쪽으로 당긴다.
        val sideL = dp(12f); val sideR = dp(30f)
        root.addView(content, FrameLayout.LayoutParams(-1, -1).apply { topMargin = headerH; leftMargin = sideL; rightMargin = sideR })
        root.addView(buildHeader(headerH), FrameLayout.LayoutParams(-1, headerH, Gravity.TOP).apply { leftMargin = sideL; rightMargin = sideR })
        build(content, bw - sideL - sideR, bh - headerH)
        unclip(root)
    }

    /** 부모가 자식의 그림자·모서리를 잘라내지 않도록 모든 하위 뷰의 잘라내기를 끈다(나중에 추가되는 뷰 포함). */
    private val unclipListener = object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: View?, child: View?) { child?.let { unclip(it) } }
        override fun onChildViewRemoved(parent: View?, child: View?) {}
    }
    private fun unclip(v: View) {
        if (v !is ViewGroup) return
        v.clipChildren = false
        if (v !is android.widget.ScrollView && v !is android.widget.HorizontalScrollView) v.clipToPadding = false
        v.setOnHierarchyChangeListener(unclipListener)
        for (i in 0 until v.childCount) unclip(v.getChildAt(i))
    }
    private fun buildHeader(h: Int): View {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(36f), dp(8f), dp(32f), 0)
        }
        row.addView(iconButton(R.drawable.ic_y_chev_left, 56f) { goTo(parentScreen) },
            LinearLayout.LayoutParams(dp(56f), dp(56f)).apply { rightMargin = dp(14f) })
        row.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_wheel) }, LinearLayout.LayoutParams(dp(40f), dp(40f)))
        row.addView(tv("연저씨", 30f, NAVY, true).apply { setPadding(dp(12f), 0, 0, 0) },
            LinearLayout.LayoutParams(0, -2, 1f))
        listOf(R.drawable.ic_y_home to "홈", R.drawable.ic_y_settings to "설정", R.drawable.ic_y_help to "도움말").forEach { (ic, name) ->
            row.addView(iconButton(ic, 56f) {
                if (name == "홈") goTo(HomeActivity::class.java)
                else if (name == "도움말" && helpItems() != null) showHelp()
                else Toast.makeText(this, "$name 화면은 준비 중이에요", Toast.LENGTH_SHORT).show()
            }, LinearLayout.LayoutParams(dp(56f), dp(56f)).apply { leftMargin = dp(12f) })
        }
        return row
    }

    // ───── 도움말 ─────
    /** 도움말에서 가리킬 항목. 화면마다 override 한다. null이면 "준비 중" 안내만 보여준다. */
    protected class HelpItem(val view: View, val title: String, val body: String, val place: HelpPlace = HelpPlace.OVER_MID, val frac: Float = 0.5f)
    protected open fun helpHeadline(): String = "도움말"
    protected open fun helpItems(): List<HelpItem>? = null
    private var helpOverlay: HelpOverlay? = null
    private fun showHelp() {
        if (helpOverlay != null) return
        val items = helpItems() ?: return
        val rl = IntArray(2); root.getLocationInWindow(rl)
        val targets = items.map { it2 ->
            val l = IntArray(2); it2.view.getLocationInWindow(l)
            val x = (l[0] - rl[0]).toFloat(); val y = (l[1] - rl[1]).toFloat()
            // 좁은 화면(1/3)에서는 왼쪽 공간이 없으므로 카드 위쪽에 놓는다
            val place = if (compact && (it2.place == HelpPlace.LEFT || it2.place == HelpPlace.RIGHT || it2.place == HelpPlace.BELOW)) HelpPlace.ABOVE else it2.place
            HelpTarget(RectF(x, y, x + it2.view.width, y + it2.view.height), it2.title, it2.body, place, it2.frac)
        }
        val o = HelpOverlay(this, k, helpHeadline(), "화면을 누르면 닫혀요", targets) { helpOverlay = null }
        helpOverlay = o
        root.addView(o, FrameLayout.LayoutParams(-1, -1))
        o.elevation = dp(100f).toFloat()
    }

    // ───── 공통 부품 ─────
    protected fun tv(text: CharSequence, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_PX, size * k); setTextColor(color)
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
    }

    protected fun iconButton(icon: Int, size: Float, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(icon)
        val p = dp(size * 0.26f); setPadding(p, p, p, p)
        background = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(16f).toFloat() }
        elevation = dp(4f).toFloat()
        isClickable = true; isFocusable = true
        setOnClickListener { onClick() }
    }

    protected fun rounded(color: Int, radius: Float, stroke: Int = 0, strokeWidth: Float = 3f) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(strokeWidth), stroke)
    }

    protected fun gradientBox(c1: String, c2: String, radius: Float) = GradientDrawable(GradientDrawable.Orientation.TL_BR,
        intArrayOf(Color.parseColor(c1), Color.parseColor(c2))).apply {
        cornerRadius = dp(radius).toFloat(); setStroke(dp(4f), Color.WHITE)
    }

    /** 큰 버튼. primary=청록 그라데이션(시작하기 버튼과 같은 색), 아니면 흰 버튼. */
    protected fun button(label: String, primary: Boolean = true, enabled: Boolean = true, onClick: () -> Unit) =
        tv(label, 26f, if (primary) Color.WHITE else NAVY, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(44f), dp(22f), dp(44f), dp(22f))
            background = if (primary) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#12A3A8"), Color.parseColor("#0B6AA8"))).apply { cornerRadius = dp(40f).toFloat() }
            else rounded(Color.WHITE, 40f)
            elevation = dp(4f).toFloat()
            alpha = if (enabled) 1f else 0.4f
            isClickable = enabled; isFocusable = enabled
            if (enabled) setOnClickListener { onClick() }
        }

    protected fun pose(index: Int) = ImageView(this).apply {
        setImageResource(when (index) { 0 -> R.drawable.yeonsu_pose0; 1 -> R.drawable.yeonsu_pose1; else -> R.drawable.yeonsu_pose2 })
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    /** 말풍선. 두 번째 줄(strong)은 파랑으로 강조한다. */
    protected fun bubble(first: String, strong: String, size: Float): TextView {
        val s = SpannableString("$first\n$strong")
        s.setSpan(ForegroundColorSpan(Color.parseColor("#2363B4")), first.length + 1, s.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return tv(s, size, NAVY, true).apply {
            background = rounded(Color.parseColor("#EDFFFFFF"), 34f, Color.WHITE, 3f)
            setPadding(dp(36f), dp(26f), dp(36f), dp(26f))
            setLineSpacing(0f, 1.25f); elevation = dp(4f).toFloat()
        }
    }

    protected fun statusLabel(status: String) = when (status) { "doing" -> "진행 중"; "done" -> "완료"; else -> "미완료" }
    protected fun statusColor(status: String) = Color.parseColor(when (status) { "doing" -> "#2E66AA"; "done" -> "#32815C"; else -> "#6D7B8F" })
    protected fun statusBg(status: String) = Color.parseColor(when (status) { "doing" -> "#DCEAFA"; "done" -> "#DCEFE2"; else -> "#EDF0F6" })

    companion object {
        val NAVY = Color.parseColor("#0F1B3D")
        val GRAY = Color.parseColor("#66728A")
    }
}
