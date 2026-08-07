package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

public record MyMonthlyAttendanceView(int year, int month, List<Day> days) {
    public MyMonthlyAttendanceView {
        days = List.copyOf(days);
    }

    public record Day(LocalDate date, MyAttendanceDayStatus status,
                      LocalTime clockInAt, LocalTime clockOutAt) {
    }
}
