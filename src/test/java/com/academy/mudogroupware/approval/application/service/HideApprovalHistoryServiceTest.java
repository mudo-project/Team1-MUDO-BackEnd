package com.academy.mudogroupware.approval.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.application.command.HideApprovalHistoryCommand;
import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;
import com.academy.mudogroupware.approval.domain.model.ApprovalContent;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDecision;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocument;
import com.academy.mudogroupware.approval.domain.repository.ApprovalDocumentRepository;
import com.academy.mudogroupware.approval.domain.repository.ApprovalHistoryHiddenRepository;

class HideApprovalHistoryServiceTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 4, 9, 0);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 15, 0);

    private final ApprovalDocumentRepository approvalDocumentRepository = mock(ApprovalDocumentRepository.class);
    private final ApprovalHistoryHiddenRepository approvalHistoryHiddenRepository =
            mock(ApprovalHistoryHiddenRepository.class);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private final HideApprovalHistoryService service =
            new HideApprovalHistoryService(approvalDocumentRepository, approvalHistoryHiddenRepository, clock);

    @Test
    void hideProcessedHistorySavesPersonalHiddenRow() {
        ApprovalDocument document = document();
        document.decide(12L, ApprovalDecision.APPROVE, null, NOW);
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalHistoryHiddenRepository.exists(1L, 12L)).thenReturn(false);

        service.hide(new HideApprovalHistoryCommand(1L, 12L));

        verify(approvalHistoryHiddenRepository).save(1L, 12L, NOW);
    }

    @Test
    void hideProcessedHistoryIsIdempotentWhenAlreadyHidden() {
        ApprovalDocument document = document();
        document.decide(12L, ApprovalDecision.APPROVE, null, NOW);
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(approvalHistoryHiddenRepository.exists(1L, 12L)).thenReturn(true);

        service.hide(new HideApprovalHistoryCommand(1L, 12L));

        verify(approvalHistoryHiddenRepository, never()).save(1L, 12L, NOW);
    }

    @Test
    void hideProcessedHistoryRejectsApproverWhoHasNotDecidedLine() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document()));

        assertThatThrownBy(() -> service.hide(new HideApprovalHistoryCommand(1L, 12L)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.HISTORY_HIDE_NOT_ALLOWED);
    }

    @Test
    void hideProcessedHistoryRejectsUserOutsideApprovalLines() {
        when(approvalDocumentRepository.findById(1L)).thenReturn(Optional.of(document()));

        assertThatThrownBy(() -> service.hide(new HideApprovalHistoryCommand(1L, 99L)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.DOCUMENT_ACCESS_DENIED);
    }

    private ApprovalDocument document() {
        return ApprovalDocument.create(1L, "Vacation", ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                7L, List.of(12L), List.of(), CREATED_AT);
    }
}
