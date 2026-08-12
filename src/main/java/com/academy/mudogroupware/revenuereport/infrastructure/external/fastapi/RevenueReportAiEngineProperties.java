package com.academy.mudogroupware.revenuereport.infrastructure.external.fastapi;

import java.net.URI;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import jakarta.annotation.PostConstruct;

/**
 * {@code app.revenue-report.ai.*} 프로퍼티 키로 바인딩한다(환경변수 REVENUE_REPORT_AI_*는
 * application.yaml에서 그대로 참조하되, 키 자체는 profile별로 재정의 가능하게 함).
 * {@link #validateAtStartup()}이 활성화 상태(enabled)일 때 URL/자격증명을 부팅 시점에
 * 검증해서, 잘못된 설정을 배치 실행 시점(한 달 뒤)이 아니라 배포 시점에 잡는다.
 */
@ConfigurationProperties(prefix = "app.revenue-report.ai")
public record RevenueReportAiEngineProperties(
        @DefaultValue("") String baseUrl,
        @DefaultValue("/api/revenue-report/generate") String generatePath,
        @DefaultValue("") String apiKey,
        @DefaultValue("2000") int connectTimeoutMs,
        @DefaultValue("15000") int readTimeoutMs
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

    /** enabled()가 true면 URL/전송방식/자격증명을 부팅 시점에 미리 검증한다. */
    @PostConstruct
    void validateAtStartup() {
        if (enabled()) {
            validateCredentialFor(generateUri());
        }
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
