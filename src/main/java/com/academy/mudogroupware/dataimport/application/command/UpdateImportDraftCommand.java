package com.academy.mudogroupware.dataimport.application.command;

import com.academy.mudogroupware.dataimport.domain.model.ImportDraft;

public record UpdateImportDraftCommand(
        Long requesterId,
        Long importId,
        ImportDraft draft
) {
}
