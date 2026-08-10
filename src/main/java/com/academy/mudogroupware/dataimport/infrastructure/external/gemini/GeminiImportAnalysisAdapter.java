package com.academy.mudogroupware.dataimport.infrastructure.external.gemini;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.academy.mudogroupware.dataimport.application.port.ImportAnalysisPort;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportRow;
import com.academy.mudogroupware.dataimport.application.port.ParsedImportSheet;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeminiImportAnalysisAdapter implements ImportAnalysisPort {

    private static final int SAMPLE_ROW_LIMIT = 5;
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

    private final RestClient geminiRestClient;
    private final DataImportGeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;

    @Autowired
    public GeminiImportAnalysisAdapter(DataImportGeminiProperties geminiProperties,
                                       ObjectMapper objectMapper) {
        this(RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build(), geminiProperties, objectMapper);
    }

    GeminiImportAnalysisAdapter(RestClient geminiRestClient,
                                DataImportGeminiProperties geminiProperties,
                                ObjectMapper objectMapper) {
        this.geminiRestClient = geminiRestClient;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ParsedImportSheet> analyze(List<ParsedImportSheet> sheets) {
        List<ParsedImportSheet> safeSheets = sheets != null ? List.copyOf(sheets) : List.of();
        if (safeSheets.isEmpty() || isBlank(geminiProperties.apiKey())) {
            return safeSheets;
        }

        try {
            GeminiImportAnalysisResponse response = geminiRestClient.post()
                    .uri("/v1beta/models/{model}:generateContent", geminiProperties.model())
                    .header("x-goog-api-key", geminiProperties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GeminiImportAnalysisRequest.of(buildPrompt(safeSheets)))
                    .retrieve()
                    .body(GeminiImportAnalysisResponse.class);
            String text = response != null ? response.firstText() : null;
            if (isBlank(text)) {
                return safeSheets;
            }
            return applyMappings(safeSheets, parseHeaderMapping(text));
        } catch (RuntimeException | JsonProcessingException e) {
            log.warn("event=data_import_ai_analysis_fallback reason={}", e.getMessage());
            return safeSheets;
        }
    }

    private String buildPrompt(List<ParsedImportSheet> sheets) throws JsonProcessingException {
        return """
                You are helping map academy onboarding import spreadsheet headers.
                Return JSON only. Do not include markdown.
                The response schema is:
                {"sheets":[{"role":"STUDENT|LECTURE|ENROLLMENT","fileName":"string","headerMappings":{"originalHeader":"canonicalHeader"}}]}

                Canonical headers:
                STUDENT: name, grade, school, phone, parentPhone, note
                LECTURE: name, grade, termName, subjectName, teacherId, teacherName, classroomName, feeType, feeAmount, day, dayOfWeek, start, startTime, end, endTime
                LECTURE repeated schedules may use day1, startTime1, endTime1, day2, startTime2, endTime2, up to day7/startTime7/endTime7.
                ENROLLMENT: studentName, studentPhone, lectureName, teacherName

                Map only headers you are confident about. Preserve the given role and fileName.
                Input sheets with headers and sample rows:
                %s
                """.formatted(objectMapper.writeValueAsString(toPromptPayload(sheets)));
    }

    private List<Map<String, Object>> toPromptPayload(List<ParsedImportSheet> sheets) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (ParsedImportSheet sheet : sheets) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("role", sheet.role());
            item.put("fileName", sheet.fileName());
            item.put("headers", headers(sheet));
            item.put("sampleRows", sheet.rows().stream()
                    .limit(SAMPLE_ROW_LIMIT)
                    .map(ParsedImportRow::values)
                    .toList());
            payload.add(item);
        }
        return payload;
    }

    private List<String> headers(ParsedImportSheet sheet) {
        return sheet.rows().stream()
                .flatMap(row -> row.values().keySet().stream())
                .distinct()
                .toList();
    }

    private GeminiHeaderMappingResult parseHeaderMapping(String text) throws JsonProcessingException {
        String json = extractJson(text);
        return objectMapper.readValue(json, GeminiHeaderMappingResult.class);
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < start) {
            return trimmed;
        }
        return trimmed.substring(start, end + 1);
    }

    private List<ParsedImportSheet> applyMappings(List<ParsedImportSheet> sheets,
                                                  GeminiHeaderMappingResult result) {
        if (result == null || result.sheets() == null || result.sheets().isEmpty()) {
            return sheets;
        }

        List<ParsedImportSheet> mappedSheets = new ArrayList<>();
        for (ParsedImportSheet sheet : sheets) {
            GeminiHeaderMappingResult.SheetMapping mapping = findMapping(result, sheet);
            if (mapping == null || mapping.headerMappings() == null || mapping.headerMappings().isEmpty()) {
                mappedSheets.add(sheet);
                continue;
            }
            mappedSheets.add(applyMapping(sheet, mapping.headerMappings()));
        }
        return mappedSheets;
    }

    private GeminiHeaderMappingResult.SheetMapping findMapping(GeminiHeaderMappingResult result,
                                                               ParsedImportSheet sheet) {
        return result.sheets().stream()
                .filter(mapping -> mapping.role() == sheet.role())
                .filter(mapping -> isBlank(mapping.fileName()) || mapping.fileName().equals(sheet.fileName()))
                .findFirst()
                .orElse(null);
    }

    private ParsedImportSheet applyMapping(ParsedImportSheet sheet, Map<String, String> headerMappings) {
        List<ParsedImportRow> rows = sheet.rows().stream()
                .map(row -> applyMapping(row, headerMappings))
                .toList();
        return new ParsedImportSheet(sheet.role(), sheet.fileName(), rows);
    }

    private ParsedImportRow applyMapping(ParsedImportRow row, Map<String, String> headerMappings) {
        Map<String, String> mappedValues = new LinkedHashMap<>(row.values());
        for (Map.Entry<String, String> entry : headerMappings.entrySet()) {
            String canonicalHeader = entry.getValue();
            if (!isAllowedCanonicalHeader(canonicalHeader)) {
                continue;
            }
            String value = findValue(row.values(), entry.getKey());
            if (!isBlank(value)) {
                mappedValues.put(canonicalHeader, value);
            }
        }
        return new ParsedImportRow(row.rowNumber(), mappedValues);
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
}
