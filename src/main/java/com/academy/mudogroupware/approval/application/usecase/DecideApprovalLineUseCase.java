package com.academy.mudogroupware.approval.application.usecase;

import com.academy.mudogroupware.approval.application.command.DecideApprovalLineCommand;

public interface DecideApprovalLineUseCase {

    void decide(DecideApprovalLineCommand command);
}
