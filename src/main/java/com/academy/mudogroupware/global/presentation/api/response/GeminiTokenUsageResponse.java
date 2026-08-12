package com.academy.mudogroupware.global.presentation.api.response;

import com.academy.mudogroupware.global.infrastructure.observability.ai.GeminiTokenUsageSnapshot;

public record GeminiTokenUsageResponse(
        String feature,
        long callCount,
        long promptTokens,
        long candidatesTokens,
        long totalTokens,
        double averageTotalTokensPerCall
) {

    public static GeminiTokenUsageResponse from(GeminiTokenUsageSnapshot snapshot) {
        return new GeminiTokenUsageResponse(snapshot.feature(), snapshot.callCount(), snapshot.promptTokens(),
                snapshot.candidatesTokens(), snapshot.totalTokens(), snapshot.averageTotalTokensPerCall());
    }
}
