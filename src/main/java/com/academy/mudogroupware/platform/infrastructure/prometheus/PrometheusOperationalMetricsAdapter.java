package com.academy.mudogroupware.platform.infrastructure.prometheus;

import com.academy.mudogroupware.platform.application.port.ApiCallFrequencyPort;
import com.academy.mudogroupware.platform.application.port.OperationalMetricsPort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.infrastructure.PlatformDashboardProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
public class PrometheusOperationalMetricsAdapter implements OperationalMetricsPort, ApiCallFrequencyPort {
  private static final List<ApiCategory> API_CATEGORIES = categories();
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final PlatformDashboardProperties properties;
  private final Executor executor;
  private final RestClient restClient;

  public PrometheusOperationalMetricsAdapter(
      PlatformDashboardProperties properties, @Qualifier("applicationTaskExecutor") Executor executor) {
    this.properties = properties;
    this.executor = executor;
    this.restClient = RestClient.builder().requestFactory(requestFactory(properties)).build();
  }

  // Prometheus가 응답 없이 멈추면 이 스레드가 applicationTaskExecutor를 무기한 붙잡아 풀을 고갈시키고,
  // 이후 무관한 요청까지 TaskRejectedException(503)으로 튕겨나간다 — 타임아웃으로 상한을 둔다.
  private static SimpleClientHttpRequestFactory requestFactory(PlatformDashboardProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(properties.getPrometheusConnectTimeoutMs()));
    requestFactory.setReadTimeout(Duration.ofMillis(properties.getPrometheusReadTimeoutMs()));
    return requestFactory;
  }

  @Override
  public List<ApiCallMetric> apiCallMetrics(List<AcademyRuntime> academies, DashboardPeriod period) {
    String tenantMatcher = tenantMatcher(academies);
    String window = window(period);
    List<CompletableFuture<ApiCallMetric>> futures = API_CATEGORIES.stream()
        .map(category -> CompletableFuture.supplyAsync(() -> new ApiCallMetric(category.name(), Math.round(scalar(
            "sum(increase(http_server_requests_seconds_count{tenant=~\"%s\",method=~\"%s\",uri=~\"%s\"}[%s]))"
                .formatted(tenantMatcher, category.methodPattern(), category.uriPattern(), window)))), executor))
        .toList();
    return futures.stream().map(CompletableFuture::join).toList();
  }

  @Override
  public Map<String, List<ApiCallMetric>> apiCallMetricsByAcademy(List<AcademyRuntime> academies, DashboardPeriod period) {
    String tenantMatcher = tenantMatcher(academies);
    String window = window(period);
    // 학원마다 11개 카테고리를 항상 채운다(activity 없는 조합은 count 0) — operational-metrics의
    // apiCallMetrics()와 동일한 규칙으로 맞춰, 클라이언트가 "배열에 없으면 0"을 직접 처리하지 않게 한다.
    Map<String, Map<String, Long>> countsByTenantAndCategory = new ConcurrentHashMap<>();
    List<CompletableFuture<Void>> futures = API_CATEGORIES.stream()
        .map(category -> CompletableFuture.runAsync(() -> {
          Map<String, Long> countsByTenant = scalarByTenant(
              "sum by (tenant) (increase(http_server_requests_seconds_count{tenant=~\"%s\",method=~\"%s\",uri=~\"%s\"}[%s]))"
                  .formatted(tenantMatcher, category.methodPattern(), category.uriPattern(), window));
          countsByTenant.forEach((tenant, count) -> countsByTenantAndCategory
              .computeIfAbsent(tenant, ignored -> new ConcurrentHashMap<>())
              .put(category.name(), count));
        }, executor))
        .toList();
    futures.forEach(CompletableFuture::join);

    Map<String, List<ApiCallMetric>> byAcademy = new java.util.LinkedHashMap<>();
    for (AcademyRuntime academy : academies) {
      Map<String, Long> counts = countsByTenantAndCategory.getOrDefault(academy.code(), Map.of());
      byAcademy.put(academy.code(), API_CATEGORIES.stream()
          .map(category -> new ApiCallMetric(category.name(), counts.getOrDefault(category.name(), 0L)))
          .toList());
    }
    return byAcademy;
  }

  @Override
  public double p95ResponseMilliseconds(List<AcademyRuntime> academies, DashboardPeriod period) {
    return scalar("histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{tenant=~\"%s\"}[%s])) by (le)) * 1000"
        .formatted(tenantMatcher(academies), window(period)));
  }

  @Override
  public double errorRatePercent(List<AcademyRuntime> academies, DashboardPeriod period) {
    String labels = "tenant=~\"%s\"".formatted(tenantMatcher(academies));
    return scalar("100 * sum(rate(http_server_requests_seconds_count{%s,status=~\"5..\"}[%s])) / clamp_min(sum(rate(http_server_requests_seconds_count{%s}[%s])), 1)"
        .formatted(labels, window(period), labels, window(period)));
  }

  @Override
  public int activeDatabaseConnections(List<AcademyRuntime> academies) {
    return (int) scalar("sum(hikaricp_connections_active{tenant=~\"%s\"})".formatted(tenantMatcher(academies)));
  }

  private JsonNode executeQuery(String query) {
    // PromQL은 label selector에 {..."..."~...|...} 같은 문자를 쓰는데, UriComponentsBuilder는
    // '{'/'}'를 URI 템플릿 변수 문법으로 취급해 encode()를 거쳐도 그대로 남긴다({}/"/| 는 URI
    // query에서 허용되지 않는 문자라 URISyntaxException이 난다). URLEncoder로 직접
    // percent-encode해서 템플릿 해석 자체를 우회한다.
    String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8);
    URI uri = URI.create(properties.getPrometheusUrl() + "/api/v1/query?query=" + encodedQuery);
    return restClient.get().uri(uri).retrieve().body(JsonNode.class);
  }

  private double scalar(String query) {
    try {
      JsonNode root = executeQuery(query);
      JsonNode result = root.path("data").path("result");
      if (!"success".equals(root.path("status").asText()) || result.isEmpty()) return 0;
      return result.get(0).path("value").get(1).asDouble(0);
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }

  private Map<String, Long> scalarByTenant(String query) {
    try {
      JsonNode root = executeQuery(query);
      JsonNode result = root.path("data").path("result");
      if (!"success".equals(root.path("status").asText()) || result.isEmpty()) return Map.of();
      Map<String, Long> counts = new java.util.LinkedHashMap<>();
      result.forEach(row -> counts.put(
          row.path("metric").path("tenant").asText(),
          Math.round(row.path("value").get(1).asDouble(0))));
      return counts;
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }

  private String window(DashboardPeriod period) {
    return switch (period) {
      case LAST_HOUR -> "1h";
      case LAST_24_HOURS -> "24h";
      case TODAY -> Math.max(60, Duration.between(
          LocalDate.now(KST).atStartOfDay(KST), ZonedDateTime.now(KST)).toSeconds()) + "s";
    };
  }

  String tenantMatcher(List<AcademyRuntime> academies) {
    return academies.isEmpty() ? ".*" : academies.stream().map(AcademyRuntime::code).collect(Collectors.joining("|"));
  }

  private static List<ApiCategory> categories() {
    return List.of(
        new ApiCategory("INITIAL_DATA_READ", "GET",
            "/api/(users|roles|permissions|workspaces|calendars).*"),
        new ApiCategory("ACCOUNT_ISSUANCE", "POST", "/api/users"),
        new ApiCategory("CHECK_IN", "POST", "/api/attendance/check-ins"),
        new ApiCategory("ATTENDANCE_EXPORT", "GET",
            "/api/rollcall/lectures/.*/attendance/export"),
        new ApiCategory("NOTICE_CREATE", "POST", "/api/notices"),
        new ApiCategory("WORKSPACE_TASK_CREATE", "POST", "/api/workspaces/.*/tasks"),
        new ApiCategory("WORKSPACE_TASK_STATUS_CHANGE", "PATCH",
            "/api/workspaces/.*/tasks.*"),
        new ApiCategory("APPROVAL_SUBMISSION", "POST", "/api/approvals"),
        new ApiCategory("SETTLEMENT_SUBMISSION", "POST",
            "/api/corporate-card/transactions.*"),
        new ApiCategory("CALENDAR_CREATE", "POST", "/api/calendars"),
        new ApiCategory("MEMO_CREATE", "POST", "/api/memos"));
  }

  private record ApiCategory(String name, String methodPattern, String uriPattern) {}
}
