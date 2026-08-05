package com.academy.mudogroupware.global.infrastructure.executor;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.async")
public class AsyncExecutionProperties {
  @Min(1)
  private int corePoolSize = 1;

  @Min(1)
  private int maxPoolSize = 2;

  @Min(0)
  private int queueCapacity = 20;

  @Min(0)
  private int keepAliveSeconds = 60;

  @Min(0)
  private int awaitTerminationSeconds = 30;
}
