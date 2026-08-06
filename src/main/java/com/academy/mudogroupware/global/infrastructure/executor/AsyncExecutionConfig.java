package com.academy.mudogroupware.global.infrastructure.executor;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties({AsyncExecutionProperties.class, HeavyJobProperties.class})
public class AsyncExecutionConfig {

  @Bean(name = "applicationTaskExecutor")
  public ThreadPoolTaskExecutor applicationTaskExecutor(
      AsyncExecutionProperties properties, MeterRegistry meterRegistry) {
    if (properties.getMaxPoolSize() < properties.getCorePoolSize()) {
      throw new IllegalArgumentException("app.async.max-pool-size must be at least core-pool-size");
    }

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getCorePoolSize());
    executor.setMaxPoolSize(properties.getMaxPoolSize());
    executor.setQueueCapacity(properties.getQueueCapacity());
    executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
    executor.setThreadNamePrefix("mudo-async-");
    executor.setTaskDecorator(new MdcTaskDecorator());
    executor.setRejectedExecutionHandler(
        (task, threadPool) -> {
          meterRegistry.counter("mudo.async.rejected").increment();
          new ThreadPoolExecutor.AbortPolicy().rejectedExecution(task, threadPool);
        });
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
    return executor;
  }

  @Bean
  MeterBinder asyncExecutorMetrics(
      @Qualifier("applicationTaskExecutor") ThreadPoolTaskExecutor executor,
      AsyncExecutionProperties properties) {
    return registry -> {
      Gauge.builder("mudo.async.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize)
          .description("Current application async executor pool size")
          .register(registry);
      Gauge.builder("mudo.async.active", executor, ThreadPoolTaskExecutor::getActiveCount)
          .description("Active application async tasks")
          .register(registry);
      Gauge.builder("mudo.async.queue.size", executor, ThreadPoolTaskExecutor::getQueueSize)
          .description("Queued application async tasks")
          .register(registry);
      Gauge.builder("mudo.async.queue.capacity", properties, AsyncExecutionProperties::getQueueCapacity)
          .description("Configured application async queue capacity")
          .register(registry);
    };
  }
}
