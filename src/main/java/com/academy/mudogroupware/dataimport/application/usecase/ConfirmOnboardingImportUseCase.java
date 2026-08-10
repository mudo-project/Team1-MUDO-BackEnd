package com.academy.mudogroupware.dataimport.application.usecase;

import com.academy.mudogroupware.dataimport.domain.model.ImportResult;

public interface ConfirmOnboardingImportUseCase {

    ImportResult confirm(Long importId, Long requesterId);
}
