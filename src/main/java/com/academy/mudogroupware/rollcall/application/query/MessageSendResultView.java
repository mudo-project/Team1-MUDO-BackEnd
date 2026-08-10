package com.academy.mudogroupware.rollcall.application.query;

public record MessageSendResultView(
        Long studentId,
        String studentName,
        boolean sent,
        String failureReason
) {
}
