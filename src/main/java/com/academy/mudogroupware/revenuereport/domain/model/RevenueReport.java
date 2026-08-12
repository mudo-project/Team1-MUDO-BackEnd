package com.academy.mudogroupware.revenuereport.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class RevenueReport {

    private final Long id;
    private final LocalDate targetMonth;
    private final String report;
    private final String dataSnapshot;
    private LocalDateTime readAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private RevenueReport(Long id, LocalDate targetMonth, String report, String dataSnapshot,
                          LocalDateTime readAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.targetMonth = targetMonth;
        this.report = report;
        this.dataSnapshot = dataSnapshot;
        this.readAt = readAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static RevenueReport create(LocalDate targetMonth, String report, String dataSnapshot,
                                       LocalDateTime now) {
        return new RevenueReport(null, targetMonth, report, dataSnapshot, null, now, now);
    }

    public static RevenueReport restore(Long id, LocalDate targetMonth, String report, String dataSnapshot,
                                        LocalDateTime readAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new RevenueReport(id, targetMonth, report, dataSnapshot, readAt, createdAt, updatedAt);
    }

    public void markRead(LocalDateTime now) {
        if (readAt == null) {
            readAt = now;
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    public Long getId() { return id; }
    public LocalDate getTargetMonth() { return targetMonth; }
    public String getReport() { return report; }
    public String getDataSnapshot() { return dataSnapshot; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
