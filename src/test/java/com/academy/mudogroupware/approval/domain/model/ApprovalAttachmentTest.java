package com.academy.mudogroupware.approval.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ApprovalAttachmentTest {

    @Test
    void createStartsWithPendingStatusAndNoSummary() {
        ApprovalAttachment attachment = ApprovalAttachment.create(101L);

        assertThat(attachment.getFileId()).isEqualTo(101L);
        assertThat(attachment.getSummaryStatus()).isEqualTo(AttachmentSummaryStatus.PENDING);
        assertThat(attachment.getAiSummary()).isNull();
        assertThat(attachment.getSummarizedAt()).isNull();
    }

    @Test
    void createRejectsNullFileId() {
        assertThatThrownBy(() -> ApprovalAttachment.create(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applySummaryMarksCompletedAndStoresSummary() {
        ApprovalAttachment attachment = ApprovalAttachment.create(101L);
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 12, 0);

        attachment.applySummary("요약 결과", now);

        assertThat(attachment.getAiSummary()).isEqualTo("요약 결과");
        assertThat(attachment.getSummaryStatus()).isEqualTo(AttachmentSummaryStatus.COMPLETED);
        assertThat(attachment.getSummarizedAt()).isEqualTo(now);
    }

    @Test
    void applySummaryRejectsBlankSummary() {
        ApprovalAttachment attachment = ApprovalAttachment.create(101L);
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 12, 0);

        assertThatThrownBy(() -> attachment.applySummary(" ", now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applySummaryRejectsNullSummarizedAt() {
        ApprovalAttachment attachment = ApprovalAttachment.create(101L);

        assertThatThrownBy(() -> attachment.applySummary("요약", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markSummaryFailedSetsFailedStatusWithoutTouchingSummaryText() {
        ApprovalAttachment attachment = ApprovalAttachment.create(101L);
        LocalDateTime now = LocalDateTime.of(2026, 8, 4, 12, 0);

        attachment.markSummaryFailed(now);

        assertThat(attachment.getSummaryStatus()).isEqualTo(AttachmentSummaryStatus.FAILED);
        assertThat(attachment.getSummarizedAt()).isEqualTo(now);
        assertThat(attachment.getAiSummary()).isNull();
    }

    @Test
    void markSummaryFailedRejectsNullSummarizedAt() {
        ApprovalAttachment attachment = ApprovalAttachment.create(101L);

        assertThatThrownBy(() -> attachment.markSummaryFailed(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
