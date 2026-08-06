package com.academy.mudogroupware.rollcall.presentation.api.response;

import com.academy.mudogroupware.rollcall.application.query.MessageSendCandidateView;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record MessageSendCandidateResponse(
        Long studentId,
        String studentName,
        AttendanceStatus status,
        String parentPhone,
        Long matchedTemplateId,
        String matchedTemplateName,
        boolean eligible
) {

    public static MessageSendCandidateResponse from(MessageSendCandidateView view) {
        return new MessageSendCandidateResponse(view.studentId(), view.studentName(), view.status(),
                view.parentPhone(), view.matchedTemplateId(), view.matchedTemplateName(), view.eligible());
    }
}
