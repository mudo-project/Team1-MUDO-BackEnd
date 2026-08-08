package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.CancelApprovalDocumentCommand;

public interface CancelApprovalDocumentUseCase {

    void cancel(CancelApprovalDocumentCommand command);
}
