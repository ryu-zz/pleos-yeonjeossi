package com.example.test.yeonsu.lock

/**
 * 전체 문 잠금 상태 수신원 추상화.
 *
 * 실제 구현(VehicleDoorLockSource)은 Pleos Vehicle SDK 의 Door Lock API
 * (getDoorLockState / registerDoorLockState, DoorLockStateListener)를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러는 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 문 네 짝(운전석/조수석/뒷좌석 좌·우)을 각각 구독하고,
 * "네 짝 모두 잠김"인지 여부(allLocked)만 상위로 올린다.
 * 한 짝이라도 상태를 모르면(null) allLocked 는 알 수 없음(null)으로 둔다.
 *
 * 다른 수신원(문/창문/전조등)과 같은 구조를 따른다.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */
interface DoorLockSource {
    interface Observer {
        /** 현재 '전체 잠김' 여부(초기 1회). 한 짝이라도 불명이면 null. */
        fun onInitial(allLocked: Boolean?)
        /** 이후 변경 수신. 한 짝이라도 바뀌면 재계산한 전체 잠김 여부. */
        fun onUpdate(allLocked: Boolean?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
