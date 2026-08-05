package com.academy.mudogroupware.rollcall.presentation.api.request;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.rollcall.application.command.AttendanceEntryInput;
import com.academy.mudogroupware.rollcall.application.command.SaveAttendanceEntriesCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SaveAttendanceEntriesRequest(
        @NotEmpty List<@Valid AttendanceEntryRequestItem> entries
) {

    public SaveAttendanceEntriesCommand toCommand(Long lectureId, Long academyId, LocalDate date) {
        List<AttendanceEntryInput> inputs = entries.stream().map(AttendanceEntryRequestItem::toInput).toList();
        return new SaveAttendanceEntriesCommand(lectureId, academyId, date, inputs);
    }
}
