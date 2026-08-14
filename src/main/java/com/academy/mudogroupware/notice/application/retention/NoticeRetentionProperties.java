package com.academy.mudogroupware.notice.application.retention;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class NoticeRetentionProperties {

    private static final long DEFAULT_RECOVERY_DAYS = 30L;
    private static final int DEFAULT_BATCH_SIZE = 500;

    private final long recoveryDays;
    private final int batchSize;

    public NoticeRetentionProperties() {
        this(DEFAULT_RECOVERY_DAYS, DEFAULT_BATCH_SIZE);
    }

    public NoticeRetentionProperties(long recoveryDays, int batchSize) {
        if (recoveryDays <= 0) {
            throw new IllegalArgumentException("notice recoveryDays must be positive");
        }
        if (batchSize <= 0 || batchSize > 500) {
            throw new IllegalArgumentException("notice retention batchSize must be between 1 and 500");
        }
        this.recoveryDays = recoveryDays;
        this.batchSize = batchSize;
    }

    public long recoveryDays() {
        return recoveryDays;
    }

    public int batchSize() {
        return batchSize;
    }

    public LocalDateTime retentionUntil(LocalDateTime deletedAt) {
        if (deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null");
        }
        return deletedAt.plusDays(recoveryDays);
    }
}
