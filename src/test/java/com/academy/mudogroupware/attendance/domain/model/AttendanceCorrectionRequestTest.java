package com.academy.mudogroupware.attendance.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.academy.mudogroupware.attendance.domain.exception.*;

class AttendanceCorrectionRequestTest {
    private static final LocalDate DATE = LocalDate.of(2026, 8, 5);
    private static final LocalDateTime REQUESTED_AT = DATE.atTime(20, 0);

    @Test
    void snapshotsBothNotesWhenRequestingClockInTimeCorrection() {
        AttendanceCorrectionRequest request = AttendanceCorrectionRequest.submit(
                1L, 10L, attendance(), DATE, AttendanceCorrectionType.CLOCK_IN_TIME,
                DATE.atTime(9, 0), null, null, null, " 버튼을 늦게 누름 ", REQUESTED_AT);
        assertEquals("출근 메모", request.getOriginalClockInNote());
        assertEquals("퇴근 메모", request.getOriginalClockOutNote());
        assertEquals("버튼을 늦게 누름", request.getReason());
        assertEquals(AttendanceCorrectionStatus.PENDING, request.getStatus());
    }

    @Test
    void separatesClockOutNoteCorrectionFromClockInNote() {
        AttendanceCorrectionRequest request = AttendanceCorrectionRequest.submit(
                1L, 10L, attendance(), DATE, AttendanceCorrectionType.CLOCK_OUT_NOTE,
                null, null, null, " 외근 후 퇴근 ", "비고 오입력", REQUESTED_AT);
        assertNull(request.getRequestedClockInNote());
        assertEquals("외근 후 퇴근", request.getRequestedClockOutNote());
    }

    @Test
    void missingRecordRequiresNoExistingAttendance() {
        AttendanceException exception = assertThrows(AttendanceException.class,
                () -> AttendanceCorrectionRequest.submit(1L, 10L, attendance(), DATE,
                        AttendanceCorrectionType.MISSING_RECORD, DATE.atTime(9, 0),
                        DATE.atTime(18, 0), null, null, "누락", REQUESTED_AT));
        assertSame(AttendanceErrorCode.INVALID_CORRECTION_REQUEST, exception.getErrorCode());
    }

    @Test
    void rejectsFieldsUnrelatedToSelectedType() {
        assertThrows(AttendanceException.class,
                () -> AttendanceCorrectionRequest.submit(1L, 10L, attendance(), DATE,
                        AttendanceCorrectionType.CLOCK_IN_NOTE, DATE.atTime(9, 0), null,
                        "수정 메모", null, "사유", REQUESTED_AT));
    }

    private AttendanceRecord attendance() {
        return AttendanceRecord.restore(7L, 1L, 10L, DATE, DATE.atTime(9, 5), "출근 메모",
                DATE.atTime(18, 0), "퇴근 메모", ClockOutType.NORMAL, AttendanceStatus.NORMAL,
                DATE.atTime(9, 5), DATE.atTime(18, 0));
    }
}
