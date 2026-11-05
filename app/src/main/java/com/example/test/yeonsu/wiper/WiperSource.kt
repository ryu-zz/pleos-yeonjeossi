package com.example.test.yeonsu.wiper

/**
 * 와이퍼 작동 상태 수신원 추상화.
 *
 * 실제 구현(VehicleWiperSource)은 Pleos Vehicle SDK 의 Wiper API
 * (getCurrentWiperState / registerWiperState, WiperStateListener)를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러/판정 로직은 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * WiperState(OFF/ON/OTHER/SERVICE)를 "작동 중 여부(on: Boolean)"로 바꿔 올린다.
 * - ON → on=true, OFF → on=false, 그 외/불명 → null.
 *
 * 주의: 이 신호는 ON/OFF 만 구분한다. 와이퍼 '단계'(저속/고속/자동/간헐)는
 *       WiperSwitch 로만 알 수 있는데 SDK 에 상태 변경 리스너가 없어(getter 만) 자동 판정 대상이 아니다.
 *
 * 다른 수신원(문/창문/전조등)과 같은 구조를 따른다.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */
interface WiperSource {
    interface Observer {
        /** 현재 작동 여부(초기 1회). 못 읽거나 불명이면 null. */
        fun onInitial(on: Boolean?)
        /** 이후 변경 수신. */
        fun onUpdate(on: Boolean?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
