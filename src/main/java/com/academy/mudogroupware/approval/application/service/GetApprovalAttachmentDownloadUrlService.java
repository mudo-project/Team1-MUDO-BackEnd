package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.GetApprovalAttachmentDownloadUrlCommand;
import com.academy.mudogroupware.approval.application.usecase.GetApprovalAttachmentDownloadUrlUseCase;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetApprovalAttachmentDownloadUrlService implements GetApprovalAttachmentDownloadUrlUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final GetFileDownloadUrlUseCase getFileDownloadUrlUseCase;

    @Override
    public String getDownloadUrl(GetApprovalAttachmentDownloadUrlCommand command) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(command.documentId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        if (!approvalDocument.isApprover(command.requesterId())
                && !approvalDocument.getCreatorId().equals(command.requesterId())) {
            throw new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        approvalDocument.findAttachmentByFileId(command.fileId())
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.ATTACHMENT_NOT_FOUND));

        return getFileDownloadUrlUseCase.getDownloadUrl(command.fileId(), command.academyId());
    }
}
