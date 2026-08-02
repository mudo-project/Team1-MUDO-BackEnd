package com.academy.mudogroupware.approval.presentation.api.response;

public record ApprovalTemplateCreateResponse(
        Long templateId
) {

    public static ApprovalTemplateCreateResponse from(Long templateId) {
        return new ApprovalTemplateCreateResponse(templateId);
    }
}
