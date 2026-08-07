package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.command.SummarizeApprovalAttachmentCommand;
import com.academy.mudogroupware.approval.application.port.AttachmentContentPort;
import com.academy.mudogroupware.approval.application.port.AttachmentContentUnavailableException;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizationException;
import com.academy.mudogroupware.approval.application.port.AttachmentSummarizerPort;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentSummaryView;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.AttachmentSummaryStatus;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

class SummarizeApprovalAttachmentServiceTest {

    private static final Long CREATOR_ID = 7L;
    private static final Long APPROVER_ID = 12L;
    private static final Long FILE_ID = 101L;
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 15, 0);

    private final ApprovalDocumentRepository approvalDocumentRepository = mock(ApprovalDocumentRepository.class);
    private final AttachmentContentPort attachmentContentPort = mock(AttachmentContentPort.class);
    private final AttachmentSummarizerPort attachmentSummarizerPort = mock(AttachmentSummarizerPort.class);
    private final Clock clock = Clock.fixed(
            NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private SummarizeApprovalAttachmentService service;

    @BeforeEach
    void setUp() {
        service = new SummarizeApprovalAttachmentService(approvalDocumentRepository, attachmentContentPort,
                attachmentSummarizerPort, clock);
    }

    private ApprovalDocument newDocument() {
        return ApprovalDocument.create(1L, 1L, "제목", ApprovalContent.create(ApprovalContentType.TEXT, "내용"),
                CREATOR_ID, List.of(APPROVER_ID), List.of(FILE_ID), CREATED_AT);
    }

    @Test
    void throwsWhenDocumentNotFound() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.summarize(new SummarizeApprovalAttachmentCommand(1L, FILE_ID, CREATOR_ID)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.DOCUMENT_NOT_FOUND);
    }

    @Test
    void throwsWhenRequesterIsNeitherCreatorNorApprover() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(newDocument()));

        assertThatThrownBy(() -> service.summarize(new SummarizeApprovalAttachmentCommand(1L, FILE_ID, 999L)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);

        verifyNoInteractions(attachmentSummarizerPort);
    }

    @Test
    void throwsWhenAttachmentNotFound() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(newDocument()));

        assertThatThrownBy(() -> service.summarize(new SummarizeApprovalAttachmentCommand(1L, 999L, CREATOR_ID)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.ATTACHMENT_NOT_FOUND);
    }

    @Test
    void appliesSummaryAndSavesDocumentOnSuccess() {
        ApprovalDocument document = newDocument();
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(attachmentContentPort.loadContent(FILE_ID)).thenReturn("실제 첨부파일 본문");
        when(attachmentSummarizerPort.summarize(anyString())).thenReturn("생성된 요약");
        when(approvalDocumentRepository.save(document)).thenReturn(document);

        ApprovalAttachmentSummaryView view = service.summarize(
                new SummarizeApprovalAttachmentCommand(1L, FILE_ID, APPROVER_ID));

        assertThat(view.fileId()).isEqualTo(FILE_ID);
        assertThat(view.aiSummary()).isEqualTo("생성된 요약");
        assertThat(view.summaryStatus()).isEqualTo(AttachmentSummaryStatus.COMPLETED);
        assertThat(view.summarizedAt()).isEqualTo(NOW);
        verify(attachmentSummarizerPort).summarize("실제 첨부파일 본문");
        verify(approvalDocumentRepository).save(document);
    }

    @Test
    void doesNotSendPlaceholderToSummarizerWhenAttachmentContentIsUnavailable() {
        ApprovalDocument document = newDocument();
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(attachmentContentPort.loadContent(FILE_ID))
                .thenThrow(new AttachmentContentUnavailableException(FILE_ID));

        assertThatThrownBy(() -> service.summarize(new SummarizeApprovalAttachmentCommand(1L, FILE_ID, CREATOR_ID)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.ATTACHMENT_CONTENT_UNAVAILABLE);

        verifyNoInteractions(attachmentSummarizerPort);
        verify(approvalDocumentRepository).save(document);
        assertThat(document.findAttachmentByFileId(FILE_ID)).get()
                .satisfies(attachment -> {
                    assertThat(attachment.getSummaryStatus()).isEqualTo(AttachmentSummaryStatus.FAILED);
                    assertThat(attachment.getSummarizedAt()).isEqualTo(NOW);
                });
    }

    @Test
    void marksFailedAndStillSavesWhenSummarizerFails() {
        ApprovalDocument document = newDocument();
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(attachmentContentPort.loadContent(FILE_ID)).thenReturn("실제 첨부파일 본문");
        when(attachmentSummarizerPort.summarize(anyString()))
                .thenThrow(new AttachmentSummarizationException("Gemini API 호출에 실패했습니다."));

        assertThatThrownBy(() -> service.summarize(new SummarizeApprovalAttachmentCommand(1L, FILE_ID, CREATOR_ID)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.SUMMARY_GENERATION_FAILED);

        verify(approvalDocumentRepository).save(document);
        assertThat(document.findAttachmentByFileId(FILE_ID)).get()
                .satisfies(attachment -> {
                    assertThat(attachment.getSummaryStatus()).isEqualTo(AttachmentSummaryStatus.FAILED);
                    assertThat(attachment.getSummarizedAt()).isEqualTo(NOW);
                });
    }
}
