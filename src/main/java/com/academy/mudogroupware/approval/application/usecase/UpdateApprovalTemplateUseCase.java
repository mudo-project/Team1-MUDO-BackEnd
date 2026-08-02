package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.UpdateApprovalTemplateCommand;

public interface UpdateApprovalTemplateUseCase {

    void updateTemplate(UpdateApprovalTemplateCommand command);
}
