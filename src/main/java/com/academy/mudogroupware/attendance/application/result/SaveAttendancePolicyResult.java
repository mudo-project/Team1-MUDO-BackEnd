package com.academy.mudogroupware.attendance.application.result;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;

public record SaveAttendancePolicyResult(
        Long policyId,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        int lateGraceMinutes,
        boolean weekdayExceptionEnabled,
        List<WeekdayResult> weekdays
) {
    public static SaveAttendancePolicyResult from(AttendancePolicy policy) {
        List<WeekdayResult> weekdays = policy.getWeekdays().stream()
                .sorted(Comparator.comparingInt(weekday -> weekday.dayOfWeek()))
                .map(weekday -> new WeekdayResult(
                        weekday.dayOfWeek(), weekday.workday(),
                        weekday.startTime(), weekday.endTime()))
                .toList();
        return new SaveAttendancePolicyResult(
                policy.getId(), policy.getDefaultStartTime(), policy.getDefaultEndTime(),
                policy.getLateGraceMinutes(), policy.isWeekdayExceptionEnabled(), weekdays);
    }

    public record WeekdayResult(
            int dayOfWeek,
            boolean isWorkday,
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
