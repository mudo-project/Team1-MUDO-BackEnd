package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import java.net.URI;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record RevenueReportAiEngineProperties(
        @Value("${REVENUE_REPORT_AI_BASE_URL:}") String baseUrl,
        @Value("${REVENUE_REPORT_AI_PATH:/api/revenue-report/generate}") String generatePath,
        @Value("${REVENUE_REPORT_AI_API_KEY:}") String apiKey,
        @Value("${REVENUE_REPORT_AI_CONNECT_TIMEOUT_MS:2000}") int connectTimeoutMs,
        @Value("${REVENUE_REPORT_AI_READ_TIMEOUT_MS:15000}") int readTimeoutMs
) {

    public RevenueReportAiEngineProperties {
        baseUrl = blankToEmpty(baseUrl);
        generatePath = blankToDefault(generatePath, "/api/revenue-report/generate");
        apiKey = blankToEmpty(apiKey);
        connectTimeoutMs = connectTimeoutMs > 0 ? connectTimeoutMs : 2000;
        readTimeoutMs = readTimeoutMs > 0 ? readTimeoutMs : 15000;
    }

    public boolean enabled() {
        return !baseUrl.isBlank();
    }

    public URI generateUri() {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = generatePath.startsWith("/") ? generatePath : "/" + generatePath;
        URI uri = URI.create(normalizedBaseUrl + normalizedPath);
        validateTransport(uri);
        return uri;
    }

    public void validateCredentialFor(URI uri) {
        if (!isLocalDevelopmentUri(uri) && apiKey.isBlank()) {
            throw new IllegalStateException("REVENUE_REPORT_AI_API_KEY is required for non-local FastAPI endpoints.");
        }
    }

    private static void validateTransport(URI uri) {
        String scheme = uri.getScheme();
        if (isLocalDevelopmentUri(uri) && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return;
        }
        if (!"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException(
                    "REVENUE_REPORT_AI_BASE_URL must use HTTPS for non-local FastAPI endpoints.");
        }
    }

    private static boolean isLocalDevelopmentUri(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return "localhost".equals(normalizedHost) || "127.0.0.1".equals(normalizedHost)
                || "::1".equals(normalizedHost) || "[::1]".equals(normalizedHost)
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
