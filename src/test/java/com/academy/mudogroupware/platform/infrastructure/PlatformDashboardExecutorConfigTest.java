package com.academy.mudogroupware.platform.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class PlatformDashboardExecutorConfigTest {

  @Test
  void defaultCapacityIsLargeEnoughForASingleOperationalMetricsFanOut() {
    PlatformDashboardExecutorProperties properties = new PlatformDashboardExecutorProperties();
    ThreadPoolTaskExecutor executor =
        new PlatformDashboardExecutorConfig().platformDashboardExecutor(properties, new SimpleMeterRegistry());
    executor.initialize();
    try {
      // operational-metrics 한 번 호출이 activeConnections/apiCallMetrics(11개 카테고리)/p95/errorRate/
      // ecsHeadrooms로 최대 16개까지 동시에 던진다 — 전용 실행기는 이걸 거절 없이 받아야 한다.
      int singleRequestFanOut = 16;
      assertThat(executor.getMaxPoolSize() + executor.getQueueCapacity())
          .isGreaterThanOrEqualTo(singleRequestFanOut);
    } finally {
      executor.shutdown();
    }
  }

  @Test
  void rejectsImmediatelyWhenBoundedPoolAndQueueAreFull() throws Exception {
    PlatformDashboardExecutorProperties properties = new PlatformDashboardExecutorProperties();
    properties.setCorePoolSize(1);
    properties.setMaxPoolSize(1);
    properties.setQueueCapacity(1);
    properties.setAwaitTerminationSeconds(1);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    ThreadPoolTaskExecutor executor =
        new PlatformDashboardExecutorConfig().platformDashboardExecutor(properties, registry);
    executor.initialize();

    CountDownLatch firstTaskStarted = new CountDownLatch(1);
    CountDownLatch releaseFirstTask = new CountDownLatch(1);
    try {
      executor.execute(
          () -> {
            firstTaskStarted.countDown();
            try {
              releaseFirstTask.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          });
      assertThat(firstTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();

      executor.execute(() -> {});

      assertThatThrownBy(() -> executor.execute(() -> {}))
          .isInstanceOf(TaskRejectedException.class);
      assertThat(registry.get("mudo.platform_dashboard.async.rejected").counter().count()).isEqualTo(1.0);
    } finally {
      releaseFirstTask.countDown();
      executor.shutdown();
    }
  }
}
