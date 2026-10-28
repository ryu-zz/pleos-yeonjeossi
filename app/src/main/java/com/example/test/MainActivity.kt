package com.example.test

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.stt.constant.ErrorType as SttError
import ai.pleos.playground.stt.constant.Mode as SttMode
import ai.pleos.playground.stt.listener.OnInitializeListener as SttInit
import ai.pleos.playground.tts.constant.ErrorType as TtsError
import ai.pleos.playground.tts.constant.Mode as TtsMode
import ai.pleos.playground.tts.listener.OnInitializeListener as TtsInit

/**
 * Pleos SDK 연결 확인 화면.
 * Vehicle(사용 가능) + FusedLocation/STT/TTS(Playground "준비 중")가
 * 에뮬레이터에서 실제로 동작하는지 한 화면에 표시한다. Logcat 태그: RoadMate
 */
class MainActivity : AppCompatActivity() {

    private lateinit var vehicle: Vehicle
    private lateinit var fused: FusedLocation
    private lateinit var stt: SpeechToText
    private lateinit var tts: TextToSpeech
    private lateinit var out: TextView
    private val lines = linkedMapOf(
        "Vehicle" to "확인 중...",
        "FusedLocation" to "확인 중...",
        "SpeechToText" to "확인 중...",
        "TextToSpeech" to "확인 중..."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        out = TextView(this).apply { textSize = 26f; setPadding(60, 60, 60, 60) }
        setContentView(ScrollView(this).apply { addView(out) })
        render()

        checkVehicle()
        checkStt()
        checkTts()

        // FUSED_LOCATION 은 dangerous 권한이라 실행 중에 허용을 받아야 한다
        if (checkSelfPermission(FUSED_PERM) == PackageManager.PERMISSION_GRANTED) checkFused()
        else requestPermissions(arrayOf(FUSED_PERM), REQ_FUSED)
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        if (code != REQ_FUSED) return
        if (results.firstOrNull() == PackageManager.PERMISSION_GRANTED) checkFused()
        else set("FusedLocation", "❌ 위치 권한 거부됨")
    }

    private fun checkVehicle() {
        vehicle = Vehicle(applicationContext)
        vehicle.initialize()
        vehicle.getDoor().getCurrentDoorPosition(
            DoorArea(DoorValue.ROW_1_LEFT),
            { pos -> set("Vehicle", "✅ 성공 (운전석 문 위치: $pos)") },
            { e -> set("Vehicle", "❌ ${e.javaClass.simpleName}: ${e.message}") }
        )
    }

    private fun checkFused() {
        try {
            fused = FusedLocation(applicationContext)
            fused.initialize()
            if (!fused.isSupported()) { set("FusedLocation", "❌ isSupported() = false"); return }
            fused.getLastKnownLocation(object : FusedLocationListener {
                override fun onChanged(location: LocationData?) {
                    set("FusedLocation",
                        if (location == null) "⚠️ 지원됨, 위치 값 없음(null)"
                        else "✅ 성공 (${location.latitude}, ${location.longitude})")
                }
                override fun onError(exception: Exception) {
                    set("FusedLocation", "❌ ${exception.javaClass.simpleName}: ${exception.message}")
                }
            })
        } catch (e: Exception) {
            set("FusedLocation", "❌ 예외 ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun checkStt() {
        try {
            stt = SpeechToText(applicationContext, SttMode.ON_DEVICE)
            stt.initialize(object : SttInit {
                override fun onSuccess() = set("SpeechToText", "✅ 초기화 성공 (ON_DEVICE)")
                override fun onFailure(errorType: SttError) = set("SpeechToText", "❌ 초기화 실패: $errorType")
            })
        } catch (e: Exception) {
            set("SpeechToText", "❌ 예외 ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun checkTts() {
        try {
            tts = TextToSpeech(applicationContext, TtsMode.ON_DEVICE)
            tts.initialize(object : TtsInit {
                override fun onSuccess() {
                    set("TextToSpeech", "✅ 초기화 성공 (ON_DEVICE)")
                    tts.speak("로드메이트 연결 테스트입니다", false)
                }
                override fun onFailure(errorType: TtsError) = set("TextToSpeech", "❌ 초기화 실패: $errorType")
            })
        } catch (e: Exception) {
            set("TextToSpeech", "❌ 예외 ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    private fun set(key: String, value: String) {
        Log.d(TAG, "$key -> $value")
        lines[key] = value
        runOnUiThread { render() }
    }

    private fun render() {
        out.text = "Pleos SDK 연결 확인\n\n" + lines.entries.joinToString("\n\n") { "${it.key}\n  ${it.value}" }
    }

    override fun onDestroy() {
        if (::vehicle.isInitialized) vehicle.release()
        if (::fused.isInitialized) fused.release()
        if (::stt.isInitialized) stt.release()
        if (::tts.isInitialized) tts.release()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RoadMate"
        private const val FUSED_PERM = "pleos.car.permission.FUSED_LOCATION"
        private const val REQ_FUSED = 1
    }
}
