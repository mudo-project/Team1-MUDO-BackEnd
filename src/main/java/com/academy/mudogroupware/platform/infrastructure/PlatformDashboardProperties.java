package com.academy.mudogroupware.platform.infrastructure;

import jakarta.validation.constraints.Min;
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

  @NotBlank private String tenantDirectoryJson = "[]";

  // HttpURLConnection 계열은 타임아웃 0을 "무한 대기"로 해석한다 — 0/음수를 막아
  // 설정 실수로 타임아웃이 조용히 사라지는 걸 방지한다.
  @Min(1) private int prometheusConnectTimeoutMs = 2000;

  @Min(1) private int prometheusReadTimeoutMs = 5000;
}
