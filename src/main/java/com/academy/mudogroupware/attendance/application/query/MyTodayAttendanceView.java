package com.academy.mudogroupware.attendance.application.query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;

public record MyTodayAttendanceView(LocalDate date, LocalTime workStartTime,
                                    LocalTime workEndTime, OffsetDateTime clockInAt,
                                    OffsetDateTime clockOutAt, MyAttendanceDayStatus status,
                                    OffsetDateTime serverTime) {
}
