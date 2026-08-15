package com.academy.mudogroupware.rollcall.application.port;

public record SmsSendResult(boolean success, boolean indeterminate, String failureReason) {

    public static SmsSendResult succeeded() {
        return new SmsSendResult(true, false, null);
    }

    /**
     * 공급자가 명확한 실패 응답을 준 경우. 재시도해도 SMS가 중복 발송될 위험은 없다.
     */
    public static SmsSendResult failed(String reason) {
        return new SmsSendResult(false, false, reason);
    }

    /**
     * 응답을 받기 전에 타임아웃/연결 끊김이 발생해 실제 발송 여부를 알 수 없는 경우.
     */
    public static SmsSendResult indeterminate(String reason) {
        return new SmsSendResult(false, true, reason);
    }
}
