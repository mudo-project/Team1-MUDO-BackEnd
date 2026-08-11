package com.academy.mudogroupware.dataimport.infrastructure.external.fastapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

class FastApiImportAnalysisClientTest {

    @Test
    void returnsOriginalSheetsWhenBaseUrlIsMissing() {
        FastApiImportAnalysisClient client = new FastApiImportAnalysisClient(RestClient.builder().build(),
                new DataImportAiEngineProperties("", "/api/import/analyze", "", 2000, 8000));
        ParsedImportSheet sheet = studentSheet();

        List<ParsedImportSheet> result = client.analyze(List.of(sheet));

        assertThat(result).containsExactly(sheet);
    }

    @Test
    void rejectsNonLocalHttpEndpoint() {
        FastApiImportAnalysisClient client = new FastApiImportAnalysisClient(RestClient.builder().build(),
                new DataImportAiEngineProperties("http://ai.example.com", "/api/import/analyze",
                        "secret", 2000, 8000));

        assertThatThrownBy(() -> client.analyze(List.of(studentSheet())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void rejectsNonLocalEndpointWithoutApiKey() {
        FastApiImportAnalysisClient client = new FastApiImportAnalysisClient(RestClient.builder().build(),
                new DataImportAiEngineProperties("https://ai.example.com", "/api/import/analyze",
                        "", 2000, 8000));

        assertThatThrownBy(() -> client.analyze(List.of(studentSheet())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATA_IMPORT_AI_API_KEY");
    }

    @Test
    void appliesNormalizedRowValuesFromFastApiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FastApiImportAnalysisClient client = new FastApiImportAnalysisClient(builder.build(),
                new DataImportAiEngineProperties("http://localhost:8000", "/api/import/analyze",
                        "secret", 2000, 8000));
        String responseJson = """
                {"sheets":[{"role":"STUDENT","fileName":"students.csv","rows":[{"rowNumber":2,"values":{"name":"Kim","grade":"HIGH_1","ignored":"x"}}]}]}
                """;
        server.expect(requestTo("http://localhost:8000/api/import/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Data-Import-Ai-Key", "secret"))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<ParsedImportSheet> result = client.analyze(List.of(studentSheet()));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).rows().get(0).values())
                .containsEntry("student_name_column", "Kim")
                .containsEntry("name", "Kim")
                .containsEntry("grade", "HIGH_1")
                .doesNotContainKey("ignored");
        server.verify();
    }

    @Test
    void appliesHeaderMappingsWhenRowsAreNotReturned() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FastApiImportAnalysisClient client = new FastApiImportAnalysisClient(builder.build(),
                new DataImportAiEngineProperties("http://localhost:8000", "/api/import/analyze",
                        "", 2000, 8000));
        String responseJson = """
                {"sheets":[{"role":"STUDENT","fileName":"students.csv","headerMappings":{"student_name_column":"name","grade_column":"grade"}}]}
                """;
        server.expect(requestTo("http://localhost:8000/api/import/analyze"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        List<ParsedImportSheet> result = client.analyze(List.of(studentSheet()));

        assertThat(result.get(0).rows().get(0).values())
                .containsEntry("name", "Kim")
                .containsEntry("grade", "HIGH_1");
        server.verify();
    }

    private ParsedImportSheet studentSheet() {
        return new ParsedImportSheet(ImportFileRole.STUDENT, "students.csv", List.of(
                new ParsedImportRow(2, Map.of(
                        "student_name_column", "Kim",
                        "grade_column", "HIGH_1"))));
    }
}
