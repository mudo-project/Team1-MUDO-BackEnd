package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;
import com.academy.mudogroupware.approval.application.port.AttachmentFieldExtractionException;
import com.academy.mudogroupware.approval.application.port.AttachmentFieldExtractorPort;
import com.academy.mudogroupware.approval.application.port.ExtractedReceiptFields;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentFieldsView;
import com.academy.mudogroupware.approval.application.usecase.ExtractApprovalAttachmentFieldsUseCase;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalAttachment;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExtractApprovalAttachmentFieldsService implements ExtractApprovalAttachmentFieldsUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final PlatformTransactionManager transactionManager;
    private final AttachmentContentPort attachmentContentPort;
    private final AttachmentFieldExtractorPort attachmentFieldExtractorPort;

    @Override
    public ApprovalAttachmentFieldsView extractFields(Long documentId) {
        Long fileId = findFirstAttachmentFileId(documentId);

        AttachmentContent content;
        try {
            content = attachmentContentPort.loadContent(fileId);
        } catch (AttachmentContentUnavailableException e) {
            throw new ApprovalException(ApprovalErrorCode.ATTACHMENT_CONTENT_UNAVAILABLE, e);
        }

        try {
            ExtractedReceiptFields fields = attachmentFieldExtractorPort.extract(content);
            return new ApprovalAttachmentFieldsView(fileId, fields.amount(), fields.date(),
                    fields.merchant());
        } catch (AttachmentFieldExtractionException e) {
            throw new ApprovalException(ApprovalErrorCode.FIELD_EXTRACTION_FAILED, e);
        }
    }

    private Long findFirstAttachmentFileId(Long documentId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        return transactionTemplate.execute(status -> {
            ApprovalDocument approvalDocument = approvalDocumentRepository.findById(documentId)
                    .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

            ApprovalAttachment attachment = approvalDocument.getAttachments().stream().findFirst()
                    .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.ATTACHMENT_NOT_FOUND));
            return attachment.getFileId();
        });
    }
}
