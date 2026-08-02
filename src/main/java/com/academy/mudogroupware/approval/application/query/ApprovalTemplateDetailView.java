package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;
import java.util.List;

public record ApprovalTemplateDetailView(
        Long id,
        String name,
        Long creatorId,
        LocalDateTime createdAt,
        List<ApprovalTemplateLineView> lines
) {
}
