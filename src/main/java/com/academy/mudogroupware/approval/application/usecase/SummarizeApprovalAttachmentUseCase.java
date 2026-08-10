package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.SummarizeApprovalAttachmentCommand;
import com.academy.mudogroupware.approval.application.query.ApprovalAttachmentSummaryView;

public interface SummarizeApprovalAttachmentUseCase {

    ApprovalAttachmentSummaryView summarize(SummarizeApprovalAttachmentCommand command);
}
