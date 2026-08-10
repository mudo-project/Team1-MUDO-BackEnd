package com.academy.mudogroupware.dataimport.application.usecase;

import com.academy.mudogroupware.dataimport.application.command.CreateOnboardingImportCommand;

public interface CreateOnboardingImportUseCase {

    Long create(CreateOnboardingImportCommand command);
}
