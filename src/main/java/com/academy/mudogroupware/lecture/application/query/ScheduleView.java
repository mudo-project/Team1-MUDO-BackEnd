package com.academy.mudogroupware.lecture.application.query;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleView(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
