package com.academy.mudogroupware.rollcall.application.port;

public record SmsSendResult(boolean success, String failureReason) {

    public static SmsSendResult succeeded() {
        return new SmsSendResult(true, null);
    }

    public static SmsSendResult failed(String reason) {
        return new SmsSendResult(false, reason);
    }
}
