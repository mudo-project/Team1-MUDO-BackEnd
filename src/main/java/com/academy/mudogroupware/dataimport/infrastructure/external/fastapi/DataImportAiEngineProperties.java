package com.academy.mudogroupware.dataimport.infrastructure.external.fastapi;

import java.net.URI;
import java.util.Locale;

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
        URI uri = URI.create(normalizedBaseUrl + normalizedPath);
        validateTransport(uri);
        return uri;
    }

    public void validateCredentialFor(URI analyzeUri) {
        if (!isLocalDevelopmentUri(analyzeUri) && apiKey.isBlank()) {
            throw new IllegalStateException("DATA_IMPORT_AI_API_KEY is required for non-local FastAPI endpoints.");
        }
    }

    private static void validateTransport(URI uri) {
        String scheme = uri.getScheme();
        if (isLocalDevelopmentUri(uri)
                && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return;
        }
        if (!"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "DATA_IMPORT_AI_BASE_URL must use HTTPS for non-local FastAPI endpoints.");
        }
    }

    private static boolean isLocalDevelopmentUri(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalizedHost)
                || "127.0.0.1".equals(normalizedHost)
                || "::1".equals(normalizedHost)
                || "[::1]".equals(normalizedHost)
                || "0:0:0:0:0:0:0:1".equals(normalizedHost);
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToDefault(String value, String defaultValue) {
        String safeValue = blankToEmpty(value);
        return safeValue.isBlank() ? defaultValue : safeValue;
    }
}
