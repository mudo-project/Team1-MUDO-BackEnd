package com.academy.mudogroupware.platform.infrastructure.prometheus;

import com.academy.mudogroupware.platform.application.port.ActiveMemberCountPort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveMemberGauge {
  private final MeterRegistry meterRegistry;
  private final ActiveMemberCountPort activeMemberCountPort;

  @PostConstruct
  void register() {
    Gauge.builder("mudo_active_members", activeMemberCountPort, port -> port.countActiveMembers())
        .description("Current tenant active member count")
        .register(meterRegistry);
  }
}
