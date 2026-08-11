package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;

import com.academy.mudogroupware.approval.application.command.CancelApprovalDocumentCommand;
import com.academy.mudogroupware.approval.domain.event.ApprovalDocumentDecidedEvent;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

class CancelApprovalDocumentServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 15, 0);

    private final ApprovalDocumentRepository approvalDocumentRepository = mock(ApprovalDocumentRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private final CancelApprovalDocumentService service =
            new CancelApprovalDocumentService(approvalDocumentRepository, eventPublisher, clock);

    @Test
    void cancelChangesDocumentStatusAndPublishesRejectedDecisionEvent() {
        ApprovalDocument document = document();
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalDocumentRepository.save(document)).thenReturn(document);

        service.cancel(new CancelApprovalDocumentCommand(1L, 7L));

        assertThat(document.getStatus()).isEqualTo(ApprovalStatus.CANCELLED);
        verify(approvalDocumentRepository).save(document);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(ApprovalDocumentDecidedEvent.class, event -> {
                    assertThat(event.documentId()).isEqualTo(1L);
                    assertThat(event.requesterId()).isEqualTo(7L);
                    assertThat(event.approved()).isFalse();
                    assertThat(event.decidedAt()).isEqualTo(NOW);
                });
    }

    @Test
    void cancelRejectsRequesterWhoIsNotDocumentCreator() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document()));

        assertThatThrownBy(() -> service.cancel(new CancelApprovalDocumentCommand(1L, 99L)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.NOT_DOCUMENT_OWNER_CANCEL);

        verifyNoInteractions(eventPublisher);
    }

    private ApprovalDocument document() {
        return ApprovalDocument.restore(
                1L,
                1L,
                "Vacation",
                ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                7L,
                ApprovalDocument.create(1L, "Vacation", ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                        7L, List.of(12L), List.of(), CREATED_AT).getLines(),
                List.of(),
                ApprovalStatus.IN_PROGRESS,
                CREATED_AT,
                null);
    }
}
