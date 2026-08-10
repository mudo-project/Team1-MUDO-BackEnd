package com.academy.mudogroupware.lecture.presentation.api.response;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.academy.mudogroupware.lecture.application.query.ScheduleView;

public record ScheduleResponse(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {

    public static ScheduleResponse from(ScheduleView view) {
        return new ScheduleResponse(view.dayOfWeek(), view.startTime(), view.endTime());
    }
}
