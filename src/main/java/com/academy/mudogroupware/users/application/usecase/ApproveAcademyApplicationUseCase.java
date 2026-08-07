package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.ApproveAcademyApplicationCommand;
import com.academy.mudogroupware.users.application.result.ApproveAcademyApplicationResult;

public interface ApproveAcademyApplicationUseCase {

    ApproveAcademyApplicationResult approve(ApproveAcademyApplicationCommand command);
}
