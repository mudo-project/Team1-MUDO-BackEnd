package com.academy.mudogroupware.dataimport.infrastructure.external.fastapi;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.dataimport.application.port.ImportFileRole;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;

@Component
public class FastApiImportAnalysisClient {

    private static final String API_KEY_HEADER = "X-Data-Import-Ai-Key";
    private static final Set<String> CANONICAL_HEADERS = Set.of(
            "name",
            "grade",
            "school",
            "phone",
            "parentPhone",
            "note",
            "termName",
            "subjectName",
            "teacherId",
            "teacherName",
            "classroomName",
            "feeType",
            "feeAmount",
            "day",
            "dayOfWeek",
            "start",
            "startTime",
            "end",
            "endTime",
            "studentName",
            "studentPhone",
            "lectureName");

    private final RestClient restClient;
    private final DataImportAiEngineProperties properties;

    @Autowired
    public FastApiImportAnalysisClient(DataImportAiEngineProperties properties) {
        this(RestClient.builder()
                .requestFactory(requestFactory(properties))
                .build(), properties);
    }

    FastApiImportAnalysisClient(RestClient restClient, DataImportAiEngineProperties properties) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public List<ParsedImportSheet> analyze(List<ParsedImportSheet> sheets) {
        List<ParsedImportSheet> safeSheets = sheets != null ? List.copyOf(sheets) : List.of();
        if (safeSheets.isEmpty() || !properties.enabled()) {
            return safeSheets;
        }

        URI analyzeUri = properties.analyzeUri();
        properties.validateCredentialFor(analyzeUri);

        RestClient.RequestBodySpec request = restClient.post()
                .uri(analyzeUri)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (!properties.apiKey().isBlank()) {
            request.header(API_KEY_HEADER, properties.apiKey());
        }

        FastApiImportAnalysisResponse response = request
                .body(FastApiImportAnalysisRequest.from(safeSheets))
                .retrieve()
                .body(FastApiImportAnalysisResponse.class);
        return applyResponse(safeSheets, response);
    }

    private List<ParsedImportSheet> applyResponse(List<ParsedImportSheet> sheets,
                                                  FastApiImportAnalysisResponse response) {
        if (response == null || response.sheets() == null || response.sheets().isEmpty()) {
            return sheets;
        }

        List<ParsedImportSheet> analyzedSheets = new ArrayList<>();
        for (ParsedImportSheet sheet : sheets) {
            FastApiImportAnalysisResponse.Sheet responseSheet = findResponseSheet(response, sheet);
            analyzedSheets.add(responseSheet == null ? sheet : applyResponseSheet(sheet, responseSheet));
        }
        return analyzedSheets;
    }

    private FastApiImportAnalysisResponse.Sheet findResponseSheet(FastApiImportAnalysisResponse response,
                                                                  ParsedImportSheet sheet) {
        return response.sheets().stream()
                .filter(candidate -> role(candidate) == sheet.role())
                .filter(candidate -> isBlank(candidate.fileName()) || candidate.fileName().equals(sheet.fileName()))
                .findFirst()
                .orElse(null);
    }

    private ParsedImportSheet applyResponseSheet(ParsedImportSheet sheet,
                                                 FastApiImportAnalysisResponse.Sheet responseSheet) {
        Map<Integer, Map<String, String>> responseRows = responseRowsByNumber(responseSheet);
        List<ParsedImportRow> rows = sheet.rows().stream()
                .map(row -> applyResponseRow(row, responseSheet.headerMappings(), responseRows.get(row.rowNumber())))
                .toList();
        return new ParsedImportSheet(sheet.role(), sheet.fileName(), rows);
    }

    private ParsedImportRow applyResponseRow(ParsedImportRow row, Map<String, String> headerMappings,
                                             Map<String, String> normalizedValues) {
        Map<String, String> values = new LinkedHashMap<>(row.values());
        applyHeaderMappings(values, row.values(), headerMappings);
        applyNormalizedValues(values, normalizedValues);
        return new ParsedImportRow(row.rowNumber(), values);
    }

    private void applyHeaderMappings(Map<String, String> target, Map<String, String> source,
                                     Map<String, String> headerMappings) {
        if (headerMappings == null || headerMappings.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : headerMappings.entrySet()) {
            if (!isAllowedCanonicalHeader(entry.getValue())) {
                continue;
            }
            String value = findValue(source, entry.getKey());
            if (!isBlank(value)) {
                target.put(entry.getValue(), value);
            }
        }
    }

    private void applyNormalizedValues(Map<String, String> target, Map<String, String> normalizedValues) {
        if (normalizedValues == null || normalizedValues.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : normalizedValues.entrySet()) {
            if (isAllowedCanonicalHeader(entry.getKey()) && !isBlank(entry.getValue())) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private Map<Integer, Map<String, String>> responseRowsByNumber(FastApiImportAnalysisResponse.Sheet sheet) {
        if (sheet.rows() == null || sheet.rows().isEmpty()) {
            return Map.of();
        }
        Map<Integer, Map<String, String>> rows = new LinkedHashMap<>();
        for (FastApiImportAnalysisResponse.Row row : sheet.rows()) {
            rows.put(row.rowNumber(), row.values());
        }
        return rows;
    }

    private ImportFileRole role(FastApiImportAnalysisResponse.Sheet sheet) {
        try {
            return sheet.role() == null ? null : ImportFileRole.valueOf(sheet.role());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String findValue(Map<String, String> values, String header) {
        String normalizedHeader = normalize(header);
        return values.entrySet().stream()
                .filter(entry -> normalize(entry.getKey()).equals(normalizedHeader))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isAllowedCanonicalHeader(String header) {
        if (isBlank(header)) {
            return false;
        }
        return CANONICAL_HEADERS.contains(header) || header.matches("(day|dayOfWeek|start|startTime|end|endTime)[1-7]");
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static SimpleClientHttpRequestFactory requestFactory(DataImportAiEngineProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.connectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));
        return requestFactory;
    }
}
