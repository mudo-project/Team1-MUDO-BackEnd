package com.academy.mudogroupware.dataimport.application.usecase;

import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;

public interface GetImportDraftUseCase {

    ImportDraft getDraft(Long academyId, Long importId);
}
