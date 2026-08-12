package com.academy.mudogroupware.dataimport.infrastructure.external.gemini;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.academy.mudogroupware.global.infrastructure.observability.ai.GeminiTokenUsageTracker;
import com.fasterxml.jackson.databind.ObjectMapper;

class GeminiImportAnalysisAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiTokenUsageTracker tokenUsageTracker = new GeminiTokenUsageTracker();

    @Test
    void returnsOriginalSheetsWhenApiKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiImportAnalysisAdapter adapter = new GeminiImportAnalysisAdapter(
                builder.build(), new DataImportGeminiProperties("", "gemini-test"), objectMapper, tokenUsageTracker);
        ParsedImportSheet sheet = studentSheet();

        List<ParsedImportSheet> result = adapter.analyze(List.of(sheet));

        assertThat(result).containsExactly(sheet);
        server.verify();
    }

    @Test
    void appliesHeaderMappingsFromGeminiResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiImportAnalysisAdapter adapter = new GeminiImportAnalysisAdapter(
                builder.build(), new DataImportGeminiProperties("test-key", "gemini-test"), objectMapper,
                tokenUsageTracker);
        String analysisJson = """
                {"sheets":[{"role":"STUDENT","fileName":"students.csv","headerMappings":{"student_name_column":"name","grade_column":"grade"}}]}
                """;
        String responseJson = """
                {"candidates":[{"content":{"parts":[{"text":%s}]}}],
                 "usageMetadata":{"promptTokenCount":80,"candidatesTokenCount":20,"totalTokenCount":100}}
                """.formatted(objectMapper.writeValueAsString(analysisJson));
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<ParsedImportSheet> result = adapter.analyze(List.of(studentSheet()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rows().get(0).values())
                .containsEntry("student_name_column", "Kim")
                .containsEntry("name", "Kim")
                .containsEntry("grade", "HIGH_1");
        assertThat(tokenUsageTracker.snapshot()).hasSize(1);
        assertThat(tokenUsageTracker.snapshot().get(0).feature()).isEqualTo("dataimport-onboarding-analysis");
        assertThat(tokenUsageTracker.snapshot().get(0).totalTokens()).isEqualTo(100);
        server.verify();
    }

    @Test
    void returnsOriginalSheetsWhenGeminiResponseCannotBeParsed() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GeminiImportAnalysisAdapter adapter = new GeminiImportAnalysisAdapter(
                builder.build(), new DataImportGeminiProperties("test-key", "gemini-test"), objectMapper,
                tokenUsageTracker);
        ParsedImportSheet sheet = studentSheet();
        server.expect(requestTo(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        List<ParsedImportSheet> result = adapter.analyze(List.of(sheet));

        assertThat(result).containsExactly(sheet);
        server.verify();
    }

    private ParsedImportSheet studentSheet() {
        return new ParsedImportSheet(ImportFileRole.STUDENT, "students.csv", List.of(
                new ParsedImportRow(2, Map.of(
                        "student_name_column", "Kim",
                        "grade_column", "HIGH_1"))));
    }
}
