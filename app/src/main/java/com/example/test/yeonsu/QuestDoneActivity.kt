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
import android.widget.Toast
import android.widget.TextView
import android.graphics.Typeface
import android.graphics.text.LineBreakConfig
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import androidx.core.widget.TextViewCompat
import com.example.test.R
/**
 * 연저씨 · 퀘스트 완료.
 * 디자인 시안(UI image/운전퀘스트완료_정렬수정.png)을 화면에 꽉 채워 띄우고,
 * 그림 속 버튼 위치마다 투명한 터치 영역을 얹는다.
 */
class QuestDoneActivity : FadeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val img = ImageView(this).apply {
            setImageResource(R.drawable.yeonsu_quest_done)
            scaleType = ImageView.ScaleType.MATRIX
        }
        // 연저씨 표정: 표정 시트 6장 중 무작위 (배경 이미지에서는 캐릭터를 지워 둠)
        val faceIdx = savedInstanceState?.getInt("face") ?: FACES.indices.random()
        faceIndex = faceIdx
        val face = ImageView(this).apply {
            setImageResource(FACES[faceIdx])
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val root = FrameLayout(this).apply {
            // 배경은 차량 퀘스트 목록(출발 준비) 화면과 같은 그림, 그 위에 버튼·배지만 있는 투명 이미지를 얹음
            background = androidx.core.content.ContextCompat.getDrawable(this@QuestDoneActivity, R.drawable.yeonsu_list_bg)
            addView(img, FrameLayout.LayoutParams(-1, -1))
            addView(face, FrameLayout.LayoutParams(0, 0))
        }
        val spots = listOf(
            Triple(RectF(44f, 38f, 122f, 116f), "뒤로") { goTo(QuestListActivity::class.java) },
            Triple(RectF(1161f, 38f, 1241f, 116f), "홈") { goTo(HomeActivity::class.java) },
            Triple(RectF(1264f, 38f, 1344f, 116f), "설정") { soon("설정") },
            Triple(RectF(1366f, 38f, 1446f, 116f), "도움말") { soon("도움말") },
            Triple(RectF(848f, 700f, 1256f, 814f), "다른 퀘스트") { goTo(QuestListActivity::class.java) },
        ).map { (rect, name, action) ->
            val v = View(this).apply {
                isClickable = true; isFocusable = true; contentDescription = name
                background = GradientDrawable().apply { setColor(Color.TRANSPARENT) }
                setOnClickListener { action() }
                setOnTouchListener { view, e ->
                    val bg = view.background as GradientDrawable
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> bg.setColor(Color.parseColor("#22000000"))
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> bg.setColor(Color.TRANSPARENT)
                    }
                    false
                }
            }
            root.addView(v, FrameLayout.LayoutParams(0, 0))
            rect to v
        }
        // 퀘스트마다 달라지는 문구 (이미지에서는 이 자리 글자를 지워 둠)
        val q = QuestSession.selected()
        val pick = Math.floorMod(q.id.hashCode(), 2)
        val descText = (q.tip ?: COMMON_DESC[pick]).let { d ->
            // 단어 중간에서 줄이 바뀌지 않게, 가운데에 가장 가까운 띄어쓰기에서 줄바꿈
            if ('\n' in d) d else d.indices.filter { d[it] == ' ' }.minByOrNull { kotlin.math.abs(it - d.length / 2) }
                ?.let { d.substring(0, it) + "\n" + d.substring(it + 1) } ?: d
        }
        // 제목은 완료할 때마다 무작위 (화면 회전 등으로 다시 만들어져도 유지)
        val titleIdx = savedInstanceState?.getInt("title") ?: TITLES.indices.random()
        titleIndex = titleIdx
        val title = TITLES[titleIdx]
        val texts = listOf(
            Triple(RectF(849f, 380f, 1430f, 480f), 0, title to Color.parseColor("#1D3878")),
            Triple(RectF(849f, 515f, 1400f, 645f), 2, descText to Color.parseColor("#5E6D93")),
        ).map { (rect, lines, content) ->
            val t = TextView(this).apply {
                text = content.first; setTextColor(content.second)
                maxLines = maxOf(lines, 1); gravity = Gravity.CENTER_VERTICAL
                includeFontPadding = false
                if (Build.VERSION.SDK_INT >= 33) lineBreakWordStyle = LineBreakConfig.LINE_BREAK_WORD_STYLE_PHRASE
            }
            root.addView(t, FrameLayout.LayoutParams(0, 0))
            Triple(rect, t, lines)
        }
        setContentView(root)
        img.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val d = img.drawable ?: return@addOnLayoutChangeListener
            if (img.width == 0 || img.height == 0) return@addOnLayoutChangeListener
            // 화면을 빈틈없이 채우는 배율(넘치는 부분은 가운데 기준으로 잘림)
            val iw = d.intrinsicWidth.toFloat(); val ih = d.intrinsicHeight.toFloat()
            val s = maxOf(img.width / iw, img.height / ih)
            val m = Matrix().apply { setScale(s, s); postTranslate((img.width - iw * s) / 2f, (img.height - ih * s) / 2f) }
            img.imageMatrix = m
            val sx = iw / SRC_W; val sy = ih / SRC_H
            spots.forEach { (src, v) ->
                val r = RectF(src.left * sx, src.top * sy, src.right * sx, src.bottom * sy)
                m.mapRect(r)
                val lp = v.layoutParams as FrameLayout.LayoutParams
                val nl = r.left.toInt(); val nt = r.top.toInt(); val nw = r.width().toInt(); val nh = r.height().toInt()
                if (lp.leftMargin != nl || lp.topMargin != nt || lp.width != nw || lp.height != nh) {
                    lp.leftMargin = nl; lp.topMargin = nt; lp.width = nw; lp.height = nh
                    (v.background as GradientDrawable).cornerRadius = minOf(nw, nh) * 0.2f
                    v.post { v.layoutParams = lp }
                }
            }
            run {
                // 캐릭터 자리: 원본 캐릭터처럼 아래 끝이 y=900 선에 맞닿게
                val r = RectF(-60f * sx, 200f * sy, 790f * sx, 1090f * sy)
                m.mapRect(r)
                val lp = face.layoutParams as FrameLayout.LayoutParams
                val nl = r.left.toInt(); val nt = r.top.toInt(); val nw = r.width().toInt(); val nh = r.height().toInt()
                if (lp.leftMargin != nl || lp.topMargin != nt || lp.width != nw || lp.height != nh) {
                    lp.leftMargin = nl; lp.topMargin = nt; lp.width = nw; lp.height = nh
                    face.post { face.layoutParams = lp }
                }
            }
            texts.forEach { (src, t, lines) ->
                val r = RectF(src.left * sx, src.top * sy, src.right * sx, src.bottom * sy)
                m.mapRect(r)
                val lp = t.layoutParams as FrameLayout.LayoutParams
                val nl = r.left.toInt(); val nt = r.top.toInt(); val nw = r.width().toInt(); val nh = r.height().toInt()
                if (lp.leftMargin != nl || lp.topMargin != nt || lp.width != nw || lp.height != nh) {
                    lp.leftMargin = nl; lp.topMargin = nt; lp.width = nw; lp.height = nh
                    // 원본 글자 크기에 맞추고, 길면 칸 안에 들어가도록 줄임
                    // lines 0 = 제목(굵은 큰 글씨, 한 줄)
                    val maxPx = (when (lines) { 0 -> 78f; 2 -> 38f; else -> 27f }) * s * sy
                    if (lines == 0) t.typeface = Typeface.create(Typeface.DEFAULT, 900, false)
                    t.setLineSpacing(0f, 1.5f)
                    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(t,
                        (maxPx * 0.6f).toInt().coerceAtLeast(1), maxPx.toInt().coerceAtLeast(2), 1, TypedValue.COMPLEX_UNIT_PX)
                    t.post {
                        t.layoutParams = lp
                        // 제목·설명의 첫 글자 잉크가 같은 세로선에서 시작하도록 글자 앞 여백만큼 당김
                        if (lines != 1) t.post {
                            val b = android.graphics.Rect()
                            val str = t.text.toString()
                            if (str.isNotEmpty()) { t.paint.getTextBounds(str, 0, 1, b); t.translationX = -b.left.toFloat() }
                        }
                    }
                }
            }
        }
    }
    private var titleIndex = 0
    private var faceIndex = 0
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("title", titleIndex); outState.putInt("face", faceIndex)
    }
    private fun soon(name: String) = Toast.makeText(this, "$name 화면은 준비 중이에요", Toast.LENGTH_SHORT).show()
    companion object {
        /** UI image/연저씨 3D 스티커 포즈 모음.png 를 6장으로 나누고 배경을 지운 것 */
        private val FACES = listOf(
            R.drawable.yeonsu_face_0, R.drawable.yeonsu_face_1, R.drawable.yeonsu_face_2,
            R.drawable.yeonsu_face_3, R.drawable.yeonsu_face_4, R.drawable.yeonsu_face_5,
        )
        /** 완료 제목 (완료할 때마다 무작위) */
        private val TITLES = listOf(
            "좋아, 잘 해냈어!", "제법인데, 자네!", "바로 그거야!",
            "한 걸음 더 왔구먼!", "이제 감 잡았지?", "역시 내 제자구먼!",
        )
        /** 퀘스트 전용 팁이 없을 때 쓰는 공통 문구 (퀘스트마다 둘 중 하나) */
        private val COMMON_DESC = listOf(
            "한 단계 더 익숙해졌어!\n이 느낌 그대로 기억해 두게.",
            "처음엔 다 어려운 법이야.\n벌써 하나 해냈잖아!",
        )
        /** 터치 영역 좌표의 기준 크기 (yeonsu_quest_done.png 원본 픽셀) */
        private const val SRC_W = 1490f
        private const val SRC_H = 1055f // UI image/운전퀘스트완료_정렬수정.png
    }
}
