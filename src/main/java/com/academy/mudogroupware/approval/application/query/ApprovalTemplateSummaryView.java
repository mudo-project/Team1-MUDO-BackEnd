package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record ApprovalTemplateSummaryView(
        Long id,
        String name,
        LocalDateTime createdAt,
        List<ApprovalTemplateLineView> lines
) {
}
