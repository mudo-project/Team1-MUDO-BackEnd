package com.academy.mudogroupware.approval.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class ApprovalDocumentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 9, 0);

    @Test
    void findAttachmentByFileIdReturnsMatchingAttachment() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, 1L, "제목", ApprovalContent.create(ApprovalContentType.TEXT, "내용"),
                7L, List.of(12L), List.of(101L, 102L), NOW);

        assertThat(document.findAttachmentByFileId(102L))
                .isPresent()
                .get()
                .satisfies(attachment -> assertThat(attachment.getFileId()).isEqualTo(102L));
    }

    @Test
    void findAttachmentByFileIdReturnsEmptyWhenNoAttachmentMatches() {
        ApprovalDocument document = ApprovalDocument.create(
                1L, 1L, "제목", ApprovalContent.create(ApprovalContentType.TEXT, "내용"),
                7L, List.of(12L), List.of(101L), NOW);

        assertThat(document.findAttachmentByFileId(999L)).isEmpty();
    }
}
