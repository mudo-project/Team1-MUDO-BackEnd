package com.academy.mudogroupware.rollcall.application.command;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record CreateMessageTemplateCommand(
        Long academyId,
        String name,
        AttendanceStatus status,
        String content,
        Long createdBy
) {
}
