package com.academy.mudogroupware.dataimport.application.usecase;

import com.academy.mudogroupware.dataimport.application.command.UpdateImportDraftCommand;

public interface UpdateImportDraftUseCase {

    void updateDraft(UpdateImportDraftCommand command);
}
