package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;

public record ApprovalTemplateSummaryView(
        Long id,
        String name,
        LocalDateTime createdAt
) {
}
