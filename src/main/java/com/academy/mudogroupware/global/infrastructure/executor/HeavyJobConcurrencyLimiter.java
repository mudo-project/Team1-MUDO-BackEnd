package com.academy.mudogroupware.global.infrastructure.executor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class HeavyJobConcurrencyLimiter {
  private final Semaphore semaphore;
  private final AtomicInteger activeJobs = new AtomicInteger();
  private final Counter rejectedJobs;

  public HeavyJobConcurrencyLimiter(HeavyJobProperties properties, MeterRegistry meterRegistry) {
    int maxConcurrency = properties.getMaxConcurrency();
    this.semaphore = new Semaphore(maxConcurrency, true);
    this.rejectedJobs =
        Counter.builder("mudo.heavy.job.rejected")
            .description("Heavy jobs rejected because the concurrency limit was reached")
            .register(meterRegistry);

    Gauge.builder("mudo.heavy.job.active", activeJobs, AtomicInteger::get)
        .description("Currently active heavy jobs")
        .register(meterRegistry);
    Gauge.builder("mudo.heavy.job.available", semaphore, Semaphore::availablePermits)
        .description("Available heavy-job concurrency permits")
        .register(meterRegistry);
  }

  public Permit acquire() {
    if (!semaphore.tryAcquire()) {
      rejectedJobs.increment();
      throw new HeavyJobLimitExceededException();
    }
    activeJobs.incrementAndGet();
    return new Permit(this);
  }

  public <T> T execute(Supplier<T> job) {
    try (Permit ignored = acquire()) {
      return job.get();
    }
  }

  public void execute(Runnable job) {
    try (Permit ignored = acquire()) {
      job.run();
    }
  }

  private void release() {
    activeJobs.decrementAndGet();
    semaphore.release();
  }

  public static final class Permit implements AutoCloseable {
    private final HeavyJobConcurrencyLimiter limiter;
    private final AtomicBoolean closed = new AtomicBoolean();

    private Permit(HeavyJobConcurrencyLimiter limiter) {
      this.limiter = limiter;
    }

    @Override
    public void close() {
      if (closed.compareAndSet(false, true)) {
        limiter.release();
      }
    }
  }
}
