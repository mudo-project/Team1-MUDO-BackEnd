package com.academy.mudogroupware.approval.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.approval.application.command.CreateApprovalTemplateCommand;
import com.academy.mudogroupware.approval.domain.model.ApprovalContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateApprovalTemplateRequest(
        @NotBlank String title,
        @NotNull ApprovalContentType contentType,
        String text,
        String fileUrl,
        @NotNull Long creatorId,
        @NotEmpty List<Long> approverIds
) {

    public CreateApprovalTemplateCommand toCommand() {
        return new CreateApprovalTemplateCommand(title, contentType, text, fileUrl, creatorId, approverIds);
    }
}
