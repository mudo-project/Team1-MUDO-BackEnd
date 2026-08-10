package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.ResubmitApprovalDocumentCommand;
import com.academy.mudogroupware.approval.application.usecase.ResubmitApprovalDocumentUseCase;
import com.academy.mudogroupware.approval.domain.model.ApprovalAttachment;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentLine;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ResubmitApprovalDocumentService implements ResubmitApprovalDocumentUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final Clock clock;

    @Override
    public Long resubmit(ResubmitApprovalDocumentCommand command) {
        ApprovalDocument original = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        if (!original.getCreatorId().equals(command.requesterId())) {
            throw new ApprovalException(ApprovalErrorCode.NOT_DOCUMENT_OWNER_RESUBMIT);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        original.markResubmitted(now);

        List<Long> approverIds = original.getLines().stream()
                .map(ApprovalDocumentLine::getApproverId)
                .toList();
        List<Long> fileIds = original.getAttachments().stream()
                .map(ApprovalAttachment::getFileId)
                .toList();

        ApprovalDocument resubmitted = ApprovalDocument.create(
                original.getTemplateId(), original.getSourceType(), original.getTitle(), original.getContent(),
                original.getCreatorId(), approverIds, fileIds, now);

        Long newDocumentId = approvalDocumentRepository.save(resubmitted).getId();
        approvalDocumentRepository.save(original);
        return newDocumentId;
    }
}
