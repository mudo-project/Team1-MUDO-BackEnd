package com.academy.mudogroupware.attendance.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.attendance.application.command.SubmitLeaveRequestCommand;
import com.academy.mudogroupware.attendance.application.usecase.SubmitLeaveRequestUseCase;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceErrorCode;
import com.academy.mudogroupware.attendance.domain.exception.AttendanceException;
import com.academy.mudogroupware.attendance.domain.model.AttendancePolicy;
import com.academy.mudogroupware.attendance.domain.model.LeaveGrant;
import com.academy.mudogroupware.attendance.domain.model.LeaveRequest;
import com.academy.mudogroupware.attendance.domain.repository.AttendancePolicyRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveGrantRepository;
import com.academy.mudogroupware.attendance.domain.repository.LeaveRequestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitLeaveRequestService implements SubmitLeaveRequestUseCase {

    private final AttendancePolicyRepository attendancePolicyRepository;
    private final LeaveGrantRepository leaveGrantRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveUsedDaysCalculator leaveUsedDaysCalculator;

    @Override
    @Transactional
    public void submit(SubmitLeaveRequestCommand command) {
        log.info("event=attendance_leave_request_submit_시작 academyId={}, userId={}, documentId={}", command.academyId(), command.userId(), command.documentId());
        try {
        AttendancePolicy policy = attendancePolicyRepository.findByAcademyId(command.academyId())
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.ATTENDANCE_POLICY_NOT_FOUND));
        int usedDays = leaveUsedDaysCalculator.calculate(policy, command.startDate(), command.endDate());

        LeaveGrant grant = leaveGrantRepository.findActiveForUpdate(command.academyId(), command.userId(),
                        command.submittedAt().toLocalDate())
                .orElseThrow(() -> new AttendanceException(AttendanceErrorCode.INSUFFICIENT_LEAVE_BALANCE));
        if (!grant.contains(command.startDate()) || !grant.contains(command.endDate())) {
            throw new AttendanceException(AttendanceErrorCode.INVALID_LEAVE_PERIOD);
        }

        if (leaveRequestRepository.existsOverlapping(command.academyId(), command.userId(),
                command.startDate(), command.endDate())) {
            throw new AttendanceException(AttendanceErrorCode.LEAVE_REQUEST_OVERLAP);
        }

        int reservedDays = leaveRequestRepository.sumReservedDays(command.academyId(), command.userId(),
                grant.getGrantDate(), grant.getExpirationDate());
        if (grant.getGrantedDays() - reservedDays < usedDays) {
            throw new AttendanceException(AttendanceErrorCode.INSUFFICIENT_LEAVE_BALANCE);
        }

        leaveRequestRepository.save(LeaveRequest.submit(command.academyId(), command.userId(),
                command.documentId(), command.startDate(), command.endDate(), usedDays, command.submittedAt()));
        log.info("event=attendance_leave_request_submit_완료 academyId={}, userId={}, documentId={}, usedDays={}", command.academyId(), command.userId(), command.documentId(), usedDays);
        } catch (RuntimeException e) { log.warn("event=attendance_leave_request_submit_실패 academyId={}, userId={}, reason={}", command.academyId(), command.userId(), e.getMessage()); throw e; }
    }
}
