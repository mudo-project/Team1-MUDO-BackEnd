package com.academy.mudogroupware.approval.application.query;

import java.time.LocalDateTime;

import com.academy.mudogroupware.approval.domain.model.AttachmentSummaryStatus;

public record ApprovalAttachmentSummaryView(
        Long fileId,
        String aiSummary,
        AttachmentSummaryStatus summaryStatus,
        LocalDateTime summarizedAt
) {
}
