package com.academy.mudogroupware.approval.presentation.api.request;

import java.time.LocalDate;
import java.util.List;

import com.academy.mudogroupware.approval.application.command.CreateApprovalDocumentCommand;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateApprovalDocumentRequest(
        @NotNull Long templateId,
        @NotBlank String title,
        @NotNull ApprovalContentType contentType,
        String text,
        List<Long> fileIds,
        List<Long> approverIds,
        LocalDate leaveStartDate,
        LocalDate leaveEndDate
) {

    public CreateApprovalDocumentCommand toCommand(Long creatorId, Long academyId) {
        return new CreateApprovalDocumentCommand(templateId, title, contentType, text, fileIds, creatorId,
                approverIds, leaveStartDate, leaveEndDate, academyId);
    }
}
