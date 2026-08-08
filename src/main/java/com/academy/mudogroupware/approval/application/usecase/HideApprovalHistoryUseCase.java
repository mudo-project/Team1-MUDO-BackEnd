package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.HideApprovalHistoryCommand;

public interface HideApprovalHistoryUseCase {

    void hide(HideApprovalHistoryCommand command);
}
