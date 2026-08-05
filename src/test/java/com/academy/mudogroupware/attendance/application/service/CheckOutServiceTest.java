package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.repository.AcademyWifiIpRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;

@ExtendWith(MockitoExtension.class)
class CheckOutServiceTest {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 6, 2, 0);

    @Mock
    private AcademyWifiIpRepository academyWifiIpRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    private CheckOutService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(KOREA_ZONE).toInstant(), KOREA_ZONE);
        service = new CheckOutService(
                academyWifiIpRepository, attendanceRecordRepository, clock);
    }

    @Test
    void checksOutPreviousDayOpenRecordFromRegisteredIp() {
        AttendanceRecord openRecord = openRecord();
        when(academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                1L, "203.0.113.10")).thenReturn(true);
        when(attendanceRecordRepository.findLatestOpenSince(
                1L, 10L, LocalDate.of(2026, 8, 5)))
                .thenReturn(Optional.of(openRecord));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.checkOut(command(" 추가 근무 "));

        assertEquals(5L, result.attendanceId());
        assertEquals(NOW, result.clockOutAt());
        assertEquals("추가 근무", result.clockOutNote());
        verify(attendanceRecordRepository).save(any(AttendanceRecord.class));
    }

    @Test
    void allowsNullClockOutNote() {
        when(academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                1L, "203.0.113.10")).thenReturn(true);
        when(attendanceRecordRepository.findLatestOpenSince(
                1L, 10L, LocalDate.of(2026, 8, 5)))
                .thenReturn(Optional.of(openRecord()));
        when(attendanceRecordRepository.save(any(AttendanceRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.checkOut(command(null));

        assertNull(result.clockOutNote());
    }

    @Test
    void rejectsCheckOutFromUnregisteredIp() {
        when(academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                1L, "203.0.113.10")).thenReturn(false);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.checkOut(command(null)));

        assertSame(AttendanceErrorCode.UNREGISTERED_CHECK_OUT_IP,
                exception.getErrorCode());
        verifyNoInteractions(attendanceRecordRepository);
    }

    @Test
    void rejectsRepeatedCheckOutCompletedToday() {
        allowRegisteredIpWithoutOpenRecord();
        when(attendanceRecordRepository.existsCheckedOutBetween(
                1L, 10L, LocalDate.of(2026, 8, 6).atStartOfDay(), NOW))
                .thenReturn(true);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.checkOut(command(null)));

        assertSame(AttendanceErrorCode.ATTENDANCE_ALREADY_CHECKED_OUT,
                exception.getErrorCode());
    }

    @Test
    void rejectsCheckOutWithoutRecentCheckIn() {
        allowRegisteredIpWithoutOpenRecord();
        when(attendanceRecordRepository.existsCheckedOutBetween(
                1L, 10L, LocalDate.of(2026, 8, 6).atStartOfDay(), NOW))
                .thenReturn(false);

        AttendanceException exception = assertThrows(
                AttendanceException.class,
                () -> service.checkOut(command(null)));

        assertSame(AttendanceErrorCode.ATTENDANCE_CHECK_IN_NOT_FOUND,
                exception.getErrorCode());
    }

    private void allowRegisteredIpWithoutOpenRecord() {
        when(academyWifiIpRepository.existsByAcademyIdAndIpAddress(
                1L, "203.0.113.10")).thenReturn(true);
        when(attendanceRecordRepository.findLatestOpenSince(
                1L, 10L, LocalDate.of(2026, 8, 5)))
                .thenReturn(Optional.empty());
    }

    private AttendanceRecord openRecord() {
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 5, 22, 0);
        return AttendanceRecord.restore(
                5L, 1L, 10L, clockInAt.toLocalDate(), clockInAt,
                null, null, null, AttendanceStatus.NORMAL,
                clockInAt, clockInAt);
    }

    private CheckOutCommand command(String note) {
        return new CheckOutCommand(10L, 1L, "203.0.113.10", note);
    }
}
