package com.academy.mudogroupware.dataimport.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.dataimport.infrastructure.external.fastapi.FastApiImportAnalysisClient;
import com.academy.mudogroupware.dataimport.infrastructure.external.gemini.GeminiImportAnalysisAdapter;

@ExtendWith(MockitoExtension.class)
class DelegatingImportAnalysisAdapterTest {

    @Mock
    private FastApiImportAnalysisClient fastApiClient;

    @Mock
    private GeminiImportAnalysisAdapter geminiImportAnalysisAdapter;

    @InjectMocks
    private DelegatingImportAnalysisAdapter adapter;

    @Test
    void usesFastApiWhenItIsEnabledAndSuccessful() {
        ParsedImportSheet original = sheet("raw");
        ParsedImportSheet analyzed = sheet("fastapi");
        when(fastApiClient.enabled()).thenReturn(true);
        when(fastApiClient.analyze(List.of(original))).thenReturn(List.of(analyzed));

        List<ParsedImportSheet> result = adapter.analyze(List.of(original));

        assertThat(result).containsExactly(analyzed);
        verify(geminiImportAnalysisAdapter, never()).analyze(List.of(original));
    }

    @Test
    void fallsBackToGeminiWhenFastApiFails() {
        ParsedImportSheet original = sheet("raw");
        ParsedImportSheet analyzed = sheet("gemini");
        when(fastApiClient.enabled()).thenReturn(true);
        when(fastApiClient.analyze(List.of(original))).thenThrow(new IllegalStateException("timeout"));
        when(geminiImportAnalysisAdapter.analyze(List.of(original))).thenReturn(List.of(analyzed));

        List<ParsedImportSheet> result = adapter.analyze(List.of(original));

        assertThat(result).containsExactly(analyzed);
    }

    @Test
    void fallsBackToGeminiWhenFastApiIsDisabled() {
        ParsedImportSheet original = sheet("raw");
        ParsedImportSheet analyzed = sheet("gemini");
        when(fastApiClient.enabled()).thenReturn(false);
        when(geminiImportAnalysisAdapter.analyze(List.of(original))).thenReturn(List.of(analyzed));

        List<ParsedImportSheet> result = adapter.analyze(List.of(original));

        assertThat(result).containsExactly(analyzed);
        verify(fastApiClient, never()).analyze(List.of(original));
    }

    private ParsedImportSheet sheet(String value) {
        return new ParsedImportSheet(ImportFileRole.STUDENT, "students.csv", List.of(
                new ParsedImportRow(2, Map.of("name", value))));
    }
}
