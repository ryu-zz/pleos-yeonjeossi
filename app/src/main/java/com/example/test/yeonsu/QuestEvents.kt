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
 * - 목적지 음성 검색: 인식한 문장이 목적지처럼 들리면(장소 끝말 또는 길 안내 말투) 완료. 내비 검색은 실행하지 않는다
 * - 주유소·충전소 검색: 인식한 문장에 "충전소"나 "주유소"가 들어 있으면 완료. 내비 검색은 실행하지 않는다
 * - 경유지 추가: 내비에서 경유지가 추가되면 완료
 */
class QuestEvents private constructor(
    val hint: String,
    private val name: String,
    private val start: (QuestEvents, Context, Int, Callbacks) -> Unit,
) {
    /** 화면이 받는 알림. 모두 메인 스레드가 아닐 수 있으니 화면에서 runOnUiThread 로 받는다. */
    interface Callbacks {
        fun onReady()
        fun onDone(why: String)
        /** 사용자에게 보여줄 오류 안내. 이 뒤에는 새로 open 해야 다시 시작한다. */
        fun onError(message: String)
    }
    private val gate = EventGate()
    internal var stt: SpeechToText? = null
    internal var navi: NaviHelper? = null
    internal var naviListener: NaviHelperEventListener? = null
    val isOpen: Boolean get() = gate.active
    /** 듣기·수신 시작. 이미 열려 있으면 먼저 닫고 새로 연다(이전 구간의 늦은 결과는 버린다). */
    fun open(ctx: Context, cb: Callbacks) {
        close("다시 열기")
        val t = gate.open()
        Log.i(TAG, "[$name] 수신 시작 token=$t")
        try { start(this, ctx, t, cb) } catch (e: Throwable) {
            Log.e(TAG, "[$name] 시작 실패 $e")
            cb.onError("음성·내비 연결을 시작하지 못했어요. '다시 시도'를 눌러 주세요.")
        }
    }
    /** 중단·완료·화면 이탈·종료 때 호출. SDK 를 해제하고 이후 도착하는 결과는 무시한다. */
    fun close(reason: String) {
        val wasOpen = gate.active
        gate.close()
        val hadStt = stt != null; val hadNavi = navi != null
        try { stt?.stop() } catch (_: Throwable) {}
        try { stt?.release() } catch (_: Throwable) {}
        try { naviListener?.let { navi?.removeListener(it) } } catch (_: Throwable) {}
        try { navi?.release() } catch (_: Throwable) {}
        stt = null; navi = null; naviListener = null
        if (wasOpen || hadStt || hadNavi) Log.i(TAG, "[$name] 수신 종료 reason=$reason stt해제=$hadStt navi해제=$hadNavi")
    }
    internal fun accept(t: Int) = gate.accept(t)
    /** 음성을 듣다가 ok(문장)가 참이면 완료. 한 번 듣기가 끝나면 다시 듣는다. */
    internal fun listen(ctx: Context, t: Int, cb: Callbacks, ok: (String) -> Boolean) {
        val s = SpeechToText(ctx.applicationContext, Mode.ON_DEVICE)
        stt = s
        var silentRetries = 0
        s.initialize(object : OnInitializeListener {
            override fun onSuccess() {
                if (!accept(t)) { Log.i(TAG, "[$name] 연결 완료가 늦게 도착해 무시(token=$t)"); try { s.release() } catch (_: Throwable) {}; return }
                s.setLocale(java.util.Locale.KOREA)
                s.addListener(object : ResultListener {
                    override fun onUpdated(text: String, isFinal: Boolean) {
                        if (!accept(t)) return
                        if (!isFinal || text.isBlank()) return
                        silentRetries = 0
                        Log.d(TAG, "[$name] 음성 인식: $text")
                        if (ok(text)) cb.onDone("\"$text\"")
                    }
                    override fun onEndedRecognition() { if (accept(t)) try { s.request() } catch (_: Throwable) {} }
                    override fun onError(errorType: ErrorType) {
                        if (!accept(t)) return
                        Log.w(TAG, "[$name] 음성 인식 오류 $errorType")
                        val d = EventErrors.describe(errorType.name)
                        if (d.silentRetry && silentRetries < EventErrors.MAX_SILENT_RETRY) {
                            silentRetries++
                            try { s.request() } catch (_: Throwable) {}
                        } else {
                            val msg = if (d.silentRetry) "말씀이 들리지 않아요. '다시 시도'를 눌러 주세요." else d.message
                            cb.onError(msg)
                            close("오류 $errorType")
                        }
                    }
                    override fun onUpdatedEpdData(a: Long, b: Long) {}
                    override fun onStartedRecognition() { silentRetries = 0 }
                    override fun onReady() {}
                    override fun onUpdatedRms(r: Double) {}
                })
                Log.i(TAG, "[$name] 녹음 시작 요청 token=$t")
                cb.onReady()
                s.request()
            }
            override fun onFailure(errorType: ErrorType) {
                if (!accept(t)) return
                Log.e(TAG, "[$name] 음성 연결 실패 $errorType")
                cb.onError(EventErrors.describe(errorType.name).message.ifBlank { "음성 인식에 연결하지 못했어요. '다시 시도'를 눌러 주세요." })
                close("연결 실패 $errorType")
            }
        })
    }
    companion object {
        private const val TAG = "YeonsuSignal"
        fun find(name: String): QuestEvents? = when (name) {
            "목적지 음성 검색" -> voice(name, "\"서울역으로 가자\"처럼 목적지를 말하면 완료돼요 (말한 문장만 확인하고, 내비 검색은 실행하지 않아요)") { isDestination(it) }
            "주유소·충전소 검색" -> voice(name, "\"가까운 충전소 찾아줘\"라고 말하면 완료돼요 (말한 문장만 확인하고, 내비 검색은 실행하지 않아요)") { isStationQuery(it) }
            "경유지 추가" -> waypoint(name)
            else -> null
        }

        /** 장소를 뜻하는 끝말 (단어 끝에 붙어 있을 때만 장소로 본다. "역사"처럼 중간에 들어간 글자는 아니다) */
        private val PLACE = listOf("역", "시청", "공원", "병원", "마트", "터미널", "공항", "학교", "타워", "백화점", "주차장")
        /** 길 안내를 부탁하는 말투 (띄어쓰기 없이 비교) */
        private val ASK = listOf("가자", "가줘", "안내해", "데려다", "길찾", "가고싶", "가려고")
        /** 장소 단어 뒤에 붙는 조사 */
        private val PARTICLES = listOf("으로", "에서", "까지", "로", "에", "을", "를", "은", "는", "이", "가")
        /** 목적지를 말한 것으로 보는 규칙 (내비 검색을 실행하지는 않는다): 단어가 장소 끝말로 끝나거나, 길 안내를 부탁하는 말투가 있으면 통과. */
        internal fun isDestination(text: String): Boolean {
            val compact = text.replace(" ", "")
            if (ASK.any { compact.contains(it) }) return true
            return text.trim().split(Regex("\\s+")).any { w ->
                val bare = PARTICLES.firstOrNull { w.length > it.length + 1 && w.endsWith(it) }?.let { w.removeSuffix(it) } ?: w
                PLACE.any { p -> bare.endsWith(p) && bare.length >= maxOf(p.length, 2) }
            }
        }
        /** 주유소·충전소를 찾아 달라는 말인지. */
        internal fun isStationQuery(text: String): Boolean {
            val t = text.replace(" ", "")
            return t.contains("충전소") || t.contains("주유소")
        }
        private fun voice(name: String, hint: String, ok: (String) -> Boolean) =
            QuestEvents(hint, name) { q, ctx, t, cb -> q.listen(ctx, t, cb, ok) }
        private fun waypoint(name: String) = QuestEvents("내비에서 경유지를 추가하면 완료돼요", name) { q, ctx, t, cb ->
            val n = NaviHelper(ctx.applicationContext)
            q.navi = n
            n.initialize({
                if (!q.accept(t)) return@initialize
                val l = object : NaviHelperEventListener {
                    override fun onWaypointChanged(waypointChangedInfo: WaypointChangedInfo) {
                        if (!q.accept(t)) return
                        Log.d(TAG, "[$name] 경유지 변경: ${waypointChangedInfo.index} ${waypointChangedInfo.cmdType}")
                        if (waypointChangedInfo.cmdType == WaypointCmdType.ADD) cb.onDone("경유지 추가됨")
                    }
                }
                q.naviListener = l
                n.addListener(l)
                Log.i(TAG, "[$name] 내비 수신 시작 token=$t")
                cb.onReady()
            }, { e ->
                if (!q.accept(t)) return@initialize
                Log.e(TAG, "[$name] 내비 연결 실패 $e")
                cb.onError("내비에 연결하지 못했어요. 내비 앱이 켜져 있는지 확인하고 '다시 시도'를 눌러 주세요.")
                q.close("내비 연결 실패")
            })
        }
    }
}
