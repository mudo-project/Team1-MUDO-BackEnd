package com.academy.mudogroupware.rollcall.presentation.api.response;

import com.academy.mudogroupware.rollcall.application.query.MessageSendResultView;

public record MessageSendResultResponse(
        Long studentId,
        String studentName,
        boolean sent,
        String failureReason
) {

    public static MessageSendResultResponse from(MessageSendResultView view) {
        return new MessageSendResultResponse(view.studentId(), view.studentName(), view.sent(),
                view.failureReason());
    }
}
