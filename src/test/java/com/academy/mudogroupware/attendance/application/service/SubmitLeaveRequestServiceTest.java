package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.application.command.SubmitLeaveRequestCommand;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicyWeekday;
import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class SubmitLeaveRequestServiceTest {

    private static final Long ACADEMY_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 8, 6, 10, 0);

    @Mock
    private AttendancePolicyRepository attendancePolicyRepository;
    @Mock
    private LeaveGrantRepository leaveGrantRepository;
    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    private SubmitLeaveRequestService service;

    @BeforeEach
    void setUp() {
        service = new SubmitLeaveRequestService(attendancePolicyRepository, leaveGrantRepository,
                leaveRequestRepository, new LeaveUsedDaysCalculator());
    }

    @Test
    void savesPendingRequestWithWorkdaysOnly() {
        SubmitLeaveRequestCommand command = command(
                LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 10));
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(policy()));
        when(leaveRequestRepository.existsOverlapping(
                USER_ID, command.startDate(), command.endDate())).thenReturn(false);
        when(leaveGrantRepository.findActiveForUpdate(USER_ID, SUBMITTED_AT.toLocalDate()))
                .thenReturn(Optional.of(grant()));
        when(leaveRequestRepository.sumReservedDays(
                USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 7, 31)))
                .thenReturn(3);

        service.submit(command);

        ArgumentCaptor<LeaveRequest> captor = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(leaveRequestRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getUsedDays());
        assertEquals(LeaveRequestStatus.PENDING, captor.getValue().getStatus());
        InOrder inOrder = org.mockito.Mockito.inOrder(
                leaveGrantRepository, attendancePolicyRepository);
        inOrder.verify(leaveGrantRepository).findActiveForUpdate(
                USER_ID, SUBMITTED_AT.toLocalDate());
        inOrder.verify(attendancePolicyRepository).findCurrent();
    }

    @Test
    void rejectsOverlappingRequest() {
        SubmitLeaveRequestCommand command = command(
                LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 10));
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(policy()));
        when(leaveGrantRepository.findActiveForUpdate(USER_ID, SUBMITTED_AT.toLocalDate()))
                .thenReturn(Optional.of(grant()));
        when(leaveRequestRepository.existsOverlapping(
                USER_ID, command.startDate(), command.endDate())).thenReturn(true);

        AttendanceException exception = assertThrows(AttendanceException.class, () -> service.submit(command));

        assertSame(AttendanceErrorCode.LEAVE_REQUEST_OVERLAP, exception.getErrorCode());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void rejectsRequestWhenAvailableDaysAreInsufficient() {
        SubmitLeaveRequestCommand command = command(
                LocalDate.of(2026, 8, 7), LocalDate.of(2026, 8, 10));
        when(attendancePolicyRepository.findCurrent()).thenReturn(Optional.of(policy()));
        when(leaveRequestRepository.existsOverlapping(
                USER_ID, command.startDate(), command.endDate())).thenReturn(false);
        when(leaveGrantRepository.findActiveForUpdate(USER_ID, SUBMITTED_AT.toLocalDate()))
                .thenReturn(Optional.of(grant()));
        when(leaveRequestRepository.sumReservedDays(
                USER_ID, LocalDate.of(2026, 8, 1), LocalDate.of(2027, 7, 31)))
                .thenReturn(14);

        AttendanceException exception = assertThrows(AttendanceException.class, () -> service.submit(command));

        assertSame(AttendanceErrorCode.INSUFFICIENT_LEAVE_BALANCE, exception.getErrorCode());
        verify(leaveRequestRepository, never()).save(any());
    }

    private SubmitLeaveRequestCommand command(LocalDate startDate, LocalDate endDate) {
        return new SubmitLeaveRequestCommand(10L, USER_ID, startDate, endDate, SUBMITTED_AT);
    }

    private AttendancePolicy policy() {
        return AttendancePolicy.restore(1L, LocalTime.of(9, 0), LocalTime.of(18, 0), 0, true,
                List.of(
                        new AttendancePolicyWeekday(6, false, null, null),
                        new AttendancePolicyWeekday(7, false, null, null)),
                SUBMITTED_AT.minusDays(1), SUBMITTED_AT.minusDays(1));
    }

    private LeaveGrant grant() {
        return LeaveGrant.restore(1L, USER_ID, LocalDate.of(2026, 8, 1),
                LocalDate.of(2027, 7, 31), 15, SUBMITTED_AT.minusDays(5));
    }
}
