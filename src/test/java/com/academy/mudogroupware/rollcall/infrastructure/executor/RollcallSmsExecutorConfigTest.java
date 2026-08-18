package com.academy.mudogroupware.rollcall.infrastructure.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class RollcallSmsExecutorConfigTest {

    @Test
    void rejectsImmediatelyWhenBoundedPoolAndQueueAreFull() throws Exception {
        RollcallSmsExecutorProperties properties = new RollcallSmsExecutorProperties();
        properties.setCorePoolSize(1);
        properties.setMaxPoolSize(1);
        properties.setQueueCapacity(1);
        properties.setAwaitTerminationSeconds(1);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ThreadPoolTaskExecutor executor =
                new RollcallSmsExecutorConfig().rollcallSmsExecutor(properties, registry);
        executor.initialize();

        CountDownLatch firstTaskStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstTask = new CountDownLatch(1);
        try {
            executor.execute(() -> {
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
            assertThat(registry.get("mudo.rollcall_sms.async.rejected").counter().count()).isEqualTo(1.0);
            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(executor.getMaxPoolSize()).isEqualTo(1);
            assertThat(executor.getQueueCapacity()).isEqualTo(1);
        } finally {
            releaseFirstTask.countDown();
            executor.shutdown();
        }
    }

    @Test
    void throwsWhenMaxPoolSizeIsSmallerThanCorePoolSize() {
        RollcallSmsExecutorProperties properties = new RollcallSmsExecutorProperties();
        properties.setCorePoolSize(4);
        properties.setMaxPoolSize(2);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        assertThatThrownBy(() -> new RollcallSmsExecutorConfig().rollcallSmsExecutor(properties, registry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("max-pool-size must be at least core-pool-size");
    }
}
