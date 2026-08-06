package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.TeamAttendanceStatus;

public record TodayTeamAttendanceView(
        LocalDate date,
        String dayOfWeek,
        LocalTime regularWorkStartTime,
        LocalTime regularWorkEndTime,
        Summary summary,
        List<Employee> employees
) {
    public record Summary(
            int presentCount,
            int absentCount,
            int offCount
    ) {
    }

    public record Employee(
            Long userId,
            String name,
            TeamAttendanceStatus status,
            LocalTime checkInTime
    ) {
    }
}
