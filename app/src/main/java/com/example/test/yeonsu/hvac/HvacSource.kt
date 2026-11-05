package com.example.test.yeonsu.hvac

/**
 * 공조·시야 신호 수신원 추상화.
 *
 * 실제 구현(VehicleHvacSource)은 Pleos Vehicle SDK 의 HVAC/Seat/SideMirror API 를 쓰지만,
 * 이 인터페이스 덕분에 컨트롤러/판정 로직은 SDK 없이도(가짜 수신원으로) 단위 테스트할 수 있다.
 *
 * 각 퀘스트(HvacQuest)의 값(Boolean/Int/Float)을 "목표 조건 충족 여부(isTarget)"로 바꿔 올린다.
 * - 못 읽거나 불명이면 null 을 넘긴다(완료로 취급하지 않음).
 *
 * 다른 수신원(문/창문/전조등)과 같은 구조를 따른다.
 * 콜백은 순서 로직과 같은 스레드(메인)에서 호출된다고 가정한다.
 */
interface HvacSource {
    interface Observer {
        /** 현재 목표 조건 충족 여부(초기 1회). 못 읽으면 null. */
        fun onInitial(isTarget: Boolean?)
        /** 이후 변경 수신. */
        fun onUpdate(isTarget: Boolean?)
        /** 구독 실패/오류. 완료로 취급하지 않는다. */
        fun onError(reason: String)
    }

    /** 초기화 + 현재값 읽기 + 구독 시작. */
    fun start(observer: Observer)
    /** 구독 해제 + 자원 해제. */
    fun stop()
}
