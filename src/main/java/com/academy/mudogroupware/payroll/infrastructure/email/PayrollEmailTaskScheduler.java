package com.academy.mudogroupware.payroll.infrastructure.email;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.concurrent.ScheduledFuture;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
class PayrollEmailTaskScheduler {
  private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

  PayrollEmailTaskScheduler() {
    scheduler.setPoolSize(1);
    scheduler.setThreadNamePrefix("payroll-email-scheduler-");
    scheduler.setRemoveOnCancelPolicy(true);
  }

  @PostConstruct
  void initialize() {
    scheduler.initialize();
  }

  ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
    return scheduler.schedule(task, startTime);
  }

  @PreDestroy
  void shutdown() {
    scheduler.shutdown();
  }
}
