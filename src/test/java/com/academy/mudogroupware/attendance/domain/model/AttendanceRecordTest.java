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
}
