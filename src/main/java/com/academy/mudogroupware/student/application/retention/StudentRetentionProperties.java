package com.academy.mudogroupware.student.application.retention;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class StudentRetentionProperties {

    // 소프트 삭제 후 이 기간이 지나야 하드 삭제 대상이 된다. 삭제 취소 문의 등을 대응할
    // 유예 기간을 두기 위해 즉시 삭제하지 않는다.
    private static final long DEFAULT_PERIOD_DAYS = 30L;
    private static final int DEFAULT_BATCH_SIZE = 500;

    private final long periodDays;
    private final int batchSize;

    public StudentRetentionProperties() {
        this(DEFAULT_PERIOD_DAYS, DEFAULT_BATCH_SIZE);
    }

    public StudentRetentionProperties(long periodDays, int batchSize) {
        if (periodDays <= 0) {
            throw new IllegalArgumentException("Retention periodDays는 1 이상이어야 합니다.");
        }
        if (batchSize <= 0 || batchSize > 500) {
            throw new IllegalArgumentException("Retention batchSize는 1 이상 500 이하여야 합니다.");
        }
        this.periodDays = periodDays;
        this.batchSize = batchSize;
    }

    public long periodDays() {
        return periodDays;
    }

    public int batchSize() {
        return batchSize;
    }

    // 삭제 기준 시각 계산 역할
    public LocalDateTime threshold(LocalDateTime now) {
        return now.minusDays(periodDays);
    }
}
