package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.CancelApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.usecase.CancelApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.event.ApprovalDocumentDecidedEvent;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelApprovalDocumentService implements CancelApprovalDocumentUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    public void cancel(CancelApprovalDocumentCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        if (!approvalDocument.getCreatorId().equals(command.requesterId())) {
            throw new ApprovalException(ApprovalErrorCode.NOT_DOCUMENT_OWNER_CANCEL);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        approvalDocument.cancel();
        approvalDocumentRepository.save(approvalDocument);

        eventPublisher.publishEvent(new ApprovalDocumentDecidedEvent(approvalDocument.getId(),
                approvalDocument.getAcademyId(), approvalDocument.getCreatorId(), false, now));
    }
}
