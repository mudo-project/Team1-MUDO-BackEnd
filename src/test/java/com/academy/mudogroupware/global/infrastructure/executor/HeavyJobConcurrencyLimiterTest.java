package com.academy.mudogroupware.global.infrastructure.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class HeavyJobConcurrencyLimiterTest {

  @Test
  void rejectsWorkBeyondConfiguredConcurrencyAndReleasesPermit() {
    HeavyJobProperties properties = new HeavyJobProperties();
    properties.setMaxConcurrency(1);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    HeavyJobConcurrencyLimiter limiter = new HeavyJobConcurrencyLimiter(properties, registry);

    try (HeavyJobConcurrencyLimiter.Permit ignored = limiter.acquire()) {
      assertThatThrownBy(limiter::acquire)
          .isInstanceOf(HeavyJobLimitExceededException.class)
          .hasMessageContaining("잠시 후");
      assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isEqualTo(1.0);
      assertThat(registry.get("mudo.heavy.job.rejected").counter().count()).isEqualTo(1.0);
    }

    try (HeavyJobConcurrencyLimiter.Permit ignored = limiter.acquire()) {
      assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isEqualTo(1.0);
    }
    assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isZero();
  }

  @Test
  void releasesPermitWhenExecutedJobThrows() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    HeavyJobConcurrencyLimiter limiter = limiter(1, registry);

    assertThatThrownBy(
            () ->
                limiter.execute(
                    () -> {
                      throw new IllegalStateException("boom");
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isZero();
    assertThat(registry.get("mudo.heavy.job.available").gauge().value()).isEqualTo(1.0);
    assertThat(limiter.execute(() -> "next")).isEqualTo("next");
  }

  @Test
  void rejectsOnlyWorkBeyondConcurrentLimit() throws Exception {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    HeavyJobConcurrencyLimiter limiter = limiter(2, registry);
    CountDownLatch acceptedJobsStarted = new CountDownLatch(2);
    CountDownLatch releaseAcceptedJobs = new CountDownLatch(1);
    AtomicInteger completedJobs = new AtomicInteger();
    AtomicInteger rejectedJobs = new AtomicInteger();
    ExecutorService executor = Executors.newFixedThreadPool(3);

    Runnable task =
        () -> {
          try {
            limiter.execute(
                () -> {
                  acceptedJobsStarted.countDown();
                  await(releaseAcceptedJobs);
                });
            completedJobs.incrementAndGet();
          } catch (HeavyJobLimitExceededException exception) {
            rejectedJobs.incrementAndGet();
          }
        };

    try {
      Future<?> first = executor.submit(task);
      Future<?> second = executor.submit(task);
      assertThat(acceptedJobsStarted.await(5, TimeUnit.SECONDS)).isTrue();

      Future<?> excess = executor.submit(task);
      excess.get(5, TimeUnit.SECONDS);

      assertThat(rejectedJobs).hasValue(1);
      assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isEqualTo(2.0);
      assertThat(registry.get("mudo.heavy.job.rejected").counter().count()).isEqualTo(1.0);

      releaseAcceptedJobs.countDown();
      first.get(5, TimeUnit.SECONDS);
      second.get(5, TimeUnit.SECONDS);

      assertThat(completedJobs).hasValue(2);
      assertThat(registry.get("mudo.heavy.job.active").gauge().value()).isZero();
      assertThat(registry.get("mudo.heavy.job.available").gauge().value()).isEqualTo(2.0);
    } finally {
      releaseAcceptedJobs.countDown();
      executor.shutdownNow();
    }
  }

  private HeavyJobConcurrencyLimiter limiter(
      int maxConcurrency, SimpleMeterRegistry registry) {
    HeavyJobProperties properties = new HeavyJobProperties();
    properties.setMaxConcurrency(maxConcurrency);
    return new HeavyJobConcurrencyLimiter(properties, registry);
  }

  private void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for concurrent test release");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Concurrent test was interrupted", exception);
    }
  }
}
