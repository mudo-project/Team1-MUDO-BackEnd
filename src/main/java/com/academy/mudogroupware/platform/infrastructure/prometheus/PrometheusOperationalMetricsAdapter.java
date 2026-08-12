package com.academy.mudogroupware.platform.infrastructure.prometheus;

import com.academy.mudogroupware.platform.application.port.OperationalMetricsPort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.infrastructure.PlatformDashboardProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PrometheusOperationalMetricsAdapter implements OperationalMetricsPort {
  private static final List<ApiCategory> API_CATEGORIES = categories();
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final PlatformDashboardProperties properties;

  @Override
  public List<ApiCallMetric> apiCallMetrics(List<AcademyRuntime> academies, DashboardPeriod period) {
    String tenantMatcher = tenantMatcher(academies);
    String window = window(period);
    return API_CATEGORIES.stream()
        .map(category -> new ApiCallMetric(category.name(), Math.round(scalar(
            "sum(increase(http_server_requests_seconds_count{tenant=~\"%s\",method=~\"%s\",uri=~\"%s\"}[%s]))"
                .formatted(tenantMatcher, category.methodPattern(), category.uriPattern(), window)))))
        .toList();
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

  private double scalar(String query) {
    try {
      JsonNode root = RestClient.create(properties.getPrometheusUrl())
          .get().uri(uri -> uri.path("/api/v1/query").queryParam("query", query).build())
          .retrieve().body(JsonNode.class);
      JsonNode result = root.path("data").path("result");
      if (!"success".equals(root.path("status").asText()) || result.isEmpty()) return 0;
      return result.get(0).path("value").get(1).asDouble(0);
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

  private String tenantMatcher(List<AcademyRuntime> academies) {
    return academies.stream().map(AcademyRuntime::code).collect(Collectors.joining("|"));
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
