package com.academy.mudogroupware.global.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class GlobalRetentionSchedulerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 10, 3, 0);
    private final Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    @Test
    void runsEveryRegisteredJobWithTheSameNow() {
        RecordingJob jobA = new RecordingJob("a", false);
        RecordingJob jobB = new RecordingJob("b", false);
        GlobalRetentionScheduler scheduler = new GlobalRetentionScheduler(List.of(jobA, jobB), clock);

        scheduler.runRetentionJobs();

        assertThat(jobA.invokedWith).containsExactly(NOW);
        assertThat(jobB.invokedWith).containsExactly(NOW);
    }

    @Test
    void isolatesFailureSoLaterJobsStillRun() {
        RecordingJob failingJob = new RecordingJob("failing", true);
        RecordingJob healthyJob = new RecordingJob("healthy", false);
        GlobalRetentionScheduler scheduler = new GlobalRetentionScheduler(List.of(failingJob, healthyJob), clock);

        assertThatCode(scheduler::runRetentionJobs).doesNotThrowAnyException();

        assertThat(failingJob.invokedWith).hasSize(1);
        assertThat(healthyJob.invokedWith).hasSize(1);
    }

    @Test
    void doesNothingWhenNoJobsRegistered() {
        GlobalRetentionScheduler scheduler = new GlobalRetentionScheduler(List.of(), clock);

        assertThatCode(scheduler::runRetentionJobs).doesNotThrowAnyException();
    }

    private static final class RecordingJob implements RetentionJob {
        private final String name;
        private final boolean shouldFail;
        private final List<LocalDateTime> invokedWith = new ArrayList<>();

        private RecordingJob(String name, boolean shouldFail) {
            this.name = name;
            this.shouldFail = shouldFail;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public RetentionJobResult run(LocalDateTime now) {
            invokedWith.add(now);
            if (shouldFail) {
                throw new IllegalStateException("boom");
            }
            return RetentionJobResult.empty(name);
        }
    }
}
