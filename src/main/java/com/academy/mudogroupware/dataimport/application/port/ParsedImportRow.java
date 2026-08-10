package com.academy.mudogroupware.dataimport.application.port;

import java.util.Map;

public record ParsedImportRow(
        int rowNumber,
        Map<String, String> values
) {

    public ParsedImportRow {
        values = values != null ? Map.copyOf(values) : Map.of();
    }

    public String value(String header) {
        return values.get(header);
    }
}
