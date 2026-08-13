package com.academy.mudogroupware.platform.infrastructure.prometheus;

import com.academy.mudogroupware.platform.application.port.MemberCountMetricsPort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.infrastructure.PlatformDashboardProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class PrometheusMemberCountMetricsAdapter implements MemberCountMetricsPort {
  private final PlatformDashboardProperties properties;

  @Override
  public long activeMemberCount(Collection<String> academyCodes) {
    String tenantMatcher = academyCodes.isEmpty() ? ".*" : String.join("|", academyCodes);
    String query = "sum(mudo_active_members{tenant=~\"%s\"})".formatted(tenantMatcher);
    try {
      JsonNode root = RestClient.create(properties.getPrometheusUrl())
          .get().uri(uri -> uri.path("/api/v1/query").queryParam("query", query).build())
          .retrieve().body(JsonNode.class);
      JsonNode result = root == null ? null : root.path("data").path("result");
      if (result == null || !result.isArray() || result.isEmpty()) return 0;
      return Math.round(result.get(0).path("value").get(1).asDouble(0));
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }
}
