package com.academy.mudogroupware.approval.presentation.api.response;

public record ApprovalCreateResponse(
        Long documentId
) {

    public static ApprovalCreateResponse from(Long documentId) {
        return new ApprovalCreateResponse(documentId);
    }
}
