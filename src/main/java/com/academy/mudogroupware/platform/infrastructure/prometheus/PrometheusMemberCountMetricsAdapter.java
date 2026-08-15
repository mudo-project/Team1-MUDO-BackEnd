package com.academy.mudogroupware.platform.infrastructure.prometheus;

import com.academy.mudogroupware.platform.application.port.MemberCountMetricsPort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.infrastructure.PlatformDashboardProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
public class PrometheusMemberCountMetricsAdapter implements MemberCountMetricsPort {
  private final PlatformDashboardProperties properties;
  private final RestClient restClient;

  public PrometheusMemberCountMetricsAdapter(PlatformDashboardProperties properties) {
    this.properties = properties;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(Duration.ofMillis(properties.getPrometheusConnectTimeoutMs()));
    requestFactory.setReadTimeout(Duration.ofMillis(properties.getPrometheusReadTimeoutMs()));
    this.restClient = RestClient.builder().requestFactory(requestFactory).build();
  }

  @Override
  public long activeMemberCount(Collection<String> academyCodes) {
    String tenantMatcher = academyCodes.isEmpty() ? ".*" : String.join("|", academyCodes);
    String query = "sum(mudo_active_members{tenant=~\"%s\"})".formatted(tenantMatcher);
    try {
      // PromQL의 '{'/'}'는 UriComponentsBuilder(RestClient의 uri(UriBuilder) 포함)가 URI 템플릿
      // 변수 문법으로 취급해 인코딩하지 않는다 — URLEncoder로 직접 percent-encode한다.
      String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
      URI uri = URI.create(properties.getPrometheusUrl() + "/api/v1/query?query=" + encodedQuery);
      JsonNode root = restClient.get().uri(uri).retrieve().body(JsonNode.class);
      JsonNode result = root == null ? null : root.path("data").path("result");
      if (result == null || !result.isArray() || result.isEmpty()) return 0;
      return Math.round(result.get(0).path("value").get(1).asDouble(0));
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }
}
