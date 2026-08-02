package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalDocumentLinesCommand;

public interface UpdateApprovalDocumentLinesUseCase {

    void updateLines(UpdateApprovalDocumentLinesCommand command);
}
