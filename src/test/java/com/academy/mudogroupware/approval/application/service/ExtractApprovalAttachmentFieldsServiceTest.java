package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.academy.mudogroupware.approval.application.port.AttachmentContent;
import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;
import com.academy.mudogroupware.approval.application.port.AttachmentFieldExtractionException;
import com.academy.mudogroupware.approval.application.port.AttachmentFieldExtractorPort;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentFieldsView;
import com.academy.mudogroupware.approval.application.port.ExtractedReceiptFields;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

class ExtractApprovalAttachmentFieldsServiceTest {

    private static final Long CREATOR_ID = 7L;
    private static final Long APPROVER_ID = 12L;
    private static final Long FILE_ID = 101L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);

    private final ApprovalDocumentRepository approvalDocumentRepository = mock(ApprovalDocumentRepository.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final AttachmentContentPort attachmentContentPort = mock(AttachmentContentPort.class);
    private final AttachmentFieldExtractorPort attachmentFieldExtractorPort = mock(AttachmentFieldExtractorPort.class);

    private final ExtractApprovalAttachmentFieldsService service = new ExtractApprovalAttachmentFieldsService(
            approvalDocumentRepository, transactionManager, attachmentContentPort, attachmentFieldExtractorPort);

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    private ApprovalDocument newDocument() {
        return ApprovalDocument.create(1L, "제목", ApprovalContent.create(ApprovalContentType.TEXT, "내용"),
                CREATOR_ID, List.of(APPROVER_ID), List.of(FILE_ID), CREATED_AT);
    }

    @Test
    void throwsWhenDocumentNotFound() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.extractFields(1L))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.DOCUMENT_NOT_FOUND);
    }

    @Test
    void throwsWhenDocumentHasNoAttachment() {
        ApprovalDocument document = ApprovalDocument.create(1L, "제목",
                ApprovalContent.create(ApprovalContentType.TEXT, "내용"), CREATOR_ID, List.of(APPROVER_ID),
                List.of(), CREATED_AT);
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> service.extractFields(1L))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.ATTACHMENT_NOT_FOUND);
    }

    @Test
    void throwsWhenAttachmentContentIsUnavailable() {
        ApprovalDocument document = newDocument();
        AttachmentContentUnavailableException cause = new AttachmentContentUnavailableException(FILE_ID);
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(attachmentContentPort.loadContent(FILE_ID)).thenThrow(cause);

        assertThatThrownBy(() -> service.extractFields(1L))
                .isInstanceOf(ApprovalException.class)
                .hasCause(cause)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.ATTACHMENT_CONTENT_UNAVAILABLE);
    }

    @Test
    void throwsWhenExtractionFails() {
        ApprovalDocument document = newDocument();
        AttachmentContent content = AttachmentContent.text("영수증 원문");
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(attachmentContentPort.loadContent(FILE_ID)).thenReturn(content);
        when(attachmentFieldExtractorPort.extract(content))
                .thenThrow(new AttachmentFieldExtractionException("Gemini API 호출에 실패했습니다."));

        assertThatThrownBy(() -> service.extractFields(1L))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.FIELD_EXTRACTION_FAILED);
    }

    @Test
    void returnsExtractedFieldsOnSuccess() {
        ApprovalDocument document = newDocument();
        AttachmentContent content = AttachmentContent.text("영수증 원문");
        ExtractedReceiptFields extracted = new ExtractedReceiptFields(45000L, LocalDate.of(2026, 8, 5), "스타벅스");
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(attachmentContentPort.loadContent(FILE_ID)).thenReturn(content);
        when(attachmentFieldExtractorPort.extract(content)).thenReturn(extracted);

        ApprovalAttachmentFieldsView view = service.extractFields(1L);

        assertThat(view.fileId()).isEqualTo(FILE_ID);
        assertThat(view.amount()).isEqualTo(45000L);
        assertThat(view.date()).isEqualTo(LocalDate.of(2026, 8, 5));
        assertThat(view.merchant()).isEqualTo("스타벅스");
    }
}
