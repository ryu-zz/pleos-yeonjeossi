package com.example.test.yeonsu.signal

/**
 * 방향지시등 신호 수신원 추상화.
 *
 * 실제 구현(VehicleTurnSignalSource)은 Pleos Vehicle SDK 를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러는 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */
interface TurnSignalSource {
    interface Observer {
        /** 현재 값(초기 1회). 못 읽으면 null 을 넘긴다. */
        fun onInitial(signal: Signal?)
        /** 이후 변경 수신. */
        fun onUpdate(signal: Signal?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
