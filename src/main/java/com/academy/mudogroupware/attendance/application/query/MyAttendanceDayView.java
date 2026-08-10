package com.academy.mudogroupware.attendance.application.query;
import java.time.LocalDate;
import java.time.LocalDateTime;
public record MyAttendanceDayView(LocalDate date, LocalDateTime clockInAt, LocalDateTime clockOutAt,
        String clockInNote, String clockOutNote, boolean correctionRequestPending) {}
