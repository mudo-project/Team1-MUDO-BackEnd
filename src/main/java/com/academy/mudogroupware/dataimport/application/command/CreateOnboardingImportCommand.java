package com.academy.mudogroupware.dataimport.application.command;

import java.util.List;

import com.academy.mudogroupware.dataimport.application.port.ImportFile;

public record CreateOnboardingImportCommand(
        Long createdBy,
        List<ImportFile> files
) {

    public CreateOnboardingImportCommand {
        files = files != null ? List.copyOf(files) : List.of();
    }
}
