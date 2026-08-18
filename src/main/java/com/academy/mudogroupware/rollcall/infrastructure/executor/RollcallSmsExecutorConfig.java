package com.academy.mudogroupware.rollcall.infrastructure.executor;

import com.academy.mudogroupware.global.infrastructure.executor.MdcTaskDecorator;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableConfigurationProperties(RollcallSmsExecutorProperties.class)
public class RollcallSmsExecutorConfig {

  @Bean(name = "rollcallSmsExecutor")
  public ThreadPoolTaskExecutor rollcallSmsExecutor(
      RollcallSmsExecutorProperties properties, MeterRegistry meterRegistry) {
    if (properties.getMaxPoolSize() < properties.getCorePoolSize()) {
      throw new IllegalArgumentException(
          "rollcall.sms.executor.max-pool-size must be at least core-pool-size");
    }

    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(properties.getCorePoolSize());
    executor.setMaxPoolSize(properties.getMaxPoolSize());
    executor.setQueueCapacity(properties.getQueueCapacity());
    executor.setKeepAliveSeconds(properties.getKeepAliveSeconds());
    executor.setThreadNamePrefix("mudo-rollcall-sms-");
    executor.setTaskDecorator(new MdcTaskDecorator());
    executor.setRejectedExecutionHandler(
        (task, threadPool) -> {
          meterRegistry.counter("mudo.rollcall_sms.async.rejected").increment();
          new ThreadPoolExecutor.AbortPolicy().rejectedExecution(task, threadPool);
        });
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
    return executor;
  }

  @Bean
  MeterBinder rollcallSmsExecutorMetrics(
      @Qualifier("rollcallSmsExecutor") ThreadPoolTaskExecutor executor,
      RollcallSmsExecutorProperties properties) {
    return registry -> {
      Gauge.builder("mudo.rollcall_sms.async.pool.size", executor, ThreadPoolTaskExecutor::getPoolSize)
          .description("Current rollcall SMS async executor pool size")
          .register(registry);
      Gauge.builder("mudo.rollcall_sms.async.active", executor, ThreadPoolTaskExecutor::getActiveCount)
          .description("Active rollcall SMS async tasks")
          .register(registry);
      Gauge.builder("mudo.rollcall_sms.async.queue.size", executor, ThreadPoolTaskExecutor::getQueueSize)
          .description("Queued rollcall SMS async tasks")
          .register(registry);
      Gauge.builder("mudo.rollcall_sms.async.queue.capacity", properties,
              RollcallSmsExecutorProperties::getQueueCapacity)
          .description("Configured rollcall SMS async queue capacity")
          .register(registry);
    };
  }
}
