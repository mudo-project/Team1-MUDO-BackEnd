package com.academy.mudogroupware.approval.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
public class ExtractApprovalAttachmentFieldsService implements ExtractApprovalAttachmentFieldsUseCase {

    private final ApprovalDocumentRepository approvalDocumentRepository;
    private final AttachmentContentPort attachmentContentPort;
    private final AttachmentFieldExtractorPort attachmentFieldExtractorPort;

    @Override
    public ApprovalAttachmentFieldsView extractFields(Long documentId) {
        ApprovalDocument approvalDocument = approvalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.DOCUMENT_NOT_FOUND));

        ApprovalAttachment attachment = approvalDocument.getAttachments().stream().findFirst()
                .orElseThrow(() -> new ApprovalException(ApprovalErrorCode.ATTACHMENT_NOT_FOUND));

        AttachmentContent content;
        try {
            content = attachmentContentPort.loadContent(attachment.getFileId());
        } catch (AttachmentContentUnavailableException e) {
            throw new ApprovalException(ApprovalErrorCode.ATTACHMENT_CONTENT_UNAVAILABLE);
        }

        try {
            ExtractedReceiptFields fields = attachmentFieldExtractorPort.extract(content);
            return new ApprovalAttachmentFieldsView(attachment.getFileId(), fields.amount(), fields.date(),
                    fields.merchant());
        } catch (AttachmentFieldExtractionException e) {
            throw new ApprovalException(ApprovalErrorCode.FIELD_EXTRACTION_FAILED);
        }
    }
}
