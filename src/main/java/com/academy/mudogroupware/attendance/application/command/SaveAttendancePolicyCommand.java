package com.academy.mudogroupware.attendance.application.command;

import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;

public record SaveAttendancePolicyCommand(
        Long requesterId,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        int lateGraceMinutes,
        boolean weekdayExceptionEnabled,
        List<AttendancePolicyWeekday> weekdays
) {
}
