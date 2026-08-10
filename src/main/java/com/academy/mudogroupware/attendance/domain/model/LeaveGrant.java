package com.academy.mudogroupware.attendance.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class LeaveGrant {

    public static final int ANNUAL_GRANT_DAYS = 15;

    private final Long id;
    private final Long userId;
    private final LocalDate grantDate;
    private final LocalDate expirationDate;
    private final int grantedDays;
    private final LocalDateTime createdAt;

    private LeaveGrant(Long id, Long userId, LocalDate grantDate, LocalDate expirationDate,
                       int grantedDays, LocalDateTime createdAt) {
        if (userId == null || grantDate == null || expirationDate == null
                || expirationDate.isBefore(grantDate) || grantedDays <= 0 || createdAt == null) {
            throw new IllegalArgumentException("유효하지 않은 연차 지급 정보입니다.");
        }
        this.id = id;
        this.userId = userId;
        this.grantDate = grantDate;
        this.expirationDate = expirationDate;
        this.grantedDays = grantedDays;
        this.createdAt = createdAt;
    }

    public static LeaveGrant annual(Long userId, LocalDate grantDate, LocalDateTime createdAt) {
        return new LeaveGrant(null, userId, grantDate, grantDate.plusYears(1).minusDays(1),
                ANNUAL_GRANT_DAYS, createdAt);
    }

    public static LeaveGrant restore(Long id, Long userId, LocalDate grantDate,
                                     LocalDate expirationDate, int grantedDays, LocalDateTime createdAt) {
        return new LeaveGrant(id, userId, grantDate, expirationDate, grantedDays, createdAt);
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(grantDate) && !date.isAfter(expirationDate);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public LocalDate getGrantDate() { return grantDate; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public int getGrantedDays() { return grantedDays; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
