package com.academy.mudogroupware.attendance.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.port.AttendanceCorrectionRequesterPort;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionRequest;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionType;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.model.ClockOutType;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceCorrectionRequestRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;

@ExtendWith(MockitoExtension.class)
class ManageAttendanceCorrectionServiceTest {

    private static final Long REQUEST_ID = 100L;
    private static final Long USER_ID = 10L;
    private static final LocalDate WORK_DATE = LocalDate.of(2026, 8, 6);

    @Mock
    private AttendanceCorrectionRequestRepository correctionRepository;
    @Mock
    private AttendanceRecordRepository attendanceRepository;
    @Mock
    private AttendancePolicyRepository policyRepository;
    @Mock
    private AttendanceCorrectionRequesterPort requesterPort;

    @Test
    void locksAttendanceRecordBeforeApplyingApprovedCorrection() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-06T10:00:00Z"),
                ZoneId.of("Asia/Seoul"));
        ManageAttendanceCorrectionService service = new ManageAttendanceCorrectionService(
                correctionRepository, attendanceRepository, policyRepository, requesterPort, clock);
        AttendanceRecord attendance = attendance();
        when(correctionRepository.findByIdForUpdate(REQUEST_ID))
                .thenReturn(Optional.of(request()));
        when(attendanceRepository.findByUserIdAndWorkDateForUpdate(USER_ID, WORK_DATE))
                .thenReturn(Optional.of(attendance));
        when(attendanceRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(REQUEST_ID, 20L);

        verify(attendanceRepository).findByUserIdAndWorkDateForUpdate(USER_ID, WORK_DATE);
        verify(attendanceRepository, never()).findByUserIdAndWorkDate(USER_ID, WORK_DATE);
        verify(correctionRepository).save(any(AttendanceCorrectionRequest.class));
    }

    private AttendanceCorrectionRequest request() {
        return AttendanceCorrectionRequest.restore(
                REQUEST_ID, USER_ID, 7L, WORK_DATE,
                AttendanceCorrectionType.CLOCK_OUT_NOTE, AttendanceCorrectionStatus.PENDING,
                WORK_DATE.atTime(9, 0), WORK_DATE.atTime(18, 0),
                null, "기존 메모", null, null, null, "수정 메모",
                "퇴근 메모 정정", WORK_DATE.atTime(18, 30), null, null, null);
    }

    private AttendanceRecord attendance() {
        LocalDateTime clockInAt = WORK_DATE.atTime(9, 0);
        return AttendanceRecord.restore(
                7L, USER_ID, WORK_DATE, clockInAt, null,
                WORK_DATE.atTime(18, 0), "기존 메모", ClockOutType.NORMAL,
                AttendanceStatus.NORMAL, clockInAt, WORK_DATE.atTime(18, 0));
    }
}
