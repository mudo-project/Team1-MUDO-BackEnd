package com.academy.mudogroupware.approval.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.domain.exception.ApprovalErrorCode;
import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

class ApprovalDocumentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 9, 0);

    @Test
    void generalDocumentKeepsRetentionForThreeYears() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, ApprovalDocumentSourceType.GENERAL, "Vacation",
                ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                7L, List.of(12L), List.of(), NOW);

        assertThat(document.getRetentionPolicy()).isEqualTo(ApprovalRetentionPolicy.GENERAL_BUSINESS);
        assertThat(document.getRetentionUntil()).isEqualTo(NOW.plusYears(3));
        assertThat(document.isLegalHold()).isFalse();
        assertThat(document.getArchivedAt()).isNull();
    }

    @Test
    void corporateCardExpenseKeepsRetentionForFiveYears() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, ApprovalDocumentSourceType.CORPORATE_CARD_EXPENSE, "Card expense",
                ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                7L, List.of(12L), List.of(), NOW);

        assertThat(document.getRetentionPolicy()).isEqualTo(ApprovalRetentionPolicy.TAX_EVIDENCE);
        assertThat(document.getRetentionUntil()).isEqualTo(NOW.plusYears(5));
    }

    @Test
    void findAttachmentByFileIdReturnsMatchingAttachment() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, "제목", ApprovalContent.create(ApprovalContentType.TEXT, "내용"),
                7L, List.of(12L), List.of(101L, 102L), NOW);

        assertThat(document.findAttachmentByFileId(102L))
                .isPresent()
                .get()
                .satisfies(attachment -> assertThat(attachment.getFileId()).isEqualTo(102L));
    }

    @Test
    void findAttachmentByFileIdReturnsEmptyWhenNoAttachmentMatches() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, "제목", ApprovalContent.create(ApprovalContentType.TEXT, "내용"),
                7L, List.of(12L), List.of(101L), NOW);

        assertThat(document.findAttachmentByFileId(999L)).isEmpty();
    }
    @Test
    void cancelChangesStatusWhenNoApprovalLineWasDecided() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, "Vacation", ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                7L, List.of(12L, 13L), List.of(), NOW);

        document.cancel();

        assertThat(document.getStatus()).isEqualTo(ApprovalStatus.CANCELLED);
    }

    @Test
    void cancelRejectsDocumentAfterAnyApprovalLineWasDecided() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, "Vacation", ApprovalContent.create(ApprovalContentType.TEXT, "content"),
                7L, List.of(12L, 13L), List.of(), NOW);
        document.decide(12L, ApprovalDecision.APPROVE, null, NOW.plusHours(1));

        assertThatThrownBy(document::cancel)
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.CANCEL_NOT_ALLOWED);
    }
}
