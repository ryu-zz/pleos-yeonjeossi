package com.example.test

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.constant.area.DoorValue

/**
 * Pleos Vehicle SDK 연결 확인용 첫 화면.
 * 앱을 켜면 운전석 문 위치를 한 번 읽어서 화면과 Logcat("RoadMate")에 보여준다.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var vehicle: Vehicle
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        status = TextView(this).apply {
            textSize = 28f
            gravity = Gravity.CENTER
            text = "Vehicle SDK 연결 중..."
        }
        setContentView(status)

        vehicle = Vehicle(applicationContext)
        vehicle.initialize()

        vehicle.getDoor().getCurrentDoorPosition(
            DoorArea(DoorValue.ROW_1_LEFT),
            { pos ->
                Log.d(TAG, "성공: 운전석 문 위치 = $pos")
                runOnUiThread { status.text = "✅ 연결 성공\n운전석 문 위치: $pos" }
            },
            { e ->
                Log.e(TAG, "실패: ${e.javaClass.simpleName} ${e.message}")
                runOnUiThread { status.text = "❌ 실패\n${e.message}" }
            }
        )
    }

    override fun onDestroy() {
        vehicle.release()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "RoadMate"
    }
}
