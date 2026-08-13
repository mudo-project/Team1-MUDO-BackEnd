package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import com.academy.mudogroupware.approval.application.command.DecideApprovalLineCommand;
import com.academy.mudogroupware.approval.domain.event.ApprovalDocumentDecidedEvent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDecision;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.model.ApprovalStatus;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;

class DecideApprovalLineServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 15, 0);

    private final ApprovalDocumentRepository approvalDocumentRepository = mock(ApprovalDocumentRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private final DecideApprovalLineService service =
            new DecideApprovalLineService(approvalDocumentRepository, eventPublisher, clock);

    @Test
    void approveFinalLinePublishesApprovedDecisionStatus() {
        ApprovalDocument document = document(List.of(12L));
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalDocumentRepository.save(document)).thenReturn(document);

        service.decide(new DecideApprovalLineCommand(1L, 12L, ApprovalDecision.APPROVE, "확인했습니다"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(ApprovalDocumentDecidedEvent.class, event -> {
                    assertThat(event.documentId()).isEqualTo(1L);
                    assertThat(event.requesterId()).isEqualTo(7L);
                    assertThat(event.status()).isEqualTo(ApprovalStatus.APPROVED);
                    assertThat(event.approved()).isTrue();
                    assertThat(event.decidedAt()).isEqualTo(NOW);
                });
    }

    @Test
    void rejectLinePublishesRejectedDecisionStatus() {
        ApprovalDocument document = document(List.of(12L, 13L));
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalDocumentRepository.save(document)).thenReturn(document);

        service.decide(new DecideApprovalLineCommand(1L, 12L, ApprovalDecision.REJECT, "반려합니다"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue())
                .isInstanceOfSatisfying(ApprovalDocumentDecidedEvent.class, event -> {
                    assertThat(event.documentId()).isEqualTo(1L);
                    assertThat(event.requesterId()).isEqualTo(7L);
                    assertThat(event.status()).isEqualTo(ApprovalStatus.REJECTED);
                    assertThat(event.approved()).isFalse();
                    assertThat(event.decidedAt()).isEqualTo(NOW);
                });
    }

    private ApprovalDocument document(List<Long> approverIds) {
        return ApprovalDocument.restore(
                1L,
                1L,
                "Vacation",
                ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                7L,
                ApprovalDocument.create(1L, "Vacation", ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                        7L, approverIds, List.of(), CREATED_AT).getLines(),
                List.of(),
                ApprovalStatus.IN_PROGRESS,
                CREATED_AT,
                null);
    }
}
