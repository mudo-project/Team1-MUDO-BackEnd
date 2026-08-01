package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.CreateApprovalTemplateCommand;

public interface CreateApprovalTemplateUseCase {

    Long createTemplate(CreateApprovalTemplateCommand command);
}
