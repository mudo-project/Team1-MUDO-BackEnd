package com.academy.mudogroupware.platform.infrastructure;

import com.academy.mudogroupware.global.infrastructure.executor.MdcTaskDecorator;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(PlatformDashboardExecutorProperties.class)
public class PlatformDashboardExecutorConfig {

  @Bean(name = "platformDashboardExecutor")
  public ThreadPoolTaskExecutor platformDashboardExecutor(
      PlatformDashboardExecutorProperties properties, MeterRegistry meterRegistry) {
    if (properties.getMaxPoolSize() < properties.getCorePoolSize()) {
      throw new IllegalArgumentException(
          "platform.dashboard.executor.max-pool-size must be at least core-pool-size");
    }

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getCorePoolSize());
    executor.setMaxPoolSize(properties.getMaxPoolSize());
    executor.setQueueCapacity(properties.getQueueCapacity());
    executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
    executor.setThreadNamePrefix("mudo-platform-dashboard-");
    executor.setTaskDecorator(new MdcTaskDecorator());
    executor.setRejectedExecutionHandler(
        (task, threadPool) -> {
          meterRegistry.counter("mudo.platform_dashboard.async.rejected").increment();
          new ThreadPoolExecutor.AbortPolicy().rejectedExecution(task, threadPool);
        });
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
    return executor;
  }

  @Bean
  MeterBinder platformDashboardExecutorMetrics(
      @Qualifier("platformDashboardExecutor") ThreadPoolTaskExecutor executor,
      PlatformDashboardExecutorProperties properties) {
    return registry -> {
      Gauge.builder("mudo.platform_dashboard.async.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize)
          .description("Current platform dashboard async executor pool size")
          .register(registry);
      Gauge.builder("mudo.platform_dashboard.async.active", executor, ThreadPoolTaskExecutor::getActiveCount)
          .description("Active platform dashboard async tasks")
          .register(registry);
      Gauge.builder("mudo.platform_dashboard.async.queue.size", executor, ThreadPoolTaskExecutor::getQueueSize)
          .description("Queued platform dashboard async tasks")
          .register(registry);
      Gauge.builder("mudo.platform_dashboard.async.queue.capacity", properties,
              PlatformDashboardExecutorProperties::getQueueCapacity)
          .description("Configured platform dashboard async queue capacity")
          .register(registry);
    };
  }
}
