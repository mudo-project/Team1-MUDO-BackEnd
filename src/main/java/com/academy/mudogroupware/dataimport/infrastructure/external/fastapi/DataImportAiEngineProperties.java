package com.academy.mudogroupware.dataimport.infrastructure.external.fastapi;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record DataImportAiEngineProperties(
        @Value("${DATA_IMPORT_AI_BASE_URL:}") String baseUrl,
        @Value("${DATA_IMPORT_AI_PATH:/api/import/analyze}") String analyzePath,
        @Value("${DATA_IMPORT_AI_API_KEY:}") String apiKey,
        @Value("${DATA_IMPORT_AI_CONNECT_TIMEOUT_MS:2000}") int connectTimeoutMs,
        @Value("${DATA_IMPORT_AI_READ_TIMEOUT_MS:8000}") int readTimeoutMs
) {

    public DataImportAiEngineProperties {
        baseUrl = blankToEmpty(baseUrl);
        analyzePath = blankToDefault(analyzePath, "/api/import/analyze");
        apiKey = blankToEmpty(apiKey);
        connectTimeoutMs = connectTimeoutMs > 0 ? connectTimeoutMs : 2000;
        readTimeoutMs = readTimeoutMs > 0 ? readTimeoutMs : 8000;
    }

    public boolean enabled() {
        return !baseUrl.isBlank();
    }

    public URI analyzeUri() {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        String normalizedPath = analyzePath.startsWith("/") ? analyzePath : "/" + analyzePath;
        return URI.create(normalizedBaseUrl + normalizedPath);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToDefault(String value, String defaultValue) {
        String safeValue = blankToEmpty(value);
        return safeValue.isBlank() ? defaultValue : safeValue;
    }
}
