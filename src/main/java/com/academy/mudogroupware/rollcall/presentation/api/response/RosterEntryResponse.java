package com.academy.mudogroupware.rollcall.presentation.api.response;

import com.academy.mudogroupware.rollcall.application.query.RosterEntryView;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

public record RosterEntryResponse(
        Long studentId,
        String studentName,
        String grade,
        String parentPhone,
        AttendanceStatus status,
        String note
) {

    public static RosterEntryResponse from(RosterEntryView view) {
        return new RosterEntryResponse(view.studentId(), view.studentName(), view.grade(), view.parentPhone(),
                view.status(), view.note());
    }
}
