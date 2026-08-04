package com.academy.mudogroupware.approval.presentation.api.response;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentSummaryView;
import com.academy.mudogroupware.approval.domain.model.AttachmentSummaryStatus;

public record ApprovalAttachmentSummaryResponse(
        Long fileId,
        String aiSummary,
        AttachmentSummaryStatus summaryStatus,
        LocalDateTime summarizedAt
) {

    public static ApprovalAttachmentSummaryResponse from(ApprovalAttachmentSummaryView view) {
        return new ApprovalAttachmentSummaryResponse(view.fileId(), view.aiSummary(), view.summaryStatus(),
                view.summarizedAt());
    }
}
