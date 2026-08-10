package com.academy.mudogroupware.attendance.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

class MyAttendanceStatusResolverTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);
    private final MyAttendanceStatusResolver resolver = new MyAttendanceStatusResolver();

    @Test
    void returnsUnrecordedBeforeWorkEndAndAbsentAfterWorkEnd() {
        var schedule = new MyAttendanceScheduleResolver.WorkSchedule(
                true, LocalTime.of(9, 0), LocalTime.of(18, 0));

        assertEquals(MyAttendanceDayStatus.UNRECORDED,
                resolver.resolve(TODAY, schedule, null, List.of(), TODAY, LocalTime.NOON));
        assertEquals(MyAttendanceDayStatus.ABSENT,
                resolver.resolve(TODAY, schedule, null, List.of(), TODAY, LocalTime.of(18, 0)));
    }

    @Test
    void returnsOffBeforeOtherStatusesOnNonWorkday() {
        var schedule = new MyAttendanceScheduleResolver.WorkSchedule(
                false, LocalTime.of(9, 0), LocalTime.of(18, 0));

        assertEquals(MyAttendanceDayStatus.OFF,
                resolver.resolve(TODAY, schedule, normalRecord(), List.of(), TODAY, LocalTime.NOON));
    }

    @Test
    void returnsRecordedStatusAndApprovedLeave() {
        var schedule = new MyAttendanceScheduleResolver.WorkSchedule(
                true, LocalTime.of(9, 0), LocalTime.of(18, 0));

        assertEquals(MyAttendanceDayStatus.NORMAL,
                resolver.resolve(TODAY, schedule, normalRecord(), List.of(), TODAY, LocalTime.NOON));
        assertEquals(MyAttendanceDayStatus.LEAVE,
                resolver.resolve(TODAY, schedule, null, List.of(approvedLeave()), TODAY, LocalTime.NOON));
    }

    private AttendanceRecord normalRecord() {
        LocalDateTime clockInAt = TODAY.atTime(8, 55);
        return AttendanceRecord.restore(
                1L, 2L, TODAY, clockInAt, null, null, null, null,
                AttendanceStatus.NORMAL, clockInAt, clockInAt);
    }

    private LeaveRequest approvedLeave() {
        LocalDateTime now = TODAY.atStartOfDay();
        return LeaveRequest.restore(
                1L, 2L, 100L, TODAY, TODAY, 1,
                LeaveRequestStatus.APPROVED, now, now);
    }
}
