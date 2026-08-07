package com.academy.mudogroupware.attendance.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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

import com.academy.mudogroupware.attendance.application.port.EmploymentSummaryPort;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;
import com.academy.mudogroupware.attendance.domain.model.MyAttendanceDayStatus;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class MyMonthlyAttendanceQueryServiceTest {

    private static final Long USER_ID = 2L;
    private static final Long ACADEMY_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 7);

    @Mock AttendancePolicyRepository policyRepository;
    @Mock AttendanceRecordRepository recordRepository;
    @Mock LeaveRequestRepository leaveRepository;
    @Mock EmploymentSummaryPort employmentPort;

    private MyMonthlyAttendanceQueryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-07T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new MyMonthlyAttendanceQueryService(
                policyRepository, recordRepository, leaveRepository, employmentPort,
                new MyAttendanceScheduleResolver(), new MyAttendanceStatusResolver(), clock);
    }

    @Test
    void returnsOnlyFromHireDateThroughTodayWithCalculatedStatuses() {
        LocalDate hireDate = LocalDate.of(2026, 8, 3);
        var record = attendanceRecord(hireDate);
        var leave = approvedLeave(LocalDate.of(2026, 8, 4));
        when(employmentPort.findByUserIdAndAcademyId(USER_ID, ACADEMY_ID))
                .thenReturn(Optional.of(new EmploymentSummaryPort.EmploymentSummary(hireDate)));
        when(policyRepository.findByAcademyId(ACADEMY_ID)).thenReturn(Optional.of(policy()));
        when(recordRepository.findByAcademyIdAndUserIdAndWorkDateBetween(
                ACADEMY_ID, USER_ID, hireDate, TODAY)).thenReturn(List.of(record));
        when(leaveRepository.findApprovedOverlapping(
                ACADEMY_ID, USER_ID, hireDate, TODAY)).thenReturn(List.of(leave));

        var result = service.getMonthly(USER_ID, ACADEMY_ID, 2026, 8);

        assertEquals(5, result.days().size());
        assertEquals(MyAttendanceDayStatus.NORMAL, result.days().get(0).status());
        assertEquals(MyAttendanceDayStatus.LEAVE, result.days().get(1).status());
        assertEquals(MyAttendanceDayStatus.ABSENT, result.days().get(2).status());
        assertEquals(MyAttendanceDayStatus.UNRECORDED, result.days().get(4).status());
    }

    private AttendancePolicy policy() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 0, 0);
        return AttendancePolicy.restore(
                1L, ACADEMY_ID, LocalTime.of(9, 0), LocalTime.of(18, 0),
                10, false, List.of(), createdAt, createdAt);
    }

    private AttendanceRecord attendanceRecord(LocalDate date) {
        LocalDateTime clockInAt = date.atTime(8, 55);
        return AttendanceRecord.restore(
                1L, ACADEMY_ID, USER_ID, date, clockInAt, null,
                date.atTime(18, 0), null, null, AttendanceStatus.NORMAL,
                clockInAt, date.atTime(18, 0));
    }

    private LeaveRequest approvedLeave(LocalDate date) {
        LocalDateTime now = date.atStartOfDay();
        return LeaveRequest.restore(
                1L, ACADEMY_ID, USER_ID, 100L, date, date, 1,
                LeaveRequestStatus.APPROVED, now, now);
    }
}
