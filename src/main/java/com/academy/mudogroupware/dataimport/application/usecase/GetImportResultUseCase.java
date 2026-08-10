package com.academy.mudogroupware.dataimport.application.usecase;

import com.academy.mudogroupware.dataimport.domain.model.ImportResult;

public interface GetImportResultUseCase {

    ImportResult getResult(Long academyId, Long importId);
}
