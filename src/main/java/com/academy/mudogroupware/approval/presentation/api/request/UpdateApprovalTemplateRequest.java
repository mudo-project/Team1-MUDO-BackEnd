package com.academy.mudogroupware.approval.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalTemplateCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UpdateApprovalTemplateRequest(
        @NotBlank String name,
        @NotEmpty List<Long> approverIds
) {

    public UpdateApprovalTemplateCommand toCommand(Long templateId) {
        return new UpdateApprovalTemplateCommand(templateId, name, approverIds);
    }
}
