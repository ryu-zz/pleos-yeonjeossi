package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
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

    /** 모든 퀘스트 화면은 같은 그라데이션 빛 무늬 배경. */
    override fun screenBackground(): android.graphics.drawable.Drawable = questBackground()
    /** 차량 퀘스트에서만: 전기차 모드 켜기/끄기 버튼. 켜면 왼쪽 카테고리에 "전기차"가 나온다. */
    override fun headerExtra(): View? {
        if (QuestSession.type != "vehicle") return null
        val on = EvMode.isOn(this)
        return tv(if (on) "전기차 ON" else "전기차 OFF", 20f, if (on) Color.WHITE else ScreenActivity.NAVY, true).apply {
            gravity = Gravity.CENTER; setPadding(dp(22f), 0, dp(22f), 0)
            background = rounded(if (on) Color.parseColor("#2D7653") else Color.WHITE, 16f)
            elevation = dp(4f).toFloat(); isClickable = true; isFocusable = true
            setOnClickListener {
                EvMode.set(this@QuestListActivity, !on)
                if (on && QuestSession.category >= EvMode.cats(this@QuestListActivity, "vehicle").size) QuestSession.category = 0
                refresh()
            }
        }
    }
    private var catPanel: View? = null
    private var listPanel: View? = null
    override fun helpHeadline() = if (QuestSession.type == "daily") "일상 퀘스트 화면이에요" else "차량 퀘스트 화면이에요"
    override fun helpItems(): List<HelpItem>? {
        val c = catPanel ?: return null; val l = listPanel ?: return null
        return if (QuestSession.type == "vehicle") listOf(
            HelpItem(c, "카테고리 ${EvMode.cats(this, "vehicle").size}개", "출발 준비부터 ${EvMode.cats(this, "vehicle").last()}까지\n주제별로 나뉘어 있어요", HelpPlace.RIGHT, 0.22f),
            HelpItem(l, "차량 퀘스트", "각 카테고리 안의 다양한 퀘스트를 깨볼 수 있어요\n한 카테고리를 다 깨면 뱃지를 받을 수 있어요", HelpPlace.OVER_MID, 0.62f),
        ) else listOf(
            HelpItem(c, "카테고리 9개", "차 관리부터 도전까지\n상황별로 나뉘어 있어요", HelpPlace.RIGHT, 0.22f),
            HelpItem(l, "일상 퀘스트", "각 카테고리 안의 상황별 퀘스트를 깨볼 수 있어요\n맨 위 퀘스트는 시나리오를 보고 내비 안내까지 해요", HelpPlace.OVER_MID, 0.62f),
        )
    }

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val type = QuestSession.type
        val green = type == "daily"
        val cats = EvMode.cats(this, type)
        if (QuestSession.category >= cats.size) QuestSession.category = 0

        // 카테고리: 같은 굵기·같은 색의 선 아이콘 + 가운데 정렬된 이름
        val catBar = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val scrollKey = type + if (compact) "-x" else "-y"
        val right = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        /** 오른쪽(제목 + 퀘스트 카드)을 현재 카테고리로 채운다. */
        fun fillRight() {
            right.removeAllViews()
            val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            titleRow.addView(tv(cats[QuestSession.category], 40f, ScreenActivity.NAVY, true), LinearLayout.LayoutParams(0, -2, 1f))
            titleRow.addView(tv(if (green) "일상 퀘스트" else "차량 퀘스트", 20f, Color.parseColor("#76859A")))
            right.addView(titleRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18f); topMargin = dp(4f) })
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            QuestData.names(type, QuestSession.category).forEachIndexed { i, _ ->
                list.addView(questCard(i), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16f) })
            }
            right.addView(ScrollView(this).apply { addView(list); isVerticalScrollBarEnabled = false }, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        val items = ArrayList<Pair<LinearLayout, android.widget.TextView>>()
        fun style(i: Int, sel: Boolean) {
            val (item, label) = items[i]
            item.background = if (sel) rounded(Color.WHITE, 24f) else null
            item.elevation = if (sel) dp(2f).toFloat() else 0f
            label.setTextColor(if (sel) ScreenActivity.NAVY else Color.parseColor("#7D8BA0"))
            label.typeface = android.graphics.Typeface.create("sans-serif", if (sel) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        cats.forEachIndexed { i, name ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(4f), dp(12f), dp(4f), dp(12f))
                isClickable = true; isFocusable = true
            }
            item.addView(catIcon(type, i, 46f), LinearLayout.LayoutParams(dp(46f), dp(46f)))
            val label = tv(name, 17f, ScreenActivity.NAVY).apply {
                gravity = Gravity.CENTER; textAlignment = View.TEXT_ALIGNMENT_CENTER
                maxLines = 1; setPadding(0, dp(8f), 0, 0)
            }
            item.addView(label, LinearLayout.LayoutParams(-1, -2))
            items.add(item to label)
            catBar.addView(item, LinearLayout.LayoutParams(if (compact) dp(104f) else dp(120f), -2).apply { bottomMargin = dp(4f) })
        }
        items.indices.forEach { style(it, it == QuestSession.category) }
        fillRight()
        // 카테고리를 눌러도 화면 전체를 다시 그리지 않는다. 선택 표시만 바꾸고 오른쪽 목록만 부드럽게 교체한다.
        items.forEachIndexed { i, (item, _) ->
            item.setOnClickListener {
                val old = QuestSession.category
                if (old == i) return@setOnClickListener
                QuestSession.category = i
                style(old, false); style(i, true)
                right.animate().cancel()
                right.animate().alpha(0f).setDuration(100).withEndAction {
                    fillRight()
                    right.animate().alpha(1f).setDuration(180).start()
                }.start()
            }
        }
        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            setPadding(dp(36f), 0, dp(36f), dp(24f))
        }
        if (compact) {
            root.addView(HorizontalScrollView(this).apply {
                addView(catBar); isHorizontalScrollBarEnabled = false
                restoreScroll(this, scrollKey, true)
            },
                LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10f) })
            root.addView(right, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            val catScroll = ScrollView(this).apply {
                addView(catBar); isVerticalScrollBarEnabled = false
                restoreScroll(this, scrollKey, false)
            }
            catPanel = catScroll
            root.addView(catScroll,
                LinearLayout.LayoutParams(dp(130f), -1).apply { rightMargin = dp(24f) })
            root.addView(right, LinearLayout.LayoutParams(0, -1, 1f))
        }
        listPanel = right
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    private fun questCard(i: Int): View {
        val q = QuestData.quest(QuestSession.type, QuestSession.category, i)
        val st = store.state(q).status
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(statusBg(st), 30f, Color.WHITE, 3f)
            setPadding(dp(24f), dp(20f), dp(24f), dp(20f))
            minimumHeight = dp(130f); elevation = dp(2f).toFloat()
            isClickable = true; isFocusable = true
            setOnClickListener {
                // 차량 신호로 인식하지 못하는 차량 퀘스트는 아직 구현 전이다
                if (QuestSession.type == "vehicle" && QuestSignals.find(q.name) == null && QuestEvents.find(q.name) == null) { showBanner("구현 예정..."); return@setOnClickListener }
                QuestSession.questIndex = i
                // 일상 퀘스트의 카테고리 맨 위 퀘스트는 시나리오부터 보여준다
                if (QuestSession.type == "daily" && i == 0 && QuestData.flows.containsKey(QuestSession.category)) {
                    QuestFlow.start(); goTo(ScenarioActivity::class.java)
                } else goTo(QuestDetailActivity::class.java)
            }
        }
        val imgName = if (QuestSession.type == "vehicle") "quest_img_${q.c}_$i" else ""
        val imgId = if (imgName.isEmpty()) 0 else resources.getIdentifier(imgName, "drawable", packageName)
        if (imgId != 0) {
            row.addView(android.widget.ImageView(this).apply {
                setImageResource(imgId); scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: View, o: android.graphics.Outline) { o.setRoundRect(0, 0, v.width, v.height, dp(18f).toFloat()) }
                }
            }, LinearLayout.LayoutParams(dp(180f), dp(108f)).apply { rightMargin = dp(20f) })
        } else {
            row.addView(FrameLayout(this).apply {
            background = rounded(Color.parseColor("#99FFFFFF"), 22f)
            addView(catIcon(QuestSession.type, QuestSession.category, 52f), FrameLayout.LayoutParams(dp(52f), dp(52f), Gravity.CENTER))
        }, LinearLayout.LayoutParams(dp(100f), dp(100f)).apply { rightMargin = dp(20f) })
        }
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(tv(q.name, 28f, ScreenActivity.NAVY, true))
        text.addView(tv(q.desc, 18f, ScreenActivity.GRAY).apply { setPadding(0, dp(8f), 0, 0) })
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(tv(statusLabel(st), 18f, statusColor(st), true).apply {
            background = rounded(Color.WHITE, 24f); setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(12f) })
        row.addView(tv("›", 44f, Color.parseColor("#4A5B7A")).apply { setPadding(dp(12f), 0, 0, 0) })
        return row
    }

    /** 카테고리 선 아이콘. 모두 같은 굵기, 같은 색(NAVY) 한 가지만 쓴다. */
    private fun catIcon(type: String, i: Int, size: Float) = android.widget.ImageView(this).apply {
        val id = resources.getIdentifier("ic_cat_${if (type == "vehicle") "v" else "d"}$i", "drawable", packageName)
        setImageResource(id); setColorFilter(ScreenActivity.NAVY)
        scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
    }
    /** 카테고리를 눌러 화면이 다시 그려져도 스크롤이 맨 위로 돌아가지 않게, 위치를 기억하고 되돌린다. */
    private fun restoreScroll(v: View, key: String, horizontal: Boolean) {
        v.setOnScrollChangeListener { _, x, y, _, _ -> QuestSession.catScroll[key] = if (horizontal) x else y }
        val pos = QuestSession.catScroll[key] ?: 0
        if (pos > 0) v.post { if (horizontal) v.scrollTo(pos, 0) else v.scrollTo(0, pos) }
    }
}
