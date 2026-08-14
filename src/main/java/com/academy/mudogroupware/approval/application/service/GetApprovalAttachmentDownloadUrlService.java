package com.academy.mudogroupware.approval.application.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.academy.mudogroupware.approval.application.command.GetApprovalAttachmentDownloadUrlCommand;
import com.academy.mudogroupware.approval.application.usecase.GetApprovalAttachmentDownloadUrlUseCase;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;
import com.academy.mudogroupware.global.domain.common.exception.ApplicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

        try {
            return getFileDownloadUrlUseCase.getDownloadUrl(command.fileId());
        } catch (ApplicationException exception) {
            if (!exception.getErrorCode().getHttpStatus().is5xxServerError()) {
                throw exception;
            }
            throw attachmentDownloadUrlFailed(command, exception);
        } catch (RuntimeException exception) {
            throw attachmentDownloadUrlFailed(command, exception);
        }
    }

    private ApprovalException attachmentDownloadUrlFailed(GetApprovalAttachmentDownloadUrlCommand command,
                                                          RuntimeException cause) {
        log.warn("event=approval_attachment_download_url_failed documentId={} fileId={} requesterId={} "
                        + "exceptionClass={} message={}",
                command.documentId(), command.fileId(), command.requesterId(), cause.getClass().getSimpleName(),
                cause.getMessage(), cause);
        return new ApprovalException(ApprovalErrorCode.ATTACHMENT_DOWNLOAD_URL_FAILED, cause, Map.of(
                "documentId", command.documentId(),
                "fileId", command.fileId(),
                "requesterId", command.requesterId()));
    }
}
