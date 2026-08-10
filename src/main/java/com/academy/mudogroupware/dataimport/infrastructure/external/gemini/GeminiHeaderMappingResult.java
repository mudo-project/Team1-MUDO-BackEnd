package com.academy.mudogroupware.dataimport.infrastructure.external.gemini;

import java.util.List;
import java.util.Map;

import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;

record GeminiHeaderMappingResult(List<SheetMapping> sheets) {

    record SheetMapping(
            ImportFileRole role,
            String fileName,
            Map<String, String> headerMappings
    ) {
    }
}
