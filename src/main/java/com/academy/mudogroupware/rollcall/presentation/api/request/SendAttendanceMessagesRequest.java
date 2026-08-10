package com.academy.mudogroupware.rollcall.presentation.api.request;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.rollcall.application.command.SendAttendanceMessagesCommand;

import jakarta.validation.constraints.NotEmpty;

public record SendAttendanceMessagesRequest(
        @NotEmpty List<Long> studentIds
) {

    public SendAttendanceMessagesCommand toCommand(Long lectureId, Long academyId, LocalDate date) {
        return new SendAttendanceMessagesCommand(lectureId, academyId, date, studentIds);
    }
}
