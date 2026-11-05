package com.example.test.yeonsu.door

/**
 * 문 위치 신호 수신원 추상화.
 *
 * 실제 구현(VehicleDoorSource)은 Pleos Vehicle SDK 의 Door API 를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러는 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 방향지시등(TurnSignalSource)과 같은 구조를 따른다.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */

/** 문 위치 값. SDK 의 position(0=닫힘, 1=열림)과 매핑되지만 테스트를 위해 독립적으로 둔다. */
enum class DoorState { CLOSED, OPEN }

interface DoorSource {
    interface Observer {
        /** 현재 값(초기 1회). 못 읽으면 null 을 넘긴다. */
        fun onInitial(state: DoorState?)
        /** 이후 변경 수신. */
        fun onUpdate(state: DoorState?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
