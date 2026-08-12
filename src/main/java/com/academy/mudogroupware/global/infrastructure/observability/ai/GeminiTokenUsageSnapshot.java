package com.academy.mudogroupware.global.infrastructure.observability.ai;

// 기능(feature) 하나에 대한 누적 토큰 사용량 스냅샷.
public record GeminiTokenUsageSnapshot(
        String feature,
        long callCount,
        long promptTokens,
        long candidatesTokens,
        long totalTokens,
        double averageTotalTokensPerCall
) {
}
