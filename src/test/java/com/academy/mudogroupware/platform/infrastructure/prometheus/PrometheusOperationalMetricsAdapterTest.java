package com.academy.mudogroupware.platform.infrastructure.prometheus;

import static org.assertj.core.api.Assertions.assertThat;

import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.infrastructure.PlatformDashboardProperties;
import java.util.List;
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

  private AcademyRuntime academy(String code) {
    return new AcademyRuntime(code, "cluster", "service", "rds", 100, 0.7, "staff", "finance", "tenants/" + code + "/");
  }
}
