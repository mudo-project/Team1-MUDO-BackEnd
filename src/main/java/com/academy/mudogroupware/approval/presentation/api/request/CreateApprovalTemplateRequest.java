package com.academy.mudogroupware.approval.presentation.api.request;

import java.util.List;

import com.academy.mudogroupware.approval.application.command.CreateApprovalTemplateCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateApprovalTemplateRequest(
        @NotBlank String name,
        @NotEmpty List<Long> approverIds
) {

    public CreateApprovalTemplateCommand toCommand(Long creatorId) {
        return new CreateApprovalTemplateCommand(name, creatorId, approverIds);
    }
}
