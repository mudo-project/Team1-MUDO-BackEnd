package com.academy.mudogroupware.attendance.presentation.api.response;

import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.application.result.SaveAttendancePolicyResult;

public record AttendancePolicyResponse(
        Long policyId,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        int lateGraceMinutes,
        boolean weekdayExceptionEnabled,
        List<WeekdayResponse> weekdays
) {
    public static AttendancePolicyResponse from(SaveAttendancePolicyResult result) {
        return new AttendancePolicyResponse(
                result.policyId(), result.defaultStartTime(), result.defaultEndTime(),
                result.lateGraceMinutes(), result.weekdayExceptionEnabled(),
                result.weekdays().stream().map(WeekdayResponse::from).toList());
    }

    public record WeekdayResponse(
            int dayOfWeek,
            boolean isWorkday,
            LocalTime startTime,
            LocalTime endTime
    ) {
        private static WeekdayResponse from(SaveAttendancePolicyResult.WeekdayResult result) {
            return new WeekdayResponse(
                    result.dayOfWeek(), result.isWorkday(),
                    result.startTime(), result.endTime());
        }
    }
}
