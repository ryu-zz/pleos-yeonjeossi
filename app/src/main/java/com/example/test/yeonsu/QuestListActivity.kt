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

        // 카테고리 목차 (팀장 브랜치와 같은 모양: 선 아이콘 + 이름, 선택된 칸은 흰 네모)
        val catBar = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        cats.forEachIndexed { i, name ->
            val sel = i == QuestSession.category
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(4f), dp(12f), dp(4f), dp(12f))
                isClickable = true; isFocusable = true
                background = if (sel) rounded(Color.WHITE, 24f) else null
                elevation = if (sel) dp(2f).toFloat() else 0f
                setOnClickListener { if (QuestSession.category != i) { QuestSession.category = i; refresh() } }
            }
            if (sel) selItem = item
            item.addView(catIcon(type, i, 46f), LinearLayout.LayoutParams(dp(46f), dp(46f)))
            item.addView(tv(name, 17f, if (sel) ScreenActivity.NAVY else Color.parseColor("#7D8BA0"), sel).apply {
                gravity = Gravity.CENTER; textAlignment = View.TEXT_ALIGNMENT_CENTER
                maxLines = 1; setPadding(0, dp(8f), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
            catBar.addView(item, LinearLayout.LayoutParams(if (compact) dp(104f) else dp(120f), -2).apply { bottomMargin = dp(4f) })
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
        // 퀘스트를 하고 돌아와도 보던 위치에서 다시 시작한다 (다른 카테고리로 바꾸면 맨 위부터).
        val key = "$type/${QuestSession.category}"
        if (key != listKey) { listKey = key; listScroll = 0 }
        right.addView(ScrollView(this).apply {
            addView(list); isVerticalScrollBarEnabled = false; clipToPadding = false; setPadding(0, dp(4f), 0, dp(8f))
            post { scrollTo(0, listScroll) }
            viewTreeObserver.addOnScrollChangedListener { if (isAttachedToWindow) listScroll = scrollY }
        }, LinearLayout.LayoutParams(-1, 0, 1f))

        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            setPadding(dp(36f), 0, dp(36f), 0)
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
                LinearLayout.LayoutParams(dp(130f), -1).apply { rightMargin = dp(24f) })
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
    companion object { var catScroll = 0; var listScroll = 0; var listKey = "" }
    /** 카테고리 선 아이콘. 모두 같은 굵기, 같은 색(NAVY) 한 가지만 쓴다. */
    private fun catIcon(type: String, i: Int, size: Float) = android.widget.ImageView(this).apply {
        val id = resources.getIdentifier("ic_cat_${if (type == "vehicle") "v" else "d"}$i", "drawable", packageName)
        setImageResource(id); setColorFilter(ScreenActivity.NAVY)
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }
    private fun questCard(i: Int): View {
        val q = QuestData.quest(QuestSession.type, QuestSession.category, i)
        val st = store.state(q).status
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            // 진행 중·완료는 칸 전체를 상태 색으로 칠한다 (팀장 브랜치와 같은 방식)
            background = rounded(if (st == "doing" || st == "done") statusBg(st) else Color.parseColor("#F7F9FE"), 28f, Color.WHITE, 2f)
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
