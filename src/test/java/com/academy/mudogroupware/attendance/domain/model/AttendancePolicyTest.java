package com.academy.mudogroupware.attendance.domain.model;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

class AttendancePolicyTest {

    @Test
    void rejectsDuplicateWeekdaySettings() {
        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> AttendancePolicy.create(
                        1L, LocalTime.of(9, 0), LocalTime.of(18, 0), 10, true,
                        List.of(
                                new AttendancePolicyWeekday(1, true, null, null),
                                new AttendancePolicyWeekday(1, false, null, null))));

        assertSame(
                AttendanceErrorCode.DUPLICATE_ATTENDANCE_POLICY_WEEKDAY,
                exception.getErrorCode());
    }

    @Test
    void rejectsTimeForNonWorkday() {
        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> new AttendancePolicyWeekday(
                        6, false, LocalTime.of(9, 0), LocalTime.of(18, 0)));

        assertSame(
                AttendanceErrorCode.INVALID_ATTENDANCE_POLICY_WEEKDAY,
                exception.getErrorCode());
    }

    @Test
    void rejectsOnlyOneWeekdayTime() {
        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> new AttendancePolicyWeekday(1, true, LocalTime.of(9, 0), null));

        assertSame(
                AttendanceErrorCode.INVALID_ATTENDANCE_POLICY_WEEKDAY,
                exception.getErrorCode());
    }
}
