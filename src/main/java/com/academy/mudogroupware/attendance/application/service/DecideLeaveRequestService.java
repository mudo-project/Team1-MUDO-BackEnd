package com.academy.mudogroupware.attendance.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.DecideLeaveRequestUseCase;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DecideLeaveRequestService implements DecideLeaveRequestUseCase {

    private final LeaveRequestRepository leaveRequestRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decide(Long documentId, boolean approved, LocalDateTime decidedAt) {
        log.info("event=attendance_leave_request_decide_시작 documentId={}, approved={}", documentId, approved);
        try {
        leaveRequestRepository.findByDocumentId(documentId).ifPresent(leaveRequest -> {
            if (approved) {
                leaveRequest.approve(decidedAt);
            } else {
                leaveRequest.reject(decidedAt);
            }
            leaveRequestRepository.save(leaveRequest);
        });
        log.info("event=attendance_leave_request_decide_완료 documentId={}, approved={}", documentId, approved);
        } catch (RuntimeException e) { log.warn("event=attendance_leave_request_decide_실패 documentId={}, reason={}", documentId, e.getMessage()); throw e; }
    }
}
