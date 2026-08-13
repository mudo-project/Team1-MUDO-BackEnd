package com.academy.mudogroupware.global.infrastructure.observability.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.resourceusage.application.command.RecordAiTokenUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordSmsUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;

class GeminiTokenUsageTrackerTest {

    private final GeminiTokenUsageTracker tracker = new GeminiTokenUsageTracker();

    @Test
    void accumulatesCallsForTheSameFeature() {
        tracker.record("feature-a", 100, 20, 120);
        tracker.record("feature-a", 200, 30, 230);

        List<GeminiTokenUsageSnapshot> snapshot = tracker.snapshot();

        assertThat(snapshot).hasSize(1);
        GeminiTokenUsageSnapshot usage = snapshot.get(0);
        assertThat(usage.feature()).isEqualTo("feature-a");
        assertThat(usage.callCount()).isEqualTo(2);
        assertThat(usage.promptTokens()).isEqualTo(300);
        assertThat(usage.candidatesTokens()).isEqualTo(50);
        assertThat(usage.totalTokens()).isEqualTo(350);
        assertThat(usage.averageTotalTokensPerCall()).isEqualTo(175.0);
    }

    @Test
    void keepsSeparateTotalsPerFeatureSortedByName() {
        tracker.record("feature-b", 10, 5, 15);
        tracker.record("feature-a", 100, 20, 120);

        List<GeminiTokenUsageSnapshot> snapshot = tracker.snapshot();

        assertThat(snapshot).extracting(GeminiTokenUsageSnapshot::feature)
                .containsExactly("feature-a", "feature-b");
    }

    @Test
    void ignoresCallWithoutTotalTokenCount() {
        tracker.record("feature-a", 10, 5, null);

        assertThat(tracker.snapshot()).isEmpty();
    }

    @Test
    void treatsMissingPromptOrCandidatesCountsAsZero() {
        tracker.record("feature-a", null, null, 50);

        GeminiTokenUsageSnapshot usage = tracker.snapshot().get(0);
        assertThat(usage.promptTokens()).isZero();
        assertThat(usage.candidatesTokens()).isZero();
        assertThat(usage.totalTokens()).isEqualTo(50);
    }

    @Test
    void forwardsTokenUsageToPersistentRecorderWhenTotalTokenCountExists() {
        CapturingResourceUsageRecorder recorder = new CapturingResourceUsageRecorder();
        GeminiTokenUsageTracker persistentTracker = new GeminiTokenUsageTracker(recorder);

        persistentTracker.record("feature-a", "gemini-test", 100, 20, 120);

        assertThat(recorder.aiUsage).isNotNull();
        assertThat(recorder.aiUsage.feature()).isEqualTo("feature-a");
        assertThat(recorder.aiUsage.provider()).isEqualTo("GEMINI");
        assertThat(recorder.aiUsage.modelName()).isEqualTo("gemini-test");
        assertThat(recorder.aiUsage.promptTokens()).isEqualTo(100);
        assertThat(recorder.aiUsage.outputTokens()).isEqualTo(20);
        assertThat(recorder.aiUsage.totalTokens()).isEqualTo(120);
    }

    @Test
    void keepsInMemoryTrackingWhenPersistentRecorderFails() {
        GeminiTokenUsageTracker persistentTracker = new GeminiTokenUsageTracker(new ThrowingResourceUsageRecorder());

        assertThatCode(() -> persistentTracker.record("feature-a", "gemini-test", 100, 20, 120))
                .doesNotThrowAnyException();

        assertThat(persistentTracker.snapshot()).singleElement()
                .satisfies(usage -> {
                    assertThat(usage.feature()).isEqualTo("feature-a");
                    assertThat(usage.totalTokens()).isEqualTo(120);
                });
    }

    @Test
    void isSafeUnderConcurrentRecording() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        tracker.record("feature-a", 1, 1, 2);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        GeminiTokenUsageSnapshot usage = tracker.snapshot().get(0);
        assertThat(usage.callCount()).isEqualTo(threadCount);
        assertThat(usage.totalTokens()).isEqualTo(threadCount * 2L);
    }

    private static final class CapturingResourceUsageRecorder implements ResourceUsageRecorder {
        private RecordAiTokenUsageCommand aiUsage;

        @Override
        public void recordAiTokens(RecordAiTokenUsageCommand command) {
            this.aiUsage = command;
        }

        @Override
        public void recordSmsMessages(RecordSmsUsageCommand command) {
        }
    }

    private static final class ThrowingResourceUsageRecorder implements ResourceUsageRecorder {

        @Override
        public void recordAiTokens(RecordAiTokenUsageCommand command) {
            throw new IllegalStateException("usage store unavailable");
        }

        @Override
        public void recordSmsMessages(RecordSmsUsageCommand command) {
            throw new IllegalStateException("usage store unavailable");
        }
    }
}
