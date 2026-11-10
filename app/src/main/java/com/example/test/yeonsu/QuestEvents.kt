package com.example.test.yeonsu

import ai.pleos.playground.navi.constants.WaypointCmdType
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.content.Context
import android.util.Log

/**
 * 내비·음성 퀘스트 자동 완료 (팀장 브랜치에는 없음, 내 브랜치에서 추가).
 * QuestSignals는 차량 값을 주기적으로 읽지만, 여기는 내비/음성 SDK가 알려주는 사건으로 완료를 판정한다.
 * - 목적지 음성 검색: 음성 인식 결과(문장)가 나오면 완료
 * - 주유소·충전소 검색: 인식한 문장에 "충전소"나 "주유소"가 들어 있으면 완료
 * - 경유지 추가: 내비에서 경유지가 추가되면 완료
 */
class QuestEvents private constructor(
    val hint: String,
    private val start: (Context, (String) -> Unit, (String) -> Unit) -> Unit,
) {
    private var stt: SpeechToText? = null
    private var navi: NaviHelper? = null
    private var stopped = false

    /** onReady: 연결되면 한 번, onDone(설명): 조건을 만족하면 한 번 */
    fun begin(ctx: Context, onReady: (String) -> Unit, onDone: (String) -> Unit) = start(ctx, onReady, onDone)

    fun release() {
        stopped = true
        try { stt?.stop(); stt?.release() } catch (_: Throwable) {}
        try { navi?.release() } catch (_: Throwable) {}
    }

    /** 음성을 계속 듣다가 ok(문장)가 참이면 완료. 한 번 듣기가 끝나면 다시 듣는다. */
    private fun listen(ctx: Context, onReady: (String) -> Unit, onDone: (String) -> Unit, ok: (String) -> Boolean) {
        val s = SpeechToText(ctx.applicationContext, Mode.ON_DEVICE).also { stt = it }
        s.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                s.setLocale(java.util.Locale.KOREA)
                s.addListener(object : ResultListener {
                    override fun onUpdated(text: String, isFinal: Boolean) {
                        if (!isFinal || text.isBlank()) return
                        Log.d("YeonsuSignal", "음성 인식: $text")
                        if (ok(text)) onDone("\"$text\"")
                    }
                    override fun onEndedRecognition() { if (!stopped) try { s.request() } catch (_: Throwable) {} }
                    override fun onError(errorType: ErrorType) {
                        Log.d("YeonsuSignal", "음성 인식 오류 $errorType")
                        if (!stopped) try { s.request() } catch (_: Throwable) {}
                    }
                    override fun onUpdatedEpdData(a: Long, b: Long) {}
                    override fun onStartedRecognition() {}
                    override fun onReady() {}
                    override fun onUpdatedRms(r: Double) {}
                })
                onReady("음성 듣는 중")
                s.request()
            }
            override fun onFailure(errorType: ErrorType) { Log.e("YeonsuSignal", "음성 연결 실패 $errorType") }
        })
    }

    companion object {
        fun find(name: String): QuestEvents? = when (name) {
            "목적지 음성 검색" -> voice("\"서울역으로 가자\"처럼 목적지를 말하면 자동으로 완료돼요") { isDestination(it) }
            "주유소·충전소 검색" -> voice("\"가까운 충전소 찾아줘\"라고 말하면 자동으로 완료돼요") { it.contains("충전소") || it.contains("주유소") }
            "경유지 추가" -> waypoint()
            else -> null
        }

        /** 장소를 뜻하는 끝말 */
        private val PLACE = listOf("역", "시청", "공원", "병원", "마트", "터미널", "공항", "학교", "타워", "백화점", "주차장")
        /** 길 안내를 부탁하는 말투 (띄어쓰기 없이 비교) */
        private val ASK = listOf("가자", "가줘", "가 줘", "안내해", "데려다", "길찾", "가고싶", "가려고", "까지")

        /** 장소 끝말이 있거나, 길 안내 말투가 있으면 목적지를 말한 것으로 본다. 식당 이름처럼 끝말이 없어도 "~로 가자"면 통과. */
        private fun isDestination(text: String): Boolean {
            val t = text.replace(" ", "")
            return PLACE.any { t.contains(it) } || ASK.any { t.contains(it.replace(" ", "")) }
        }

        private fun voice(hint: String, ok: (String) -> Boolean): QuestEvents {
            lateinit var q: QuestEvents
            q = QuestEvents(hint) { ctx, ready, done -> q.listen(ctx, ready, done, ok) }
            return q
        }

        private fun waypoint(): QuestEvents {
            lateinit var q: QuestEvents
            q = QuestEvents("내비에서 경유지를 추가하면 자동으로 완료돼요") { ctx, ready, done ->
                val n = NaviHelper(ctx.applicationContext).also { q.navi = it }
                n.initialize({
                    n.addListener(object : NaviHelperEventListener {
                        override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) {
                            Log.d("YeonsuSignal", "경유지 변경: ${waypointChangedInfo.index} ${waypointChangedInfo.cmdType}")
                            if (waypointChangedInfo.cmdType == WaypointCmdType.ADD) done("경유지 추가됨")
                        }
                    })
                    ready("내비 연결됨")
                }, { e -> Log.e("YeonsuSignal", "내비 연결 실패 $e") })
            }
            return q
        }
    }
}
