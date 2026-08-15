package com.academy.mudogroupware.approval.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.approval.domain.exception.ApprovalException;

class ApprovalTemplateTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 4, 9, 0);
    private static final String DUPLICATE_APPROVER_CODE = "APPROVAL_400_7";

    @Test
    void createRejectsDuplicateApprovalLine() {
        assertThatThrownBy(() -> ApprovalTemplate.create("Vacation", 7L, List.of(12L, 12L), NOW))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode().getCode())
                .isEqualTo(DUPLICATE_APPROVER_CODE);
    }

    @Test
    void updateRejectsDuplicateApprovalLine() {
        ApprovalTemplate template = ApprovalTemplate.create("Vacation", 7L, List.of(12L), NOW);

        assertThatThrownBy(() -> template.update("Vacation", List.of(12L, 12L), NOW.plusMinutes(1)))
                .isInstanceOf(ApprovalException.class)
                .extracting(e -> ((ApprovalException) e).getErrorCode().getCode())
                .isEqualTo(DUPLICATE_APPROVER_CODE);
    }
}
