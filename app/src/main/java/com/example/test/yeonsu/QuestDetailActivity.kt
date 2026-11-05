package com.example.test.yeonsu

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.example.test.yeonsu.signal.TurnSignalQuestController

/**
 * 연저씨 · 퀘스트 상세/진행/완료 (HTML detail.js, success).
 *
 * 두 가지 완료 경로가 있다.
 * 1) 미리보기 경로(대부분 퀘스트): 아래쪽 "동작을 마쳤다고 치기" 버튼으로 완료. 실제 차량 조작을 판정하지 않는다.
 * 2) 차량 신호 경로(왼쪽 방향지시등, id=vehicle-1-0): Vehicle SDK 의 방향지시등 신호
 *    OFF→LEFT→OFF 를 확인해야만 완료된다. 이 경로에서는 "동작을 마쳤다고 치기" 버튼을 제공하지 않는다.
 */
class QuestDetailActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestListActivity::class.java
    private val store by lazy { QuestStore(this) }
    private var success = false

    /** 차량 신호로 판정하는 퀘스트인지(왼쪽/오른쪽 방향지시등, 끄기). */
    private val isSignalQuest get() = TurnSignalQuestController.isSignalQuest(QuestSession.selected().id)

    private var controller: TurnSignalQuestController? = null
    /** 차량 신호 경로에서 화면에 보여줄 현재 단계 안내/사유. */
    private var signalGuide: String? = null
    private var signalError: String? = null

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val q = QuestSession.selected()
        val s = store.state(q)
        val cats = QuestData.cats.getValue(QuestSession.type)

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#CCFFFFFF"), 36f, Color.WHITE, 3f)
            setPadding(dp(36f), dp(32f), dp(36f), dp(32f)); elevation = dp(4f).toFloat()
        }
        panel.addView(tv(if (success) "퀘스트 완료" else cats[q.c], 20f, Color.parseColor("#2E66AA"), true).apply {
            background = rounded(Color.parseColor("#DCEAFA"), 24f); setPadding(dp(18f), dp(8f), dp(18f), dp(8f))
        }, LinearLayout.LayoutParams(-2, -2))
        panel.addView(tv(if (success) "좋아, 잘 해냈어!" else q.name, 44f, ScreenActivity.NAVY, true).apply { setPadding(0, dp(16f), 0, 0) })
        panel.addView(tv(if (success) (q.doneMsg ?: "작은 확인 하나가 익숙한 습관이 되는 거야.") else q.desc, 22f, ScreenActivity.GRAY).apply {
            setPadding(0, dp(12f), 0, dp(8f)); setLineSpacing(0f, 1.3f)
        })
        val steps = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        if (q.tip != null) {
            steps.addView(tv("핵심: " + q.tip, 20f, Color.parseColor("#7A4A00"), true).apply {
                background = rounded(Color.parseColor("#FFF1CC"), 18f); setPadding(dp(20f), dp(16f), dp(20f), dp(16f)); setLineSpacing(0f, 1.2f)
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })
        } else q.steps.forEachIndexed { i, name ->
            val ok = i < s.step
            steps.addView(tv((if (ok) "✓ " else "○ ") + name, 22f, if (ok) Color.parseColor("#2D7653") else ScreenActivity.NAVY).apply {
                background = rounded(Color.parseColor(if (ok) "#D9EEE1" else "#EAF0F8"), 18f); setPadding(dp(20f), dp(16f), dp(20f), dp(16f))
            }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })
        }
        panel.addView(steps, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8f); bottomMargin = dp(18f) })

        val actions = LinearLayout(this).apply { gravity = Gravity.START }
        if (success) {
            actions.addView(button("다른 퀘스트") { goTo(QuestListActivity::class.java) })
            actions.addView(button("기록 보기", primary = false) { goTo(RecordsActivity::class.java) },
                LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
        } else {
            val label = when (s.status) { "doing" -> "계속하기"; "done" -> "다시 해보기"; else -> "퀘스트 시작" }
            actions.addView(button(label) { startQuest(q) })
            actions.addView(button("중단", primary = false, enabled = s.status == "doing") {
                stopQuest(q)
                Toast.makeText(this, "퀘스트를 중단했어요. 완료 기록은 추가하지 않았어요.", Toast.LENGTH_SHORT).show()
            }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
        }
        panel.addView(actions)

        if (!success && s.status == "doing") {
            if (isSignalQuest) {
                // 차량 신호 경로: 현재 단계/사유만 보여주고 수동 완료 버튼은 제공하지 않는다.
                panel.addView(tv("차량 신호로 확인하는 퀘스트예요 · 방향지시등을 직접 조작하세요", 16f, Color.parseColor("#758298"))
                    .apply { setPadding(0, dp(22f), 0, dp(8f)) })
                signalGuide?.let { g ->
                    panel.addView(tv(g, 22f, ScreenActivity.NAVY, true).apply {
                        background = rounded(Color.parseColor("#DFEBFA"), 16f); setPadding(dp(20f), dp(16f), dp(20f), dp(16f)); setLineSpacing(0f, 1.2f)
                    }, LinearLayout.LayoutParams(-1, -2))
                }
                signalError?.let { e ->
                    panel.addView(tv(e, 20f, Color.parseColor("#B23B3B"), true).apply {
                        background = rounded(Color.parseColor("#FBE3E3"), 16f); setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
                    }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10f) })
                }
            } else {
                // 미리보기 경로(기존 기능 유지).
                panel.addView(tv("미리보기 조작 · 실제 차량 조작이 아닙니다", 16f, Color.parseColor("#758298")).apply { setPadding(0, dp(22f), 0, dp(8f)) })
                panel.addView(tv((if (q.tip != null) "동작을 마쳤다고 치기" else q.steps[s.step]) + "　→", 22f, ScreenActivity.NAVY).apply {
                    background = rounded(Color.parseColor("#DFEBFA"), 16f); setPadding(dp(20f), dp(14f), dp(20f), dp(14f))
                    isClickable = true; isFocusable = true
                    setOnClickListener { if (store.advance(q)) success = true; refresh() }
                }, LinearLayout.LayoutParams(-2, -2))
            }
        }

        val scroll = ScrollView(this).apply { addView(panel); isVerticalScrollBarEnabled = false }
        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(36f), 0, dp(36f), dp(24f))
        }
        val imgId = resources.getIdentifier("quest_img_${q.c}_${QuestSession.questIndex}", "drawable", packageName)
        val photo = !success && QuestSession.type == "vehicle" && imgId != 0
        val poseView: android.view.View = if (photo) android.widget.ImageView(this).apply {
            setImageResource(imgId); scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            if (!compact) {
                adjustViewBounds = true; clipToOutline = true
                outlineProvider = object : android.view.ViewOutlineProvider() {
                    override fun getOutline(v: android.view.View, o: android.graphics.Outline) { o.setRoundRect(0, 0, v.width, v.height, dp(30f).toFloat()) }
                }
            }
        } else pose(if (success) 2 else 0)
        if (compact) {
            root.addView(poseView, LinearLayout.LayoutParams(-1, 0, 0.28f))
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 0.72f))
        } else {
            root.addView(poseView, if (photo) LinearLayout.LayoutParams(0, -2, 0.46f) else LinearLayout.LayoutParams(0, -1, 0.40f))
            root.addView(scroll, LinearLayout.LayoutParams(0, -2, 0.60f).apply { leftMargin = dp(24f) })
        }
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    // ───── 시작/중단 분기 ─────
    private fun startQuest(q: Quest) {
        store.start(q)
        if (isSignalQuest) startSignal(q)
        refresh()
    }

    private fun stopQuest(q: Quest) {
        if (isSignalQuest) stopSignal()
        store.stop(q)
        refresh()
    }

    // ───── 차량 신호 경로 ─────
    private fun startSignal(q: Quest) {
        signalError = null
        if (controller != null) return
        val kind = TurnSignalQuestController.questKind(q.id) ?: return
        controller = TurnSignalQuestController(applicationContext, object : TurnSignalQuestController.Callback {
            override fun onStageChanged(guide: String) { runOnUiThread { signalGuide = guide; signalError = null; refresh() } }
            override fun onCompleted() {
                runOnUiThread {
                    // 완료 기록은 1건만. completeVerified 가 중복을 막고, 그 뒤 컨트롤러를 내려 추가 콜백을 차단한다.
                    store.completeVerified(q)
                    stopSignal()
                    success = true
                    refresh()
                }
            }
            override fun onError(reason: String) { runOnUiThread { signalError = reason; refresh() } }
        }, kind).also { it.start() }
    }

    private fun stopSignal() {
        controller?.stop()
        controller = null
    }

    override fun onStop() {
        // 화면을 떠나면 구독 해제. 중단/이탈 후 도착하는 콜백은 컨트롤러가 무시한다.
        if (!success) stopSignal()
        super.onStop()
    }

    override fun onDestroy() {
        stopSignal()
        super.onDestroy()
    }
}
