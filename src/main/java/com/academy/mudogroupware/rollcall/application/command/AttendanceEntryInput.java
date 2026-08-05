package com.academy.mudogroupware.rollcall.application.command;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record AttendanceEntryInput(
        Long studentId,
        AttendanceStatus status,
        String note
) {
}
