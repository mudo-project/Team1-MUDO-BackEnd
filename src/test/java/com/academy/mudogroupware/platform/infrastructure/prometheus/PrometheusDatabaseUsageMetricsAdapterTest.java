package com.academy.mudogroupware.platform.infrastructure.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.platform.infrastructure.PlatformDashboardProperties;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrometheusDatabaseUsageMetricsAdapterTest {

  @Test
  void databaseBytesQueriesPrometheusWithoutUriEncodingFailure() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/api/v1/query", exchange -> {
      byte[] bytes = "{\"status\":\"success\",\"data\":{\"result\":[{\"value\":[0,\"25165824\"]}]}}"
          .getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(200, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    try {
      PlatformDashboardProperties properties = new PlatformDashboardProperties();
      properties.setPrometheusUrl("http://localhost:" + server.getAddress().getPort());
      PrometheusDatabaseUsageMetricsAdapter adapter = new PrometheusDatabaseUsageMetricsAdapter(properties);

      long bytes = adapter.databaseBytes(List.of("academy-a"));

      assertThat(bytes).isEqualTo(25165824L);
    } finally {
      server.stop(0);
    }
  }
}
