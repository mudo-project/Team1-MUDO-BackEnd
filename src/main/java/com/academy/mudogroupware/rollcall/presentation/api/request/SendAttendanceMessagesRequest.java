package com.academy.mudogroupware.rollcall.presentation.api.request;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.rollcall.application.command.SendAttendanceMessagesCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SendAttendanceMessagesRequest(
        @NotEmpty List<@NotNull @Positive Long> studentIds
) {

    public SendAttendanceMessagesCommand toCommand(Long lectureId, LocalDate date) {
        return new SendAttendanceMessagesCommand(lectureId, date, studentIds);
    }
}
