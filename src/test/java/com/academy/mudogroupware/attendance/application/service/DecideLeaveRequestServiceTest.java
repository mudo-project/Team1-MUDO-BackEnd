package com.academy.mudogroupware.attendance.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

@ExtendWith(MockitoExtension.class)
class DecideLeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    void approvesPendingRequest() {
        DecideLeaveRequestService service = new DecideLeaveRequestService(leaveRequestRepository);
        LeaveRequest request = pendingRequest();
        when(leaveRequestRepository.findByDocumentId(10L)).thenReturn(Optional.of(request));

        service.decide(10L, true, LocalDateTime.of(2026, 8, 7, 10, 0));

        assertSame(LeaveRequestStatus.APPROVED, request.getStatus());
        verify(leaveRequestRepository).save(request);
    }

    @Test
    void doesNothingForGeneralApprovalDocument() {
        DecideLeaveRequestService service = new DecideLeaveRequestService(leaveRequestRepository);
        when(leaveRequestRepository.findByDocumentId(10L)).thenReturn(Optional.empty());

        service.decide(10L, true, LocalDateTime.of(2026, 8, 7, 10, 0));

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void preventsRejectingApprovedRequest() {
        DecideLeaveRequestService service = new DecideLeaveRequestService(leaveRequestRepository);
        LeaveRequest request = pendingRequest();
        request.approve(LocalDateTime.of(2026, 8, 7, 9, 0));
        when(leaveRequestRepository.findByDocumentId(10L)).thenReturn(Optional.of(request));

        AttendanceException exception = assertThrows(AttendanceException.class,
                () -> service.decide(10L, false, LocalDateTime.of(2026, 8, 7, 10, 0)));

        assertSame(AttendanceErrorCode.INVALID_LEAVE_REQUEST_STATE, exception.getErrorCode());
    }

    private LeaveRequest pendingRequest() {
        return LeaveRequest.submit(2L, 10L, LocalDate.of(2026, 8, 10),
                LocalDate.of(2026, 8, 11), 2, LocalDateTime.of(2026, 8, 6, 10, 0));
    }
}
