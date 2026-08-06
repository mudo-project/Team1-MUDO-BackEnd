package com.academy.mudogroupware.lecture.presentation.api.request;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.lecture.application.command.ScheduleInput;

import jakarta.validation.constraints.NotNull;

public record ScheduleRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {

    public ScheduleInput toInput() {
        return new ScheduleInput(dayOfWeek, startTime, endTime);
    }
}
