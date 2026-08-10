package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.ResubmitApprovalDocumentCommand;

public interface ResubmitApprovalDocumentUseCase {

    Long resubmit(ResubmitApprovalDocumentCommand command);
}
