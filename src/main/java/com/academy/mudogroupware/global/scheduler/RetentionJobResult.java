package com.academy.mudogroupware.global.scheduler;

public record RetentionJobResult(
        String jobName,
        int candidateCount,
        int deletedChildCount,
        int deletedParentCount
) {

    public static RetentionJobResult empty(String jobName) {
        return new RetentionJobResult(jobName, 0, 0, 0);
    }
}
