package com.academy.mudogroupware.attendance.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.usecase.DecideLeaveRequestUseCase;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DecideLeaveRequestService implements DecideLeaveRequestUseCase {

    private final LeaveRequestRepository leaveRequestRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decide(Long documentId, boolean approved, LocalDateTime decidedAt) {
        leaveRequestRepository.findByDocumentId(documentId).ifPresent(leaveRequest -> {
            if (approved) {
                leaveRequest.approve(decidedAt);
            } else {
                leaveRequest.reject(decidedAt);
            }
            leaveRequestRepository.save(leaveRequest);
        });
    }
}
