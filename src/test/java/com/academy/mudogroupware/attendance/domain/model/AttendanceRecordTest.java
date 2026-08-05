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

    @Test
    void rejectsCheckOutBeforeClockIn() {
        AttendanceRecord checkedIn = openRecord();

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> checkedIn.checkOut(
                        LocalDateTime.of(2026, 8, 5, 8, 59),
                        ClockOutType.NORMAL, null));

        assertSame(AttendanceErrorCode.INVALID_CLOCK_OUT_TIME,
                exception.getErrorCode());
    }

    @Test
    void rejectsSecondCheckOut() {
        AttendanceRecord checkedOut = openRecord().checkOut(
                LocalDateTime.of(2026, 8, 5, 18, 0),
                ClockOutType.NORMAL, null);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> checkedOut.checkOut(
                        LocalDateTime.of(2026, 8, 5, 18, 1),
                        ClockOutType.NORMAL, null));

        assertSame(AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT,
                exception.getErrorCode());
    }

    @Test
    void acceptsClockOutNoteWith255Characters() {
        String note = "a".repeat(255);

        AttendanceRecord checkedOut = openRecord().checkOut(
                LocalDateTime.of(2026, 8, 5, 18, 0),
                ClockOutType.NORMAL, note);

        assertEquals(note, checkedOut.getClockOutNote());
    }

    @Test
    void rejectsClockOutNoteWith256Characters() {
        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> openRecord().checkOut(
                        LocalDateTime.of(2026, 8, 5, 18, 0),
                        ClockOutType.NORMAL, "a".repeat(256)));

        assertSame(AttendanceErrorCode.INVALID_CLOCK_OUT_NOTE,
                exception.getErrorCode());
    }

    private AttendanceRecord openRecord() {
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 5, 9, 0);
        return AttendanceRecord.restore(
                5L, 1L, 10L, clockInAt.toLocalDate(), clockInAt, null,
                null, null, null, AttendanceStatus.NORMAL,
                clockInAt, clockInAt);
    }
}
