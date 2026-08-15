package com.academy.mudogroupware.platform.infrastructure.prometheus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.ApiCallMetric;
import com.academy.mudogroupware.platform.domain.model.DashboardPeriod;
import com.academy.mudogroupware.platform.infrastructure.PlatformDashboardProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PrometheusOperationalMetricsAdapterTest {

  private final PrometheusOperationalMetricsAdapter adapter =
      new PrometheusOperationalMetricsAdapter(new PlatformDashboardProperties(), Runnable::run);

  @Test
  void tenantMatcherFallsBackToMatchAllWhenAcademiesEmpty() {
    assertThat(adapter.tenantMatcher(List.of())).isEqualTo(".*");
  }

  @Test
  void tenantMatcherJoinsAcademyCodesWithPipe() {
    assertThat(adapter.tenantMatcher(List.of(academy("academy-a"), academy("academy-b"))))
        .isEqualTo("academy-a|academy-b");
  }

  @Test
  void apiCallMetricsByAcademyIncludesAllElevenCategoriesForEveryAcademy() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/api/v1/query", exchange -> {
      String query = exchange.getRequestURI().getQuery();
      String body = query != null && query.contains("check-ins")
          ? "{\"status\":\"success\",\"data\":{\"result\":["
              + "{\"metric\":{\"tenant\":\"academy-a\"},\"value\":[0,\"5\"]}]}}"
          : "{\"status\":\"success\",\"data\":{\"result\":[]}}";
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    try {
      PlatformDashboardProperties properties = new PlatformDashboardProperties();
      properties.setPrometheusUrl("http://localhost:" + server.getAddress().getPort());
      PrometheusOperationalMetricsAdapter localAdapter =
          new PrometheusOperationalMetricsAdapter(properties, Runnable::run);

      Map<String, List<ApiCallMetric>> result = localAdapter.apiCallMetricsByAcademy(
          List.of(academy("academy-a"), academy("academy-b")), DashboardPeriod.LAST_HOUR);

      assertThat(result.get("academy-a")).hasSize(11);
      assertThat(result.get("academy-a"))
          .filteredOn(metric -> metric.category().equals("CHECK_IN"))
          .extracting(ApiCallMetric::count)
          .containsExactly(5L);
      assertThat(result.get("academy-b")).hasSize(11);
      assertThat(result.get("academy-b")).allMatch(metric -> metric.count() == 0);
    } finally {
      server.stop(0);
    }
  }

  @Test
  void scalarQueryFailsFastInsteadOfHangingWhenPrometheusIsSlow() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/api/v1/query", exchange -> {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      byte[] bytes = "{\"status\":\"success\",\"data\":{\"result\":[]}}".getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    try {
      PlatformDashboardProperties properties = new PlatformDashboardProperties();
      properties.setPrometheusUrl("http://localhost:" + server.getAddress().getPort());
      properties.setPrometheusReadTimeoutMs(200);
      PrometheusOperationalMetricsAdapter localAdapter =
          new PrometheusOperationalMetricsAdapter(properties, Runnable::run);

      long startedAt = System.nanoTime();
      assertThatThrownBy(() -> localAdapter.activeDatabaseConnections(List.of(academy("academy-a"))))
          .isInstanceOf(PlatformException.class)
          .satisfies(exception ->
              assertThat(((PlatformException) exception).getErrorCode()).isEqualTo(PlatformErrorCode.METRICS_UNAVAILABLE));
      long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
      assertThat(elapsedMs).isLessThan(900);
    } finally {
      server.stop(0);
    }
  }

  private AcademyRuntime academy(String code) {
    return new AcademyRuntime(code, "cluster", "service", "rds", 100, 0.7, "staff", "finance", "tenants/" + code + "/");
  }
}
