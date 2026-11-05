package com.example.test.yeonsu.highbeam

/**
 * 상향등(High beam) 상태 수신원 추상화.
 *
 * 실제 구현(VehicleHighBeamSource)은 Pleos Vehicle SDK 의 Light API(HighBeamLightState)를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러는 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 하향등/방향지시등/문 수신원과 같은 구조를 따른다.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */

/**
 * 상향등 상태 값. SDK 의 LightState(OFF/ON/DAYTIME_RUNNING)와 매핑되지만 테스트를 위해 독립적으로 둔다.
 * 판정에는 ON/OFF 만 쓰고, DAYTIME_RUNNING 은 상향등 켜짐으로 인정하지 않는다.
 */
enum class HighBeamState { OFF, ON, DAYTIME_RUNNING }

interface HighBeamSource {
    interface Observer {
        /** 현재 값(초기 1회). 못 읽으면 null 을 넘긴다. */
        fun onInitial(state: HighBeamState?)
        /** 이후 변경 수신. */
        fun onUpdate(state: HighBeamState?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
