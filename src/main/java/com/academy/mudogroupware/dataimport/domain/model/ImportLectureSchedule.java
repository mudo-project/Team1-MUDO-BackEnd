package com.academy.mudogroupware.dataimport.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ImportLectureSchedule(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {
}
