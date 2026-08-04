package com.academy.mudogroupware.attendance.domain.model;

import java.time.LocalTime;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

public record AttendancePolicyWeekday(
        int dayOfWeek,
        boolean workday,
        LocalTime startTime,
        LocalTime endTime
) {
    public AttendancePolicyWeekday {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_POLICY_WEEKDAY);
        }
        if (!workday && (startTime != null || endTime != null)) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_POLICY_WEEKDAY);
        }
        if (workday && ((startTime == null) != (endTime == null))) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_POLICY_WEEKDAY);
        }
        if (startTime != null && startTime.equals(endTime)) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_ATTENDANCE_POLICY_WEEKDAY);
        }
    }
}
