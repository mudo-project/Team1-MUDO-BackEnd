package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalDocumentLinesCommand;
import com.academy.mudogroupware.approval.application.usecase.UpdateApprovalDocumentLinesUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateApprovalDocumentLinesService implements UpdateApprovalDocumentLinesUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Override
    public void updateLines(UpdateApprovalDocumentLinesCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        if (!approvalDocument.getCreatorId().equals(command.requesterId())) {
            throw new ApprovalException(ApprovalErrorCode.NOT_DOCUMENT_OWNER_LINES);
        }

        approvalDocument.updateLines(command.approverIds());

        approvalDocumentRepository.save(approvalDocument);
    }
}
