package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.HideApprovalHistoryCommand;
import com.academy.mudogroupware.approval.application.usecase.HideApprovalHistoryUseCase;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalHistoryHiddenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class HideApprovalHistoryService implements HideApprovalHistoryUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final ApprovalHistoryHiddenRepository approvalHistoryHiddenRepository;
    private final Clock clock;

    @Override
    public void hide(HideApprovalHistoryCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        if (!approvalDocument.isApprover(command.requesterId())) {
            throw new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
        }
        if (!approvalDocument.hasDecidedLine(command.requesterId())) {
            throw new ApprovalException(ApprovalErrorCode.HISTORY_HIDE_NOT_ALLOWED);
        }
        if (approvalHistoryHiddenRepository.exists(command.documentId(), command.requesterId())) {
            return;
        }

        approvalHistoryHiddenRepository.save(command.documentId(), command.requesterId(), LocalDateTime.now(clock));
    }
}
