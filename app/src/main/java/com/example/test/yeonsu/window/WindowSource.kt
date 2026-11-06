package com.example.test.yeonsu.window

/**
 * 운전석 창문 위치 수신원 추상화.
 *
 * 실제 구현(VehicleWindowSource)은 Pleos Vehicle SDK 의 Window API(WindowPosition, ROW_1_LEFT)를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러는 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 다른 수신원(방향지시등/문/전조등)과 같은 구조를 따른다.
 * 창문 위치는 0(완전히 닫힘)~100(완전히 열림) 정수이며, 운전석(ROW_1_LEFT) 영역만 구독한다.
 * (영역별 구독이므로 다른 창문 값은 이 수신원으로 들어오지 않는다.)
 *
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */
interface WindowSource {
    interface Observer {
        /** 현재 위치(초기 1회). 못 읽으면 null. 0=닫힘, >0=열림(부분 포함). */
        fun onInitial(position: Int?)
        /** 이후 변경 수신. */
        fun onUpdate(position: Int?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
