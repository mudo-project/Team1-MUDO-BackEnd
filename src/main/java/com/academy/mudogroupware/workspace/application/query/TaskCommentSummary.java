package com.academy.mudogroupware.workspace.application.query;

public record TaskCommentSummary(
        Long taskId,
        long completedCount,
        long totalCount
) {}
