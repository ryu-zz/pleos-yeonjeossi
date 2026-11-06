package com.example.test.yeonsu.polling

/**
 * 폴링 퀘스트 수신원 추상화 (SDK 비의존).
 *
 * 실제 구현(VehiclePolledSource)은 Pleos Vehicle SDK getter 를 주기 조회하지만,
 * 이 인터페이스 덕분에 컨트롤러/판정 로직은 SDK 없이(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 조회값을 "목표 조건 충족 여부(isTarget)"로 바꿔 올린다. 못 읽거나 불명이면 null.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */
interface PollSource {
    interface Observer {
        /** 첫 조회 결과(1회). 못 읽으면 null. */
        fun onInitial(isTarget: Boolean?)
        /** 이후 조회 결과. */
        fun onUpdate(isTarget: Boolean?)
        /** 조회 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 주차 확인 + 주기 조회 시작. */
    fun start(observer: Observer)
    /** 조회 중단 + 자원 해제. 이후 도착하는 응답은 무시된다. */
    fun stop()
}
