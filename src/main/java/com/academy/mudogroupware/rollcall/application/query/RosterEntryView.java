package com.academy.mudogroupware.rollcall.application.query;

import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record RosterEntryView(
        Long studentId,
        String studentName,
        String grade,
        String parentPhone,
        AttendanceStatus status,
        String note
) {
}
