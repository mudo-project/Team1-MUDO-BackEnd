package com.academy.mudogroupware.rollcall.application.command;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record CreateMessageTemplateCommand(
        String name,
        AttendanceStatus status,
        String content,
        Long createdBy
) {
}
