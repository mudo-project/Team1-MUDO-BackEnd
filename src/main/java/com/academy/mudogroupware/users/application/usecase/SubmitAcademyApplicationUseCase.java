package com.academy.mudogroupware.users.application.usecase;

import com.academy.mudogroupware.users.application.command.SubmitAcademyApplicationCommand;

public interface SubmitAcademyApplicationUseCase {

    Long submit(SubmitAcademyApplicationCommand command);
}
