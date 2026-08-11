package com.academy.mudogroupware.dataimport.infrastructure.external.fastapi;

import java.util.List;
import java.util.Map;

import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;

record FastApiImportAnalysisRequest(List<Sheet> sheets) {

    static FastApiImportAnalysisRequest from(List<ParsedImportSheet> sheets) {
        return new FastApiImportAnalysisRequest(sheets.stream()
                .map(Sheet::from)
                .toList());
    }

    record Sheet(String role, String fileName, List<String> headers, List<Row> rows) {

        static Sheet from(ParsedImportSheet sheet) {
            return new Sheet(
                    sheet.role().name(),
                    sheet.fileName(),
                    headers(sheet),
                    sheet.rows().stream().map(Row::from).toList());
        }

        private static List<String> headers(ParsedImportSheet sheet) {
            return sheet.rows().stream()
                    .flatMap(row -> row.values().keySet().stream())
                    .distinct()
                    .toList();
        }
    }

    record Row(int rowNumber, Map<String, String> values) {

        static Row from(ParsedImportRow row) {
            return new Row(row.rowNumber(), row.values());
        }
    }
}
