package com.academy.mudogroupware.resourceusage.application.command;

public record RecordAiTokenUsageCommand(
        String feature,
        String provider,
        String modelName,
        long promptTokens,
        long outputTokens,
        long totalTokens
) {
}
