package com.academy.mudogroupware.approval.application.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.academy.mudogroupware.approval.application.command.SummarizeApprovalAttachmentCommand;
import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizationException;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizerPort;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentSummaryView;
import com.academy.mudogroupware.approval.application.usecase.SummarizeApprovalAttachmentUseCase;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalAttachment;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SummarizeApprovalAttachmentService implements SummarizeApprovalAttachmentUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final PlatformTransactionManager transactionManager;
    private final AttachmentContentPort attachmentContentPort;
    private final AttachmentSummarizerPort attachmentSummarizerPort;
    private final Clock clock;

    @Override
    public ApprovalAttachmentSummaryView summarize(SummarizeApprovalAttachmentCommand command) {
        Long fileId = findAuthorizedAttachmentFileId(command);
        LocalDateTime now = LocalDateTime.now(clock);

        AttachmentContent attachmentContent;
        try {
            attachmentContent = attachmentContentPort.loadContent(fileId);
        } catch (AttachmentContentUnavailableException e) {
            markSummaryFailed(command.documentId(), fileId, now);
            throw new ApprovalException(ApprovalErrorCode.ATTACHMENT_CONTENT_UNAVAILABLE, e);
        }

        String summary;
        try {
            summary = attachmentSummarizerPort.summarize(attachmentContent);
        } catch (AttachmentSummarizationException e) {
            markSummaryFailed(command.documentId(), fileId, now);
            throw new ApprovalException(ApprovalErrorCode.SUMMARY_GENERATION_FAILED, e);
        }

        return applySummary(command.documentId(), fileId, summary, now);
    }

    private Long findAuthorizedAttachmentFileId(SummarizeApprovalAttachmentCommand command) {
        return readOnlyTransaction().execute(status -> {
            ApprovalDocument approvalDocument = findDocument(command.documentId());

            if (!approvalDocument.isApprover(command.requesterId())
                    && !approvalDocument.getCreatorId().equals(command.requesterId())) {
                throw new ApprovalException(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
            }

            return findAttachment(approvalDocument, command.fileId()).getFileId();
        });
    }

    private void markSummaryFailed(Long documentId, Long fileId, LocalDateTime now) {
        writeTransaction().executeWithoutResult(status -> {
            ApprovalDocument approvalDocument = findDocument(documentId);
            findAttachment(approvalDocument, fileId).markSummaryFailed(now);
            approvalDocumentRepository.save(approvalDocument);
        });
    }

    private ApprovalAttachmentSummaryView applySummary(Long documentId, Long fileId, String summary,
                                                       LocalDateTime summarizedAt) {
        return writeTransaction().execute(status -> {
            ApprovalDocument approvalDocument = findDocument(documentId);
            ApprovalAttachment attachment = findAttachment(approvalDocument, fileId);
            attachment.applySummary(summary, summarizedAt);
            approvalDocumentRepository.save(approvalDocument);

            return new ApprovalAttachmentSummaryView(attachment.getFileId(), attachment.getAiSummary(),
                    attachment.getSummaryStatus(), attachment.getSummarizedAt());
        });
    }

    private ApprovalDocument findDocument(Long documentId) {
        return approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));
    }

    private ApprovalAttachment findAttachment(ApprovalDocument approvalDocument, Long fileId) {
        return approvalDocument.findAttachmentByFileId(fileId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.ATTACHMENT_NOT_FOUND));
    }

    private TransactionTemplate readOnlyTransaction() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        return transactionTemplate;
    }

    private TransactionTemplate writeTransaction() {
        return new TransactionTemplate(transactionManager);
    }
}
