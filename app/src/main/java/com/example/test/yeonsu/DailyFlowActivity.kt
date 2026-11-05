package com.example.test.yeonsu

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.constants.NavigationStatus
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener

/**
 * 일상 퀘스트 흐름: 안내할까요? → (내비 앱에서 장소 검색·선택·경로 설정) → 도착 → 시나리오 한 번 더?
 * 장소 검색과 경로 설정은 에뮬레이터의 내비 앱이 직접 한다. 이 화면은 NaviHelper로 경로 시작·도착 신호를 받는다.
 * 도착 신호가 오지 않아도 진행할 수 있게 "도착했다고 치기" 버튼을 둔다(시연용).
 */
class DailyFlowActivity : ScreenActivity() {
    override val parentScreen: Class<out Activity> = QuestListActivity::class.java
    private val store by lazy { QuestStore(this) }
    private var navi: NaviHelper? = null
    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(info: RouteStartInfo) {
            android.util.Log.d("YeonsuNavi", "onRouteStarted name=${info.name}"); runOnUiThread { QuestFlow.destName = info.name; if (QuestFlow.stage == QuestFlow.WAIT) refresh() }
        }
        override fun onNavigationStatus(status: NavigationStatus) {
            android.util.Log.d("YeonsuNavi", "onNavigationStatus $status")
            if (status == NavigationStatus.ROUTE_DRIVE) askDestination()
        }
        override fun onDestinationChanged() { android.util.Log.d("YeonsuNavi", "onDestinationChanged"); askDestination() }
        override fun onDestinationInfo(info: DestinationInfo) {
            android.util.Log.d("YeonsuNavi", "onDestinationInfo name=${info.name}")
            runOnUiThread { if (info.name.isNotBlank()) { QuestFlow.destName = info.name; if (QuestFlow.stage == QuestFlow.WAIT) refresh() } }
        }
        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            android.util.Log.d("YeonsuNavi", "onDestinationArrived ${info.name}")
            runOnUiThread { if (QuestFlow.stage == QuestFlow.WAIT) { QuestFlow.stage = QuestFlow.ARRIVED; refresh() } }
        }
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var guideText: android.widget.TextView? = null
    private var guideIdx = 0
    private val guideHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val guideMsgs: List<String> by lazy {
        DriveCoach.all.filter { (it.category == "방향지시등" || it.category == "감속 준비") && it.trigger != "운전 종료 후" }.map { it.text }
    }
    private val guideTick = object : Runnable {
        override fun run() {
            if (QuestFlow.stage != QuestFlow.WAIT || QuestFlow.destName == null) return
            val m = guideMsgs[guideIdx++ % guideMsgs.size]
            guideText?.text = m; speak(m)
            guideHandler.postDelayed(this, 9000)
        }
    }
    private fun speak(t: String) { if (ttsReady) try { tts?.speak(t, true) } catch (_: Throwable) {} }
    private fun startGuide() { guideHandler.removeCallbacks(guideTick); guideHandler.postDelayed(guideTick, 1500) }
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            tts = TextToSpeech(applicationContext, TtsMode.ON_DEVICE).also { t ->
                t.initialize(object : TtsInit {
                    override fun onSuccess() { try { t.setLocale(java.util.Locale.KOREA); t.setSpeakerType("EUNWOO") } catch (_: Throwable) {}; ttsReady = true }
                    override fun onFailure(errorType: TtsError) {}
                })
            }
        } catch (_: Throwable) {}
        try {
            navi = NaviHelper(applicationContext).also {
                it.initialize({ android.util.Log.d("YeonsuNavi", "init ok"); it.addListener(listener) }, { e -> android.util.Log.d("YeonsuNavi", "init fail $e") })
            }
        } catch (_: Throwable) { navi = null }
    }

    /** 내비에 지금 설정된 목적지를 물어본다(응답은 onDestinationInfo). */
    private fun askDestination() { try { navi?.getDestinationInfo() } catch (_: Throwable) {} }

    override fun onResume() {
        super.onResume()
        if (QuestFlow.stage == QuestFlow.WAIT) askDestination()
    }

    override fun onDestroy() {
        guideHandler.removeCallbacks(guideTick)
        try { tts?.release() } catch (_: Throwable) {}
        try { navi?.removeListener(listener); navi?.release() } catch (_: Throwable) {}
        super.onDestroy()
    }

    override fun build(content: FrameLayout, w: Int, h: Int) {
        val q = QuestSession.selected()
        val flow = QuestData.flows.getValue(QuestSession.category)
        val cats = QuestData.cats.getValue(QuestSession.type)

        if (QuestFlow.stage == QuestFlow.WAIT && QuestFlow.destName != null) { buildGuide(content, q.name, QuestFlow.destName!!, flow); return }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#CCFFFFFF"), 36f, Color.WHITE, 3f)
            setPadding(dp(36f), dp(32f), dp(36f), dp(32f)); elevation = dp(4f).toFloat()
        }
        panel.addView(tv(cats[q.c], 20f, Color.parseColor("#2E66AA"), true).apply {
            background = rounded(Color.parseColor("#DCEAFA"), 24f); setPadding(dp(18f), dp(8f), dp(18f), dp(8f))
        }, LinearLayout.LayoutParams(-2, -2))
        panel.addView(tv(q.name, 36f, ScreenActivity.NAVY, true).apply { setPadding(0, dp(14f), 0, dp(10f)) })

        val actions = LinearLayout(this).apply { gravity = Gravity.START }
        when (QuestFlow.stage) {
            QuestFlow.WAIT -> {
                val dest = QuestFlow.destName
                if (dest == null) {
                    guideText = null
                    panel.addView(tv("내비에서 '${flow.find}' 중 한 곳을 골라 경로를 설정해 주세요.", 24f, ScreenActivity.NAVY, true).apply { setLineSpacing(0f, 1.25f) })
                    panel.addView(tv("아직 경로가 설정되지 않았어요.", 20f, ScreenActivity.GRAY).apply { setPadding(0, dp(12f), 0, dp(6f)) })
                } else {
                    panel.addView(tv("길 안내 중 · $dest", 22f, Color.parseColor("#2D7653"), true).apply { setPadding(0, 0, 0, dp(10f)) })
                    val g = tv(guideMsgs[guideIdx % guideMsgs.size], 30f, ScreenActivity.NAVY, true).apply { setLineSpacing(0f, 1.25f) }
                    guideText = g; panel.addView(g)
                    startGuide()
                }
                actions.addView(button("내비 열기", primary = false) { openNavi(flow) })
                actions.addView(button("도착했다고 치기") { QuestFlow.stage = QuestFlow.ARRIVED; refresh() },
                    LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
                panel.addView(actions, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(18f) })
                panel.addView(tv("도착하면 자동으로 다음으로 넘어가요. 시연할 땐 위 버튼으로 넘길 수 있어요.", 16f, Color.parseColor("#758298")).apply { setPadding(0, dp(16f), 0, 0) })
            }
            QuestFlow.ARRIVED -> {
                guideHandler.removeCallbacks(guideTick)
                panel.addView(tv("도착했어요! 시나리오를 한 번 더 볼까요?", 26f, ScreenActivity.NAVY, true).apply { setLineSpacing(0f, 1.25f) })
                speak("도착했다! 시나리오 한 번 더 볼래?")
                actions.addView(button("예") { QuestFlow.stage = QuestFlow.REPLAY; goTo(ScenarioActivity::class.java) })
                actions.addView(button("아니오", primary = false) {
                    store.complete(q)
                    Toast.makeText(this, "퀘스트를 완료했어요. 마이페이지에 기록됐어요.", Toast.LENGTH_SHORT).show()
                    goTo(QuestListActivity::class.java)
                }, LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
                panel.addView(actions, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(22f) })
            }
            else -> { // ASK
                panel.addView(tv(flow.ask, 26f, ScreenActivity.NAVY, true).apply { setLineSpacing(0f, 1.25f) })
                actions.addView(button("예") { QuestFlow.stage = QuestFlow.WAIT; QuestFlow.destName = null; openNavi(flow); refresh() })
                actions.addView(button("아니오", primary = false) { goTo(QuestListActivity::class.java) },
                    LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
                panel.addView(actions, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(22f) })
            }
        }

        val scroll = ScrollView(this).apply { addView(panel); isVerticalScrollBarEnabled = false }
        val root = LinearLayout(this).apply {
            orientation = if (compact) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(36f), 0, dp(36f), dp(24f))
        }
        val pose = pose(0)
        if (compact) {
            root.addView(pose, LinearLayout.LayoutParams(-1, 0, 0.28f))
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 0.72f))
        } else {
            root.addView(pose, LinearLayout.LayoutParams(0, -1, 0.40f))
            root.addView(scroll, LinearLayout.LayoutParams(0, -2, 0.60f).apply { leftMargin = dp(24f) })
        }
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
    }

    /** 경로 설정 후 화면: 길 안내가 주인공이고, 퀘스트 진행 중이라는 표시는 위쪽 작은 띠로만 둔다. */
    private fun buildGuide(content: FrameLayout, questName: String, dest: String, flow: QuestData.Flow) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(36f), 0, dp(36f), dp(28f))
        }
        root.addView(tv("퀘스트 중 · $questName", 17f, Color.parseColor("#2E66AA"), true).apply {
            maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
            background = rounded(Color.parseColor("#DCEAFA"), 24f); setPadding(dp(16f), dp(7f), dp(16f), dp(7f))
        }, LinearLayout.LayoutParams(-2, -2))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            background = rounded(Color.parseColor("#F2FFFFFF"), 36f, Color.WHITE, 3f)
            setPadding(dp(32f), dp(28f), dp(32f), dp(28f)); elevation = dp(4f).toFloat()
        }
        card.addView(tv("길 안내 중 · $dest", 20f, Color.parseColor("#2D7653"), true).apply { gravity = Gravity.CENTER })
        val g = tv(guideMsgs[guideIdx % guideMsgs.size], 46f, ScreenActivity.NAVY, true).apply {
            gravity = Gravity.CENTER; setLineSpacing(0f, 1.25f)
        }
        guideText = g
        card.addView(g, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(22f) })
        root.addView(card, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(16f) })
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER }
        bar.addView(button("내비 열기", primary = false) { openNavi(flow) })
        bar.addView(button("도착했다고 치기") { QuestFlow.stage = QuestFlow.ARRIVED; refresh() },
            LinearLayout.LayoutParams(-2, -2).apply { leftMargin = dp(14f) })
        root.addView(bar, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16f) })
        content.addView(root, FrameLayout.LayoutParams(-1, -1))
        startGuide()
    }
    /**
     * 내비 앱을 앞으로 가져온 뒤, 내비 앱의 SEARCH_PLACE 신호로 장소를 검색한다.
     * 내비 앱이 지도에 번호 핀과 가까운 곳 목록을 보여주고, 목록의 화살표를 누르면 그 장소로 경로가 설정된다.
     * (SEARCH_PLACE 신호의 형식은 내비 앱 로그로 확인했다: ACTION_PARAM에 JSON {"query": ...})
     */
    private fun openNavi(flow: QuestData.Flow) {
        try {
            startActivity(Intent().setComponent(ComponentName(NAVI_PKG, "$NAVI_PKG.MainActivity"))
                .setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val json = org.json.JSONObject().put("query", flow.query).put("utterance", flow.query).toString()
            android.os.Handler(mainLooper).postDelayed({
                sendBroadcast(Intent("$NAVI_PKG.SEARCH_PLACE").setPackage(NAVI_PKG).putExtra("ACTION_PARAM", json))
            }, 700)
        } catch (e: Exception) {
            Toast.makeText(this, "내비 앱을 열 수 없어요", Toast.LENGTH_SHORT).show()
        }
    }

    companion object { private const val NAVI_PKG = "ai.umos.maps.android.navigation.app" }
}
