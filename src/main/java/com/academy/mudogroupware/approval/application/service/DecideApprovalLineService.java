package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.DecideApprovalLineCommand;
import com.academy.mudogroupware.approval.application.usecase.DecideApprovalLineUseCase;
import com.academy.mudogroupware.approval.domain.event.ApprovalLineActivatedEvent;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalDecision;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DecideApprovalLineService implements DecideApprovalLineUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void decide(DecideApprovalLineCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now(clock);
        approvalDocument.decide(command.approverId(), command.decision(), command.comment(), now);

        approvalDocumentRepository.save(approvalDocument);

        if (command.decision() == ApprovalDecision.APPROVE
                && approvalDocument.getStatus() == ApprovalStatus.IN_PROGRESS) {
            approvalDocument.currentPendingApproverId().ifPresent(nextApproverId -> eventPublisher.publishEvent(
                    new ApprovalLineActivatedEvent(approvalDocument.getId(), approvalDocument.getTitle(),
                            nextApproverId, now)));
        }
    }
}
