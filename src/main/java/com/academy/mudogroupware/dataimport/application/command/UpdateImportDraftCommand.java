package com.academy.mudogroupware.dataimport.application.command;

import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;

public record UpdateImportDraftCommand(
        Long academyId,
        Long importId,
        ImportDraft draft
) {
}
