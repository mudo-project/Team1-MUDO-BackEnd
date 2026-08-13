package com.academy.mudogroupware.global.infrastructure.observability.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.academy.mudogroupware.resourceusage.application.command.RecordAiTokenUsageCommand;
import com.academy.mudogroupware.resourceusage.application.command.RecordSmsUsageCommand;
import com.academy.mudogroupware.resourceusage.application.port.ResourceUsageRecorder;

import lombok.extern.slf4j.Slf4j;

/**
 * Gemini 호출의 토큰 사용량을 기능(feature)별로 프로세스 메모리에 누적한다.
 * 로컬에서 "기능별로 토큰을 얼마나 쓰는지" 감을 잡기 위한 용도이며, 앱을 재시작하면 초기화된다.
 * 운영 과금 추적처럼 재시작 후에도 남아야 하는 요구가 생기면 DB 저장 방식으로 바꿔야 한다.
 */
@Slf4j
@Component
public class GeminiTokenUsageTracker {

    private final Map<String, FeatureUsage> usageByFeature = new ConcurrentHashMap<>();
    private final ResourceUsageRecorder resourceUsageRecorder;

    public GeminiTokenUsageTracker() {
        this(new NoopResourceUsageRecorder());
    }

    @Autowired
    public GeminiTokenUsageTracker(ObjectProvider<ResourceUsageRecorder> resourceUsageRecorderProvider) {
        this(resourceUsageRecorderProvider.getIfAvailable(NoopResourceUsageRecorder::new));
    }

    public GeminiTokenUsageTracker(ResourceUsageRecorder resourceUsageRecorder) {
        this.resourceUsageRecorder = resourceUsageRecorder;
    }

    /**
     * Gemini 응답의 usageMetadata를 기능별로 누적하고 로그를 남긴다.
     * usageMetadata 자체가 없거나(totalTokens == null) 응답이 비정상이면 집계하지 않고 경고만 남긴다.
     */
    public void record(String feature, Integer promptTokens, Integer candidatesTokens, Integer totalTokens) {
        record(feature, null, promptTokens, candidatesTokens, totalTokens);
    }

    public void record(String feature, String modelName, Integer promptTokens, Integer candidatesTokens,
                       Integer totalTokens) {
        if (totalTokens == null) {
            log.warn("event=gemini_token_usage_missing feature={}", feature);
            return;
        }

        FeatureUsage usage = usageByFeature.computeIfAbsent(feature, key -> new FeatureUsage());
        usage.add(nvl(promptTokens), nvl(candidatesTokens), totalTokens);

        log.info(
                "event=gemini_token_usage feature={} promptTokens={} candidatesTokens={} totalTokens={} "
                        + "cumulativeCallCount={} cumulativeTotalTokens={}",
                feature, nvl(promptTokens), nvl(candidatesTokens), totalTokens,
                usage.callCount.sum(), usage.totalTokens.sum());

        try {
            resourceUsageRecorder.recordAiTokens(new RecordAiTokenUsageCommand(
                    feature, "GEMINI", modelName, nvl(promptTokens), nvl(candidatesTokens), totalTokens));
        } catch (RuntimeException e) {
            log.warn("event=gemini_token_usage_persist_failed feature={} reason={}", feature, e.getMessage(), e);
        }
    }

    public List<GeminiTokenUsageSnapshot> snapshot() {
        return usageByFeature.entrySet().stream()
                .map(entry -> entry.getValue().toSnapshot(entry.getKey()))
                .sorted(Comparator.comparing(GeminiTokenUsageSnapshot::feature))
                .toList();
    }

    private int nvl(Integer value) {
        return value != null ? value : 0;
    }

    private static final class FeatureUsage {
        private final LongAdder callCount = new LongAdder();
        private final LongAdder promptTokens = new LongAdder();
        private final LongAdder candidatesTokens = new LongAdder();
        private final LongAdder totalTokens = new LongAdder();

        void add(int prompt, int candidates, int total) {
            callCount.increment();
            promptTokens.add(prompt);
            candidatesTokens.add(candidates);
            totalTokens.add(total);
        }

        GeminiTokenUsageSnapshot toSnapshot(String feature) {
            long calls = callCount.sum();
            long total = totalTokens.sum();
            double average = calls == 0 ? 0.0 : (double) total / calls;
            return new GeminiTokenUsageSnapshot(
                    feature, calls, promptTokens.sum(), candidatesTokens.sum(), total, average);
        }
    }

    private static final class NoopResourceUsageRecorder implements ResourceUsageRecorder {

        @Override
        public void recordAiTokens(RecordAiTokenUsageCommand command) {
        }

        @Override
        public void recordSmsMessages(RecordSmsUsageCommand command) {
        }
    }
}
