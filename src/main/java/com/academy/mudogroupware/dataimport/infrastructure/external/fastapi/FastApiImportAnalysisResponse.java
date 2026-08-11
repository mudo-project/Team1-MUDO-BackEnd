package com.academy.mudogroupware.dataimport.infrastructure.external.fastapi;

import java.util.List;
import java.util.Map;

record FastApiImportAnalysisResponse(List<Sheet> sheets) {

    record Sheet(String role, String fileName, Map<String, String> headerMappings, List<Row> rows) {
    }

    record Row(int rowNumber, Map<String, String> values) {
    }
}
