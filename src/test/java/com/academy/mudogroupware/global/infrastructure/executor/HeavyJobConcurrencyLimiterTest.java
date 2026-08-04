package com.academy.mudogroupware.global.infrastructure.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class HeavyJobConcurrencyLimiterTest {

  @Test
  void rejectsWorkBeyondConfiguredConcurrencyAndReleasesPermit() {
    HeavyJobProperties properties = new HeavyJobProperties();
    properties.setMaxConcurrency(1);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    HeavyJobConcurrencyLimiter limiter = new HeavyJobConcurrencyLimiter(properties, registry);

    try (HeavyJobConcurrencyLimiter.Permit ignored = limiter.acquire()) {
      assertThatThrownBy(limiter::acquire)
          .isInstanceOf(HeavyJobLimitExceededException.class)
          .hasMessageContaining("잠시 후");
      assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isEqualTo(1.0);
      assertThat(registry.get("mudo.heavy.job.rejected").counter().count()).isEqualTo(1.0);
    }

    try (HeavyJobConcurrencyLimiter.Permit ignored = limiter.acquire()) {
      assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isEqualTo(1.0);
    }
    assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isZero();
  }
}
