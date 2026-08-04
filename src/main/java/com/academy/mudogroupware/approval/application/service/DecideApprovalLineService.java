package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.DecideApprovalLineCommand;
import com.academy.mudogroupware.approval.application.usecase.DecideApprovalLineUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DecideApprovalLineService implements DecideApprovalLineUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final Clock clock;

    @Override
    public void decide(DecideApprovalLineCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        approvalDocument.decide(command.approverId(), command.decision(), command.comment(), LocalDateTime.now(clock));

        approvalDocumentRepository.save(approvalDocument);
    }
}
