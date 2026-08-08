package com.academy.mudogroupware.workspace.application.query.comment;

public record TaskCommentSummary(
        Long taskId,
        long completedCount,
        long totalCount
) {}
