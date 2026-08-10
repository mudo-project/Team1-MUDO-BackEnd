package com.academy.mudogroupware.dataimport.application.port;

import java.util.List;

public record ParsedImportSheet(
        ImportFileRole role,
        String fileName,
        List<ParsedImportRow> rows
) {

    public ParsedImportSheet {
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        rows = rows != null ? List.copyOf(rows) : List.of();
    }
}
