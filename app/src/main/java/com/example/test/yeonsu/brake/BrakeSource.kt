package com.example.test.yeonsu.brake

/**
 * 브레이크 페달 눌림 상태 수신원 추상화.
 *
 * 실제 구현(VehicleBrakeSource)은 Pleos Vehicle SDK 의 Brake API
 * (getCurrentBrakePedalStatus / registerBrakePedalStatus, BrakePedalStatusListener)를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러/판정 로직은 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 다른 수신원(문/창문/충전구)과 같은 구조를 따른다.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */
interface BrakeSource {
    interface Observer {
        /** 현재 브레이크 눌림 여부(초기 1회). 못 읽으면 null. */
        fun onInitial(pressed: Boolean?)
        /** 이후 변경 수신. */
        fun onUpdate(pressed: Boolean?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
