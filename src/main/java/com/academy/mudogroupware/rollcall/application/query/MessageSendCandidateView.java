package com.academy.mudogroupware.rollcall.application.query;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record MessageSendCandidateView(
        Long studentId,
        String studentName,
        AttendanceStatus status,
        String parentPhone,
        Long matchedTemplateId,
        String matchedTemplateName,
        boolean eligible
) {
}
