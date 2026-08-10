package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentView;
import com.academy.mudogroupware.approval.domain.model.AttachmentSummaryStatus;

public record ApprovalAttachmentResponse(
        Long fileId,
        String aiSummary,
        AttachmentSummaryStatus summaryStatus,
        LocalDateTime summarizedAt
) {

    public static ApprovalAttachmentResponse from(ApprovalAttachmentView view) {
        return new ApprovalAttachmentResponse(view.fileId(), view.aiSummary(), view.summaryStatus(), view.summarizedAt());
    }
}
