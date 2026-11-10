package com.example.test.yeonsu
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.view.Gravity
import android.util.TypedValue
import android.graphics.Typeface
import android.widget.Toast
import com.example.test.R
/**
 * 연저씨 · 퀘스트 종류 선택.
 * 디자인 시안(연저씨퀘스트안내_날카로운연저씨.png)을 화면에 꽉 채워 띄우고,
 * 그림 속 버튼 위치마다 투명한 터치 영역을 얹는다.
 */
class QuestTypesActivity : FadeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val img = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_quest_types)
            scaleType = ImageView.ScaleType.MATRIX
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#EAF1FD"))
            addView(img, FrameLayout.LayoutParams(-1, -1))
        }
        // 이미지에 그려진 두 박스 자리에 메인 화면 카드와 같은 모양의 카드를 코드로 얹는다.
        root.clipChildren = false
        val spots = SPECS.map { (rect, spec) ->
            val holder = FrameLayout(this).apply { tag = spec; clipChildren = false; clipToPadding = false }
            root.addView(holder, FrameLayout.LayoutParams(0, 0))
            rect to holder
        }
        // 이미지에 구워진 말풍선은 코드로 새로 그린 말풍선으로 덮는다 (높이를 위로 키우고 글자 굵기를 카드 제목과 맞춤)
        val bubbleHolder = FrameLayout(this)
        root.addView(bubbleHolder, FrameLayout.LayoutParams(0, 0))
        setContentView(root)
        // 위쪽 바(뒤로·연저씨·홈·도움말)는 다른 화면(ScreenActivity)과 똑같이 코드로 그린다
        root.post {
            val k = HeaderBar.scale(root.width)
            root.addView(HeaderBar.build(this, k, HomeActivity::class.java, true, null) { showHelp(root, bubbleHolder, spots.map { it.second }, k) },
                FrameLayout.LayoutParams(-1, (HeaderBar.BAR_H * k).toInt(), android.view.Gravity.TOP))
        }
        img.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val d = img.drawable ?: return@addOnLayoutChangeListener
            if (img.width == 0 || img.height == 0) return@addOnLayoutChangeListener
            // 화면을 빈틈없이 채우는 배율(넘치는 부분은 가운데 기준으로 잘림)
            val iw = d.intrinsicWidth.toFloat(); val ih = d.intrinsicHeight.toFloat()
            val s = maxOf(img.width / iw, img.height / ih)
            val m = Matrix().apply { setScale(s, s); postTranslate((img.width - iw * s) / 2f, (img.height - ih * s) / 2f) }
            img.imageMatrix = m
            val sx = iw / SRC_W; val sy = ih / SRC_H
            run {
                val br = RectF(BUB_L * sx, BUB_T * sy, BUB_R * sx, BUB_B * sy)
                m.mapRect(br)
                val lp = bubbleHolder.layoutParams as FrameLayout.LayoutParams
                val nl = br.left.toInt(); val nt = br.top.toInt(); val nw = br.width().toInt(); val nh = br.height().toInt()
                if (lp.leftMargin != nl || lp.topMargin != nt || lp.width != nw || lp.height != nh) {
                    lp.leftMargin = nl; lp.topMargin = nt; lp.width = nw; lp.height = nh
                    bubbleHolder.post {
                        bubbleHolder.layoutParams = lp
                        bubbleHolder.removeAllViews()
                        buildBubble(bubbleHolder, s * sx)
                    }
                }
            }
            spots.forEach { (src, v) ->
                // 이미지에 그려진 박스와 그림자를 가릴 수 있게 살짝 키운다
                val r = RectF((src.left - PAD) * sx, (src.top - PAD) * sy, (src.right + PAD) * sx, (src.bottom + PAD) * sy)
                m.mapRect(r)
                val lp = v.layoutParams as FrameLayout.LayoutParams
                val nl = r.left.toInt(); val nt = r.top.toInt(); val nw = r.width().toInt(); val nh = r.height().toInt()
                if (lp.leftMargin != nl || lp.topMargin != nt || lp.width != nw || lp.height != nh) {
                    lp.leftMargin = nl; lp.topMargin = nt; lp.width = nw; lp.height = nh
                    v.post {
                        v.layoutParams = lp
                        v.removeAllViews()
                        v.addView(typeCard(v.tag as TypeSpec, HeaderBar.scale(img.width), nw, nh), FrameLayout.LayoutParams(-1, -1))
                    }
                }
            }
        }
    }
    /** 말풍선 몸통과 꼬리. 좌표는 원본 이미지 픽셀(ps = 화면 픽셀/원본 픽셀)이라 어느 해상도에서도 같은 모양이다. */
    private class BubbleView(ctx: android.content.Context, private val ps: Float) : View(ctx) {
        private val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFCFF"); style = android.graphics.Paint.Style.FILL
            setShadowLayer(16f, 0f, 7f, Color.parseColor("#331E4B96"))
        }
        private val path = android.graphics.Path().apply {
            addRoundRect(RectF(BODY_L - BUB_L, BODY_T - BUB_T, BODY_R - BUB_L, BODY_B - BUB_T), 52f, 52f, android.graphics.Path.Direction.CW)
            // 꼬리: 연저씨 쪽(오른쪽 아래)을 가리킨다. 원래 말풍선의 꼬리보다 조금 크게 그려 덮는다.
            moveTo(424f - BUB_L, BODY_B - 8f - BUB_T)
            lineTo(510f - BUB_L, BODY_B - 8f - BUB_T)
            lineTo(470f - BUB_L, 522f - BUB_T)
            close()
        }
        init { setLayerType(LAYER_TYPE_SOFTWARE, null) }
        override fun onDraw(c: android.graphics.Canvas) {
            c.save(); c.scale(ps, ps); c.drawPath(path, paint); c.restore()
        }
    }
    private fun buildBubble(holder: FrameLayout, ps: Float) {
        holder.clipChildren = false; holder.clipToPadding = false
        holder.addView(BubbleView(this, ps), FrameLayout.LayoutParams(-1, -1))
        val first = "자네, 퀘스트에\n"
        val text = android.text.SpannableString(first + "도전해보겠나?").apply {
            // "도전"만 하늘색, 나머지는 진한 파랑
            setSpan(android.text.style.ForegroundColorSpan(Color.parseColor("#2197F5")), first.length, first.length + 2,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        holder.addView(TextView(this).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 52f * ps)
            setTextColor(Color.parseColor("#11286E"))
            // 카드 제목("차량 퀘스트")과 같은 굵기
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            includeFontPadding = false; gravity = Gravity.CENTER
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setLineSpacing(0f, 1.3f)
        }, FrameLayout.LayoutParams(((BODY_R - BODY_L) * ps).toInt(), ((BODY_B - BODY_T) * ps).toInt()).apply {
            leftMargin = ((BODY_L - BUB_L) * ps).toInt(); topMargin = ((BODY_T - BUB_T) * ps).toInt()
        })
    }
    private class TypeSpec(val type: String, val title: String, val desc: String, val icon: Int, val photo: Int, val bg: String)
    /**
     * 메인 화면 카드와 같은 형식: 파스텔 배경 + 흰 테두리, 제목(38)·설명(20), 오른쪽 화살표,
     * 사진은 카드 끝까지 채우고 글자 쪽 가장자리를 배경색으로 서서히 지운다. 글자 크기·간격은 메인과 같은 단위(k)다.
     */
    private fun typeCard(spec: TypeSpec, k: Float, w: Int, h: Int): View {
        fun dp(v: Float) = (v * k).toInt()
        val base = Color.parseColor(spec.bg)
        val card = FrameLayout(this).apply {
            background = GradientDrawable().apply { setColor(base); cornerRadius = dp(34f).toFloat() }
            // 흰 테두리는 사진 위에 그려서 사진 쪽에서도 끊기지 않게 한다
            foreground = GradientDrawable().apply { setColor(Color.TRANSPARENT); cornerRadius = dp(34f).toFloat(); setStroke(dp(3f), Color.WHITE) }
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(v: View, o: android.graphics.Outline) = o.setRoundRect(0, 0, v.width, v.height, dp(34f).toFloat())
            }
            elevation = dp(8f).toFloat()
            isClickable = true; isFocusable = true; contentDescription = spec.title
            setOnClickListener { open(spec.type) }
            setOnTouchListener { view, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> view.alpha = 0.88f
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> view.alpha = 1f
                }
                false
            }
        }
        val photoW = (w * 0.36f).toInt()
        card.addView(ImageView(this).apply {
            setImageResource(spec.photo); scaleType = ImageView.ScaleType.CENTER_CROP
        }, FrameLayout.LayoutParams(photoW, -1, Gravity.END))
        val fadeW = dp(70f)
        card.addView(View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(base, base and 0x00FFFFFF))
        }, FrameLayout.LayoutParams(fadeW, -1, Gravity.END).apply { rightMargin = photoW - fadeW })
        fun tv(text: String, sp: Float, color: Int, bold: Boolean) = TextView(this).apply {
            this.text = text; setTextSize(TypedValue.COMPLEX_UNIT_PX, sp * k); setTextColor(color)
            typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
            includeFontPadding = false
        }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val tx = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        tx.addView(tv(spec.title, 38f, Color.parseColor("#0F1B3D"), true))
        tx.addView(tv(spec.desc, 20f, Color.parseColor("#3A4766"), false).apply { setPadding(0, dp(8f), 0, 0); setLineSpacing(0f, 1.2f) })
        head.addView(tx, LinearLayout.LayoutParams(0, -2, 1f))
        head.addView(ImageView(this).apply { setImageResource(R.drawable.ic_y_chev_right) }, LinearLayout.LayoutParams(dp(30f), dp(30f)))
        card.addView(head, FrameLayout.LayoutParams(-1, -1).apply { setMargins(dp(30f), 0, photoW + dp(14f), 0) })
        return card
    }
    private fun open(type: String) {
        QuestSession.type = type; QuestSession.category = 0; QuestSession.catScroll.clear()
        goTo(QuestListActivity::class.java)
    }
    private var help: HelpOverlay? = null
    /** 퀘스트 종류 도움말: 말풍선과 두 퀘스트 카드가 무엇인지 알려준다. */
    private fun showHelp(root: FrameLayout, bubble: View, cards: List<View>, k: Float) {
        if (help != null) return
        fun r(v: View) = RectF(v.left.toFloat(), v.top.toFloat(), v.right.toFloat(), v.bottom.toFloat())
        val bodies = listOf(
            "차량 조작 퀘스트를 모아둔 곳이에요\n라이트·에어컨·창문 등을 따라해요",
            "카페·마트 같은 일상 운전 상황으로\n연습하는 퀘스트예요",
        )
        val targets = ArrayList<HelpTarget>()
        cards.forEachIndexed { i, c ->
            val spec = c.tag as TypeSpec
            targets.add(HelpTarget(r(c), spec.title, bodies[i], HelpPlace.OVER_MID, 0.5f, arrow = false))
        }
        val o = HelpOverlay(this, k, "퀘스트 종류 화면이에요", "화면을 누르면 닫혀요", targets) { help = null }
        help = o
        root.addView(o, FrameLayout.LayoutParams(-1, -1))
        o.elevation = 100f * k
    }
    private fun soon(name: String) = Toast.makeText(this, "$name 화면은 준비 중이에요", Toast.LENGTH_SHORT).show()
    companion object {
        /** 터치 영역 좌표의 기준 크기 (yeonsu_quest_types.png 원본 픽셀) */
        private const val SRC_W = 1536f
        private const val SRC_H = 1110f
        /** 카드를 키우는 양 (원본 픽셀). 이미지의 옛 박스는 지워 두었으므로 0 */
        private const val PAD = 0f
        // 말풍선 (원본 픽셀). 몸통은 원래 y 300~472 → 위로 높이를 늘려 BODY_T=252
        private const val BODY_L = 96f
        private const val BODY_R = 584f
        private const val BODY_T = 252f
        private const val BODY_B = 475f
        // 그림자·꼬리까지 담는 전체 영역
        private const val BUB_L = 70f
        private const val BUB_R = 620f
        private const val BUB_T = 222f
        private const val BUB_B = 540f
        private val SPECS = listOf(
            RectF(650f, 318f, 1431f, 582f) to TypeSpec("vehicle", "차량 퀘스트", "차 기본 조작을 까먹었다고?\n차근차근 따라해보자!",
                R.drawable.ic_y_car, R.drawable.types_photo_vehicle, "#DEE7FC"),
            RectF(650f, 634f, 1431f, 898f) to TypeSpec("daily", "일상 퀘스트", "연저씨가 옆에서\n하나하나 다 알려줄게",
                R.drawable.ic_y_quest, R.drawable.types_photo_daily, "#E5F0F1"),
        )
    }
}
