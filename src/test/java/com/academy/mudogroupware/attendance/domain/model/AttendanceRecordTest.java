package com.academy.mudogroupware.attendance.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;

class AttendanceRecordTest {

    @Test
    void treatsLateThresholdAsNormal() {
        AttendanceRecord record = AttendanceRecord.checkIn(
                1L, 10L, LocalDateTime.of(2026, 8, 5, 9, 10),
                LocalTime.of(9, 0), 10, null);

        assertEquals(AttendanceStatus.NORMAL, record.getStatus());
        assertNull(record.getClockInNote());
    }

    @Test
    void requiresNoteAfterLateThreshold() {
        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> AttendanceRecord.checkIn(
                        1L, 10L, LocalDateTime.of(2026, 8, 5, 9, 10, 1),
                        LocalTime.of(9, 0), 10, "   "));

        assertSame(AttendanceErrorCode.LATE_NOTE_REQUIRED, exception.getErrorCode());
    }

    @Test
    void trimsLateNote() {
        AttendanceRecord record = AttendanceRecord.checkIn(
                1L, 10L, LocalDateTime.of(2026, 8, 5, 9, 11),
                LocalTime.of(9, 0), 10, " 교통 정체 ");

        assertEquals(AttendanceStatus.LATE, record.getStatus());
        assertEquals("교통 정체", record.getClockInNote());
    }

    @Test
    void checksOutWithOptionalTrimmedNote() {
        AttendanceRecord checkedIn = AttendanceRecord.restore(
                5L, 1L, 10L, java.time.LocalDate.of(2026, 8, 5),
                LocalDateTime.of(2026, 8, 5, 22, 0), null,
                null, null, null, AttendanceStatus.NORMAL,
                LocalDateTime.of(2026, 8, 5, 22, 0),
                LocalDateTime.of(2026, 8, 5, 22, 0));

        AttendanceRecord checkedOut = checkedIn.checkOut(
                LocalDateTime.of(2026, 8, 6, 2, 0),
                ClockOutType.OVERTIME, " 추가 근무 ");

        assertEquals(LocalDateTime.of(2026, 8, 6, 2, 0), checkedOut.getClockOutAt());
        assertEquals("추가 근무", checkedOut.getClockOutNote());
        assertEquals(ClockOutType.OVERTIME, checkedOut.getClockOutType());
        assertEquals(AttendanceStatus.NORMAL, checkedOut.getStatus());
    }

    @Test
    void convertsBlankClockOutNoteToNull() {
        AttendanceRecord checkedIn = AttendanceRecord.restore(
                5L, 1L, 10L, java.time.LocalDate.of(2026, 8, 5),
                LocalDateTime.of(2026, 8, 5, 9, 0), null,
                null, null, null, AttendanceStatus.NORMAL,
                LocalDateTime.of(2026, 8, 5, 9, 0),
                LocalDateTime.of(2026, 8, 5, 9, 0));

        AttendanceRecord checkedOut = checkedIn.checkOut(
                LocalDateTime.of(2026, 8, 5, 18, 0),
                ClockOutType.NORMAL, "   ");

        assertNull(checkedOut.getClockOutNote());
    }

    @Test
    void requiresNoteForOvertimeCheckOut() {
        AttendanceRecord checkedIn = AttendanceRecord.restore(
                5L, 1L, 10L, java.time.LocalDate.of(2026, 8, 5),
                LocalDateTime.of(2026, 8, 5, 9, 0), null,
                null, null, null, AttendanceStatus.NORMAL,
                LocalDateTime.of(2026, 8, 5, 9, 0),
                LocalDateTime.of(2026, 8, 5, 9, 0));

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> checkedIn.checkOut(
                        LocalDateTime.of(2026, 8, 5, 19, 0),
                        ClockOutType.OVERTIME, "   "));

        assertSame(AttendanceErrorCode.OVERTIME_NOTE_REQUIRED,
                exception.getErrorCode());
    }
}
