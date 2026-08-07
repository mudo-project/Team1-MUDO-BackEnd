package com.academy.mudogroupware.attendance.application.command;
import java.time.LocalDate;
import java.time.LocalTime;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionType;
public record CreateAttendanceCorrectionCommand(Long academyId, Long userId, LocalDate date,
        AttendanceCorrectionType type, LocalTime requestedClockInTime, LocalTime requestedClockOutTime,
        String requestedClockInNote, String requestedClockOutNote, String reason) {}
