package com.academy.mudogroupware.global.infrastructure.executor;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.heavy-job")
public class HeavyJobProperties {
  @Min(1)
  private int maxConcurrency = 1;
}
