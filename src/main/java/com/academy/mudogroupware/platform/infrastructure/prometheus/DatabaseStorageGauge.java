package com.academy.mudogroupware.platform.infrastructure.prometheus;

import com.academy.mudogroupware.platform.application.port.CurrentTenantDatabaseUsagePort;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatabaseStorageGauge {
  private final MeterRegistry meterRegistry;
  private final CurrentTenantDatabaseUsagePort currentTenantDatabaseUsagePort;

  @PostConstruct
  void register() {
    Gauge.builder("mudo_database_storage_bytes", currentTenantDatabaseUsagePort,
            port -> port.databaseBytes())
        .description("Current tenant database data and index bytes")
        .register(meterRegistry);
  }
}
