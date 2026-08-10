package com.academy.mudogroupware.approval.presentation.api.response;

public record ApprovalAttachmentDownloadUrlResponse(
        String downloadUrl
) {

    public static ApprovalAttachmentDownloadUrlResponse from(String downloadUrl) {
        return new ApprovalAttachmentDownloadUrlResponse(downloadUrl);
    }
}
