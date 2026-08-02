package com.academy.mudogroupware.approval.presentation.api.request;

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
        String fileUrl,
        List<Long> approverIds
) {

    public CreateApprovalDocumentCommand toCommand(Long creatorId) {
        return new CreateApprovalDocumentCommand(templateId, title, contentType, text, fileUrl, creatorId, approverIds);
    }
}
