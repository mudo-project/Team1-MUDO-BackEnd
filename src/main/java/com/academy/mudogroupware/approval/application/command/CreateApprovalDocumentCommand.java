package com.academy.mudogroupware.approval.application.command;

import java.util.List;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;

public record CreateApprovalDocumentCommand(
        Long templateId,
        String title,
        ApprovalContentType contentType,
        String text,
        String fileUrl,
        Long creatorId,
        List<Long> approverIds
) {
}
