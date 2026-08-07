package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.RejectAcademyApplicationCommand;

public interface RejectAcademyApplicationUseCase {

    void reject(RejectAcademyApplicationCommand command);
}
