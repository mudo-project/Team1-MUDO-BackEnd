package com.academy.mudogroupware.platform.infrastructure;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform.dashboard.executor")
public class PlatformDashboardExecutorProperties {
  // operational-metrics 한 번 호출이 activeConnections/apiCallMetrics(11개 카테고리)/p95/errorRate/
  // ecsHeadrooms로 최대 16개까지 동시에 비동기 작업을 던진다. 업무용 공유 실행기(applicationTaskExecutor,
  // core 1/max 2/queue 20)를 같이 쓰면 대시보드 팬아웃이 다른 기능의 비동기 작업과 서로 밀어내며
  // TaskRejectedException을 낸다 - 전용 실행기로 분리해 서로 영향을 주지 않게 한다.
  @Min(1) private int corePoolSize = 4;

  @Min(1) private int maxPoolSize = 8;

  @Min(0) private int queueCapacity = 50;

  @Min(0) private int keepAliveSeconds = 60;

  @Min(0) private int awaitTerminationSeconds = 30;
}
