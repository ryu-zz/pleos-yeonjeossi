package com.example.test.yeonsu.trunk

/**
 * 트렁크(후방 테일게이트) 위치 신호 수신원 추상화.
 *
 * 실제 구현(VehicleTrunkSource)은 Pleos Vehicle SDK 의 Door API 를
 * DoorArea(DoorValue.REAR) 로 쓰지만(트렁크=후방 도어),
 * 이 인터페이스 덕분에 컨트롤러는 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 운전석 문 수신원(DoorSource)과 같은 구조를 따른다.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */

/** 트렁크 위치 값. SDK position(0=닫힘, >0=열림)과 매핑되지만 테스트를 위해 독립적으로 둔다. */
enum class TrunkState { CLOSED, OPEN }

interface TrunkSource {
    interface Observer {
        /** 현재 값(초기 1회). 못 읽으면 null 을 넘긴다. */
        fun onInitial(state: TrunkState?)
        /** 이후 변경 수신. */
        fun onUpdate(state: TrunkState?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
