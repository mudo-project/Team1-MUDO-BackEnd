package com.academy.mudogroupware.platform.infrastructure;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.dashboard")
public class PlatformDashboardProperties {
  @NotBlank private String prometheusUrl = "http://monitoring.mudo.internal:9090";

  @NotBlank private String tenantRegistryJson = "[]";

  private int prometheusConnectTimeoutMs = 2000;

  private int prometheusReadTimeoutMs = 5000;
}
