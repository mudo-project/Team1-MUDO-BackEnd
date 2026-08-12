package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.command.CheckInCommand;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 5, 9, 11);

    @Mock
    private AcademyWifiIpRepository academyWifiIpRepository;
    @Mock
    private AttendancePolicyRepository attendancePolicyRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    private CheckInService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(KOREA_ZONE).toInstant(), KOREA_ZONE);
        service = new CheckInService(
                academyWifiIpRepository, attendancePolicyRepository,
                attendanceRecordRepository, clock);
    }

    @Test
    void checksInLateEmployeeWithRequiredNoteFromRegisteredIp() {
        allowIpAndPolicy(defaultPolicy());
        when(attendanceRecordRepository.existsByUserIdAndWorkDate(
                10L, NOW.toLocalDate())).thenReturn(false);
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> {
                    AttendanceRecord record = invocation.getArgument(0);
                    return AttendanceRecord.restore(
                            5L, record.getUserId(),
                            record.getWorkDate(), record.getClockInAt(),
                            record.getClockInNote(), null, null, null,
                            record.getStatus(),
                            record.getCreatedAt(), record.getUpdatedAt());
                });

        var result = service.checkIn(command(" 교통 정체 "));

        assertEquals(5L, result.attendanceId());
        assertEquals(AttendanceStatus.LATE, result.status());
        assertEquals("교통 정체", result.clockInNote());
    }

    @Test
    void rejectsCheckInFromUnregisteredIp() {
        when(academyWifiIpRepository.existsByIpAddress(
                "203.0.113.10")).thenReturn(false);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.checkIn(command("사유")));

        assertSame(AttendanceErrorCode.UNREGISTERED_CHECK_IN_IP, exception.getErrorCode());
        verifyNoInteractions(attendancePolicyRepository, attendanceRecordRepository);
    }

    @Test
    void rejectsLateCheckInWithoutNote() {
        allowIpAndPolicy(defaultPolicy());

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.checkIn(command(null)));

        assertSame(AttendanceErrorCode.LATE_NOTE_REQUIRED, exception.getErrorCode());
    }

    @Test
    void allowsCheckInOnConfiguredNonWorkdayForHolidayWorkRecording() {
        AttendancePolicy policy = AttendancePolicy.restore(
                1L, LocalTime.of(9, 0), LocalTime.of(18, 0),
                10, true, List.of(new AttendancePolicyWeekday(3, false, null, null)),
                NOW.minusDays(1), NOW.minusDays(1));
        allowIpAndPolicy(policy);
        when(attendanceRecordRepository.existsByUserIdAndWorkDate(
                10L, NOW.toLocalDate())).thenReturn(false);
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> {
                    AttendanceRecord record = invocation.getArgument(0);
                    return AttendanceRecord.restore(
                            5L, record.getUserId(), record.getWorkDate(), record.getClockInAt(),
                            record.getClockInNote(), null, null, null, record.getStatus(),
                            record.getCreatedAt(), record.getUpdatedAt());
                });

        var result = service.checkIn(command("휴일 근무"));

        assertEquals(5L, result.attendanceId());
    }

    @Test
    void rejectsDuplicateCheckInForSameWorkDate() {
        allowIpAndPolicy(defaultPolicy());
        when(attendanceRecordRepository.existsByUserIdAndWorkDate(
                10L, NOW.toLocalDate())).thenReturn(true);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.checkIn(command("사유")));

        assertSame(AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_IN,
                exception.getErrorCode());
    }

    private void allowIpAndPolicy(AttendancePolicy policy) {
        when(academyWifiIpRepository.existsByIpAddress(
                "203.0.113.10")).thenReturn(true);
        when(attendancePolicyRepository.findCurrent())
                .thenReturn(Optional.of(policy));
    }

    private AttendancePolicy defaultPolicy() {
        return AttendancePolicy.restore(
                1L, LocalTime.of(9, 0), LocalTime.of(18, 0),
                10, false, List.of(), NOW.minusDays(1), NOW.minusDays(1));
    }

    private CheckInCommand command(String note) {
        return new CheckInCommand(10L, "203.0.113.10", note);
    }
}
