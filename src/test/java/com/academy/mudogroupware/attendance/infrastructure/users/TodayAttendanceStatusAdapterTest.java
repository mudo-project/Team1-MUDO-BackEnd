package com.academy.mudogroupware.attendance.infrastructure.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.AttendanceRecord;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.AttendanceRecordRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;
import com.academy.mudogroupware.users.application.port.MemberTodayAttendanceStatus;

@ExtendWith(MockitoExtension.class)
class TodayAttendanceStatusAdapterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);

    @Mock
    private AttendancePolicyRepository attendancePolicyRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    private TodayAttendanceStatusAdapter adapter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-08-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        adapter = new TodayAttendanceStatusAdapter(
                attendancePolicyRepository, leaveRequestRepository, attendanceRecordRepository, clock);
    }

    private AttendancePolicy workdayPolicy() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 0, 0);
        return AttendancePolicy.restore(
                1L, LocalTime.of(9, 0), LocalTime.of(18, 0), 10, false, List.of(), createdAt, createdAt);
    }

    @Test
    void returnsPresentWhenClockInRecordExists() {
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(workdayPolicy()));
        when(leaveRequestRepository.findApprovedUserIds(TODAY)).thenReturn(Set.of());
        when(attendanceRecordRepository.findAllByUserIdsAndWorkDate(List.of(1L), TODAY))
                .thenReturn(List.of(AttendanceRecord.checkIn(
                        1L, LocalDateTime.of(2026, 8, 5, 8, 52), LocalTime.of(9, 0), 10, null)));

        List<MemberTodayAttendanceStatus> result = adapter.findTodayStatusByUserIds(List.of(1L));

        assertThat(result).containsExactly(new MemberTodayAttendanceStatus(1L, "PRESENT"));
    }

    @Test
    void returnsLeaveWhenApprovedLeaveExistsAndNoClockIn() {
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(workdayPolicy()));
        when(leaveRequestRepository.findApprovedUserIds(TODAY)).thenReturn(Set.of(2L));
        when(attendanceRecordRepository.findAllByUserIdsAndWorkDate(List.of(2L), TODAY))
                .thenReturn(List.of());

        List<MemberTodayAttendanceStatus> result = adapter.findTodayStatusByUserIds(List.of(2L));

        assertThat(result).containsExactly(new MemberTodayAttendanceStatus(2L, "LEAVE"));
    }

    @Test
    void returnsAbsentWhenNoClockInAndNoLeave() {
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(workdayPolicy()));
        when(leaveRequestRepository.findApprovedUserIds(TODAY)).thenReturn(Set.of());
        when(attendanceRecordRepository.findAllByUserIdsAndWorkDate(List.of(3L), TODAY))
                .thenReturn(List.of());

        List<MemberTodayAttendanceStatus> result = adapter.findTodayStatusByUserIds(List.of(3L));

        assertThat(result).containsExactly(new MemberTodayAttendanceStatus(3L, "ABSENT"));
    }

    @Test
    void returnsOffOnNonWorkdayWithoutQueryingLeaveOrRecords() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 0, 0);
        AttendancePolicy nonWorkdayPolicy = AttendancePolicy.restore(
                1L, LocalTime.of(9, 0), LocalTime.of(18, 0), 10, true,
                List.of(new AttendancePolicyWeekday(3, false, null, null)), createdAt, createdAt);
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(nonWorkdayPolicy));

        List<MemberTodayAttendanceStatus> result = adapter.findTodayStatusByUserIds(List.of(4L));

        assertThat(result).containsExactly(new MemberTodayAttendanceStatus(4L, "OFF"));
        verifyNoInteractions(leaveRequestRepository, attendanceRecordRepository);
    }

    @Test
    void throwsWhenPolicyNotFound() {
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.findTodayStatusByUserIds(List.of(5L)))
                .isInstanceOf(AttendanceException.class);
    }

    @Test
    void returnsEmptyListWhenNoUserIdsRequestedAndPolicyExists() {
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(workdayPolicy()));

        List<MemberTodayAttendanceStatus> result = adapter.findTodayStatusByUserIds(List.of());

        assertThat(result).isEmpty();
    }

    @Test
    void throwsWhenPolicyNotFoundEvenForEmptyUserIds() {
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.findTodayStatusByUserIds(List.of()))
                .isInstanceOf(AttendanceException.class);
    }
}
