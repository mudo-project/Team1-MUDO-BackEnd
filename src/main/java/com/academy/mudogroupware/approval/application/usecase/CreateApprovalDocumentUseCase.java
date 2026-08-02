package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.CreateApprovalDocumentCommand;

public interface CreateApprovalDocumentUseCase {

    Long createDocument(CreateApprovalDocumentCommand command);
}
