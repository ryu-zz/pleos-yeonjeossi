package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView

/** 연저씨 · 퀘스트 카테고리와 목록 (HTML list.js). */
class QuestListActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestTypesActivity::class.java
    private val store by lazy { QuestStore(this) }
    /** 디자인 시안(연저씨 차량 퀘스트 화면.png)과 같은 밝은 빛 무늬 배경 */
    override fun screenBackground(): android.graphics.drawable.Drawable =
        androidx.core.content.ContextCompat.getDrawable(this, com.example.test.R.drawable.yeonsu_list_bg)!!

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val type = QuestSession.type
        val green = type == "daily"
        val cats = QuestData.cats.getValue(type)
        selItem = null

        // 카테고리 (동그라미 + 이름)
        val catBar = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }
        cats.forEachIndexed { i, name ->
            val sel = i == QuestSession.category
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(4f), dp(10f), dp(4f), dp(10f))
                isClickable = true; isFocusable = true
                setOnClickListener { QuestSession.category = i; refresh() }
            }
            if (sel) selItem = item
            val selColors = if (green) intArrayOf(Color.parseColor("#5FB08A"), Color.parseColor("#2F7A57"))
                else intArrayOf(Color.parseColor("#5B97EC"), Color.parseColor("#1F5FC4"))
            val selText = Color.parseColor(if (green) "#2F7A57" else "#1F5FC4")
            item.addView(tv(QuestData.symbol(type, i), 28f, if (sel) Color.WHITE else Color.parseColor("#8A97AB")).apply {
                gravity = Gravity.CENTER
                background = (if (sel) GradientDrawable(GradientDrawable.Orientation.TL_BR, selColors) else GradientDrawable().apply {
                    setColor(Color.parseColor("#F1F5FB"))
                }).apply { shape = GradientDrawable.OVAL; setStroke(dp(2f), Color.WHITE) }
                elevation = dp(if (sel) 4f else 2f).toFloat()
            }, LinearLayout.LayoutParams(dp(66f), dp(66f)))
            item.addView(tv(name, 15f, if (sel) selText else Color.parseColor("#5E6C82"), sel).apply {
                setPadding(0, dp(8f), 0, 0); gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(-1, -2))
            if (sel) item.addView(View(this).apply {
                background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, selColors).apply { cornerRadius = dp(3f).toFloat() }
            }, LinearLayout.LayoutParams(dp(72f), dp(5f)).apply { topMargin = dp(8f) })
            catBar.addView(item, LinearLayout.LayoutParams(if (compact) dp(104f) else dp(150f), -2))
        }

        // 퀘스트 목록
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(tv(cats[QuestSession.category], 44f, ScreenActivity.NAVY, true), LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(tv(if (green) "일상 퀘스트" else "차량 퀘스트", 19f, Color.parseColor("#6F7D93"), true))
        right.addView(titleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20f); topMargin = dp(2f) })
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        QuestData.names(type, QuestSession.category).forEachIndexed { i, _ ->
            list.addView(questCard(i), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20f); leftMargin = dp(2f); rightMargin = dp(4f) })
        }
        right.addView(ScrollView(this).apply { addView(list); isVerticalScrollBarEnabled = false; clipToPadding = false; setPadding(0, dp(4f), 0, dp(8f)) }, LinearLayout.LayoutParams(-1, 0, 1f))

        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            setPadding(dp(if (compact) 36f else 20f), 0, dp(36f), 0)
        }
        if (compact) {
            root.addView(HorizontalScrollView(this).apply {
                addView(catBar); isHorizontalScrollBarEnabled = false
                keepScroll(this, { scrollX }) { scrollTo(it, 0) }
                post { selItem?.let { smoothScrollTo(it.left + it.width / 2 - width / 2, 0) } }
            },
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10f) })
            root.addView(right, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            root.addView(ScrollView(this).apply {
                addView(catBar); isVerticalScrollBarEnabled = false
                keepScroll(this, { scrollY }) { scrollTo(0, it) }
                post { selItem?.let { smoothScrollTo(0, it.top + it.height / 2 - height / 2) } }
            },
                LinearLayout.LayoutParams(dp(150f), -1))
            // 카테고리와 목록 사이 얇은 구분선
            root.addView(View(this).apply { setBackgroundColor(Color.parseColor("#D5E0F0")) },
                LinearLayout.LayoutParams(dp(2f), -1).apply { leftMargin = dp(8f); rightMargin = dp(24f); bottomMargin = dp(24f) })
            root.addView(right, LinearLayout.LayoutParams(0, -1, 1f))
        }
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    /** 카테고리 바의 스크롤 위치를 기억해 두었다가, 다시 그릴 때 그 위치에서 시작한다 (이후 선택 항목을 가운데로 부드럽게 옮김). */
    private fun keepScroll(v: View, get: () -> Int, set: (Int) -> Unit) {
        v.post { set(catScroll) }
        v.viewTreeObserver.addOnScrollChangedListener { if (v.isAttachedToWindow) catScroll = get() }
    }
    /** 선택된 카테고리 항목 (스크롤 가운데로 옮길 대상) */
    private var selItem: View? = null
    companion object { var catScroll = 0 }
    private fun questCard(i: Int): View {
        val q = QuestData.quest(QuestSession.type, QuestSession.category, i)
        val st = store.state(q).status
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.parseColor("#F7F9FE"), 28f, Color.WHITE, 2f)
            setPadding(dp(22f), dp(16f), dp(22f), dp(16f))
            minimumHeight = dp(130f); elevation = dp(3f).toFloat()
            isClickable = true; isFocusable = true
            setOnClickListener { QuestSession.questIndex = i; goTo(QuestDetailActivity::class.java) }
        }
        val imgId = resources.getIdentifier("quest_img_${q.c}_$i", "drawable", packageName)
        if (QuestSession.type == "vehicle" && imgId != 0) {
            row.addView(android.widget.ImageView(this).apply {
                setImageResource(imgId); scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: View, o: android.graphics.Outline) { o.setRoundRect(0, 0, v.width, v.height, dp(12f).toFloat()) }
                }
            }, LinearLayout.LayoutParams(dp(192f), dp(106f)).apply { rightMargin = dp(24f) })
        } else {
            row.addView(tv(QuestData.symbol(QuestSession.type, QuestSession.category), 40f, ScreenActivity.NAVY).apply {
                gravity = Gravity.CENTER; background = rounded(Color.parseColor("#99FFFFFF"), 22f)
            }, LinearLayout.LayoutParams(dp(100f), dp(100f)).apply { rightMargin = dp(20f) })
        }
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(tv(q.name, 27f, ScreenActivity.NAVY, true))
        text.addView(tv(q.desc, 17f, Color.parseColor("#4F5D74")).apply { setPadding(0, dp(10f), 0, 0) })
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(tv(statusLabel(st), 16f, statusColor(st), true).apply {
            background = rounded(if (st != "doing" && st != "done") Color.WHITE else statusBg(st), 24f)
            elevation = dp(1f).toFloat(); setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(12f) })
        row.addView(tv("›", 44f, Color.parseColor("#2A3A5C"), true).apply { setPadding(dp(14f), 0, dp(4f), 0) })
        return row
    }
}
