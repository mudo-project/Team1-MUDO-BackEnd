package com.academy.mudogroupware.approval.presentation.api.response;

public record ApprovalCreateResponse(
        Long approvalTemplateId
) {

    public static ApprovalCreateResponse from(Long approvalTemplateId) {
        return new ApprovalCreateResponse(approvalTemplateId);
    }
}
