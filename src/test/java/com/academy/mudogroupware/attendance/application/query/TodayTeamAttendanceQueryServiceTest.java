package com.academy.mudogroupware.attendance.application.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.port.TeamAttendanceEmployee;
import com.academy.mudogroupware.attendance.application.port.TeamAttendanceQueryPort;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.TeamAttendanceStatus;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class TodayTeamAttendanceQueryServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long ACADEMY_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

    @Mock
    private AttendancePolicyRepository attendancePolicyRepository;
    @Mock
    private TeamAttendanceQueryPort teamAttendanceQueryPort;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    private TodayTeamAttendanceQueryService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new TodayTeamAttendanceQueryService(
                attendancePolicyRepository,
                teamAttendanceQueryPort, leaveRequestRepository, clock);
    }

    @Test
    void returnsPresentAndAbsentEmployeesOnWorkday() {
        AttendancePolicy policy = policy(false, List.of());
        when(attendancePolicyRepository.findCurrent())
                .thenReturn(Optional.of(policy));
        when(leaveRequestRepository.findApprovedUserIds(TODAY))
                .thenReturn(Set.of());
        when(teamAttendanceQueryPort.findEmployeesWithAttendance(
                OWNER_ID, TODAY))
                .thenReturn(List.of(
                        new TeamAttendanceEmployee(
                                2L, "김지수", LocalDateTime.of(2026, 8, 5, 8, 52)),
                        new TeamAttendanceEmployee(3L, "박서연", null)));

        TodayTeamAttendanceView result = service.getToday(OWNER_ID);

        assertEquals(TODAY, result.date());
        assertEquals("수", result.dayOfWeek());
        assertEquals(LocalTime.of(9, 0), result.regularWorkStartTime());
        assertEquals(LocalTime.of(18, 0), result.regularWorkEndTime());
        assertEquals(1, result.summary().presentCount());
        assertEquals(1, result.summary().absentCount());
        assertEquals(0, result.summary().offCount());
        assertEquals(0, result.summary().leaveCount());
        assertEquals(TeamAttendanceStatus.PRESENT, result.employees().get(0).status());
        assertEquals(LocalTime.of(8, 52), result.employees().get(0).checkInTime());
        assertEquals(TeamAttendanceStatus.ABSENT, result.employees().get(1).status());
        assertNull(result.employees().get(1).checkInTime());
    }

    @Test
    void returnsLeaveForEmployeeWithApprovedLeaveRequestToday() {
        AttendancePolicy policy = policy(false, List.of());
        when(attendancePolicyRepository.findCurrent())
                .thenReturn(Optional.of(policy));
        when(leaveRequestRepository.findApprovedUserIds(TODAY))
                .thenReturn(Set.of(3L));
        when(teamAttendanceQueryPort.findEmployeesWithAttendance(
                OWNER_ID, TODAY))
                .thenReturn(List.of(new TeamAttendanceEmployee(3L, "박서연", null)));

        TodayTeamAttendanceView result = service.getToday(OWNER_ID);

        assertEquals(TeamAttendanceStatus.LEAVE, result.employees().get(0).status());
        assertNull(result.employees().get(0).checkInTime());
        assertEquals(0, result.summary().absentCount());
        assertEquals(1, result.summary().leaveCount());
    }

    @Test
    void returnsAllEmployeesAsOffOnConfiguredNonWorkday() {
        AttendancePolicy policy = policy(
                true, List.of(new AttendancePolicyWeekday(3, false, null, null)));
        when(attendancePolicyRepository.findCurrent())
                .thenReturn(Optional.of(policy));
        when(leaveRequestRepository.findApprovedUserIds(TODAY))
                .thenReturn(Set.of());
        when(teamAttendanceQueryPort.findEmployeesWithAttendance(
                OWNER_ID, TODAY))
                .thenReturn(List.of(
                        new TeamAttendanceEmployee(2L, "김지수", null),
                        new TeamAttendanceEmployee(3L, "박서연", null)));

        TodayTeamAttendanceView result = service.getToday(OWNER_ID);

        assertEquals(0, result.summary().presentCount());
        assertEquals(0, result.summary().absentCount());
        assertEquals(2, result.summary().offCount());
        assertEquals(
                List.of(TeamAttendanceStatus.OFF, TeamAttendanceStatus.OFF),
                result.employees().stream().map(TodayTeamAttendanceView.Employee::status).toList());
    }

    @Test
    void usesConfiguredWeekdayWorkHours() {
        AttendancePolicy policy = policy(
                true,
                List.of(new AttendancePolicyWeekday(
                        3, true, LocalTime.of(10, 0), LocalTime.of(19, 0))));
        when(attendancePolicyRepository.findCurrent())
                .thenReturn(Optional.of(policy));
        when(leaveRequestRepository.findApprovedUserIds(TODAY))
                .thenReturn(Set.of());
        when(teamAttendanceQueryPort.findEmployeesWithAttendance(
                OWNER_ID, TODAY)).thenReturn(List.of());

        TodayTeamAttendanceView result = service.getToday(OWNER_ID);

        assertEquals(LocalTime.of(10, 0), result.regularWorkStartTime());
        assertEquals(LocalTime.of(19, 0), result.regularWorkEndTime());
    }

    private AttendancePolicy policy(
            boolean weekdayExceptionEnabled,
            List<AttendancePolicyWeekday> weekdays) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 0, 0);
        return AttendancePolicy.restore(
                1L,
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                10,
                weekdayExceptionEnabled,
                weekdays,
                createdAt,
                createdAt);
    }
}
