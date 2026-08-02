package com.academy.mudogroupware.approval.application.query;

public record ApprovalTemplateLineView(
        int stepOrder,
        Long approverId,
        String approverName
) {
}
