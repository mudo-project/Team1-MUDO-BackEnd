package com.academy.mudogroupware.lecture.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleInput(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
