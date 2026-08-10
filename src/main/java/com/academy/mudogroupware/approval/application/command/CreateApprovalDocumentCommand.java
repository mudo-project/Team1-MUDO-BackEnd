package com.academy.mudogroupware.approval.application.command;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;
import com.academy.mudogroupware.approval.domain.model.ApprovalDocumentSourceType;

public record CreateApprovalDocumentCommand(
        Long templateId,
        String title,
        ApprovalContentType contentType,
        String text,
        List<Long> fileIds,
        Long creatorId,
        List<Long> approverIds,
        LocalDate leaveStartDate,
        LocalDate leaveEndDate,
        ApprovalDocumentSourceType sourceType
) {
    public CreateApprovalDocumentCommand(Long templateId, String title, ApprovalContentType contentType,
                                         String text, List<Long> fileIds, Long creatorId,
                                         List<Long> approverIds, LocalDate leaveStartDate,
                                         LocalDate leaveEndDate) {
        this(templateId, title, contentType, text, fileIds, creatorId, approverIds,
                leaveStartDate, leaveEndDate, ApprovalDocumentSourceType.GENERAL);
    }
}
