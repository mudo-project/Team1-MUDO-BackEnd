package com.academy.mudogroupware.attendance.infrastructure.approval;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.academy.mudogroupware.approval.application.port.LeaveRequestSubmissionPort;
import com.academy.mudogroupware.attendance.application.command.SubmitLeaveRequestCommand;
import com.academy.mudogroupware.attendance.application.usecase.SubmitLeaveRequestUseCase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LeaveRequestSubmissionAdapter implements LeaveRequestSubmissionPort {

    private final SubmitLeaveRequestUseCase submitLeaveRequestUseCase;

    @Override
    public void submit(Long documentId, Long requesterId, LocalDate startDate, LocalDate endDate,
                       LocalDateTime submittedAt) {
        submitLeaveRequestUseCase.submit(new SubmitLeaveRequestCommand(documentId, requesterId,
                startDate, endDate, submittedAt));
    }
}
