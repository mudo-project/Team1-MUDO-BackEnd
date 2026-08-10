package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.command.GetApprovalAttachmentDownloadUrlCommand;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.file.application.usecase.GetFileDownloadUrlUseCase;

class GetApprovalAttachmentDownloadUrlServiceTest {

    private static final Long ACADEMY_ID = 1L;
    private static final Long CREATOR_ID = 7L;
    private static final Long APPROVER_ID = 12L;
    private static final Long FILE_ID = 101L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);

    private final ApprovalDocumentRepository approvalDocumentRepository = mock(ApprovalDocumentRepository.class);
    private final GetFileDownloadUrlUseCase getFileDownloadUrlUseCase = mock(GetFileDownloadUrlUseCase.class);

    private GetApprovalAttachmentDownloadUrlService service;

    @BeforeEach
    void setUp() {
        service = new GetApprovalAttachmentDownloadUrlService(approvalDocumentRepository, getFileDownloadUrlUseCase);
    }

    private ApprovalDocument newDocument() {
        return ApprovalDocument.create(1L, "제목", ApprovalContent.create(ApprovalContentType.TEXT, "내용"),
                CREATOR_ID, List.of(APPROVER_ID), List.of(FILE_ID), CREATED_AT);
    }

    @Test
    void throwsWhenDocumentNotFound() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDownloadUrl(
                new GetApprovalAttachmentDownloadUrlCommand(1L, FILE_ID, CREATOR_ID, ACADEMY_ID)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.DOCUMENT_NOT_FOUND);

        verifyNoInteractions(getFileDownloadUrlUseCase);
    }

    @Test
    void throwsWhenRequesterIsNeitherCreatorNorApprover() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(newDocument()));

        assertThatThrownBy(() -> service.getDownloadUrl(
                new GetApprovalAttachmentDownloadUrlCommand(1L, FILE_ID, 999L, ACADEMY_ID)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);

        verifyNoInteractions(getFileDownloadUrlUseCase);
    }

    @Test
    void throwsWhenFileIdDoesNotBelongToDocument() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(newDocument()));

        assertThatThrownBy(() -> service.getDownloadUrl(
                new GetApprovalAttachmentDownloadUrlCommand(1L, 999L, CREATOR_ID, ACADEMY_ID)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.ATTACHMENT_NOT_FOUND);

        verifyNoInteractions(getFileDownloadUrlUseCase);
    }

    @Test
    void returnsDownloadUrlWhenRequesterIsCreator() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(newDocument()));
        when(getFileDownloadUrlUseCase.getDownloadUrl(FILE_ID, ACADEMY_ID)).thenReturn("https://example.com/signed");

        String downloadUrl = service.getDownloadUrl(
                new GetApprovalAttachmentDownloadUrlCommand(1L, FILE_ID, CREATOR_ID, ACADEMY_ID));

        assertThat(downloadUrl).isEqualTo("https://example.com/signed");
    }

    @Test
    void returnsDownloadUrlWhenRequesterIsApprover() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(newDocument()));
        when(getFileDownloadUrlUseCase.getDownloadUrl(FILE_ID, ACADEMY_ID)).thenReturn("https://example.com/signed");

        String downloadUrl = service.getDownloadUrl(
                new GetApprovalAttachmentDownloadUrlCommand(1L, FILE_ID, APPROVER_ID, ACADEMY_ID));

        assertThat(downloadUrl).isEqualTo("https://example.com/signed");
    }
}
