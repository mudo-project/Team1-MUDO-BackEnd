package com.academy.mudogroupware.dataimport.infrastructure.external;

import java.util.List;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.dataimport.application.port.ImportAnalysisPort;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.infrastructure.external.fastapi.FastApiImportAnalysisClient;
import com.academy.mudogroupware.dataimport.infrastructure.external.gemini.GeminiImportAnalysisAdapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DelegatingImportAnalysisAdapter implements ImportAnalysisPort {

    private final FastApiImportAnalysisClient fastApiImportAnalysisClient;
    private final GeminiImportAnalysisAdapter geminiImportAnalysisAdapter;

    @Override
    public List<ParsedImportSheet> analyze(List<ParsedImportSheet> sheets) {
        List<ParsedImportSheet> safeSheets = sheets != null ? List.copyOf(sheets) : List.of();
        if (fastApiImportAnalysisClient.enabled()) {
            try {
                return fastApiImportAnalysisClient.analyze(safeSheets);
            } catch (RuntimeException e) {
                log.warn("event=data_import_fastapi_analysis_fallback reason={}", e.getMessage());
            }
        }
        return geminiImportAnalysisAdapter.analyze(safeSheets);
    }
}
