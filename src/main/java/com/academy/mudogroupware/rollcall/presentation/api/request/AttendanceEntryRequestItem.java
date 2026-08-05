package com.academy.mudogroupware.rollcall.presentation.api.request;

import com.academy.mudogroupware.rollcall.application.command.AttendanceEntryInput;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

import jakarta.validation.constraints.NotNull;

public record AttendanceEntryRequestItem(
        @NotNull Long studentId,
        @NotNull AttendanceStatus status,
        String note
) {

    public AttendanceEntryInput toInput() {
        return new AttendanceEntryInput(studentId, status, note);
    }
}
