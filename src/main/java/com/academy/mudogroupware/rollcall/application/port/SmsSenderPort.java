package com.academy.mudogroupware.rollcall.application.port;

public interface SmsSenderPort {

    /**
     * 단일 수신자에게 문자를 발송한다. 외부 공급자 호출 자체가 실패해도(네트워크 오류 등)
     * 예외를 던지지 않고 {@link SmsSendResult#failed(String)}로 반환한다 — 여러 학생에게
     * 발송하는 도중 하나가 실패해도 나머지 발송을 계속하기 위함이다.
     */
    SmsSendResult send(String receiverPhone, String message);
}
