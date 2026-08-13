package com.academy.mudogroupware.attendance.presentation.api.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.application.query.WeeklyEmployeeAttendanceView;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;
import com.academy.mudogroupware.global.domain.common.page.PagedResult;

class WeeklyEmployeeAttendanceResponseTest {

    @Test
    void includesRoleNameAndClockOutAtInDailyAttendance() {
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 11, 9, 0);
        LocalDateTime clockOutAt = LocalDateTime.of(2026, 8, 11, 18, 0);
        WeeklyEmployeeAttendanceView view = new WeeklyEmployeeAttendanceView(
                LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 16),
                5,
                PagedResult.of(List.of(new WeeklyEmployeeAttendanceView.Employee(
                        2L,
                        "employee",
                        "instructor",
                        1,
                        5,
                        List.of(new WeeklyEmployeeAttendanceView.Day(
                                LocalDate.of(2026, 8, 11),
                                MyAttendanceDayStatus.NORMAL,
                                clockInAt,
                                clockOutAt)))), 0, 20, 42));

        WeeklyEmployeeAttendanceResponse response = WeeklyEmployeeAttendanceResponse.from(view);

        WeeklyEmployeeAttendanceResponse.Day day = response.employees().content().get(0).days().get(0);
        assertEquals("instructor", response.employees().content().get(0).roleName());
        assertEquals(clockInAt, day.clockInAt());
        assertEquals(clockOutAt, day.clockOutAt());
        assertEquals(42, response.employees().totalElements());
        assertEquals(3, response.employees().totalPages());
        assertEquals(true, response.employees().first());
        assertEquals(false, response.employees().last());
        assertEquals(true, response.employees().hasNext());
        assertEquals(false, response.employees().hasPrevious());
    }
}
