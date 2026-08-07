package com.academy.mudogroupware.users.domain.model;

import java.time.LocalDateTime;

public final class Academy {

    private final Long id;
    private final String name;
    private final String businessNo;
    private final Long userId;
    private final Long applicationId;
    private final AcademyStatus status;
    private final LocalDateTime createdAt;

    private Academy(Long id, String name, String businessNo, Long userId, Long applicationId,
                     AcademyStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.businessNo = businessNo;
        this.userId = userId;
        this.applicationId = applicationId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Academy create(String name, String businessNo, Long applicationId, LocalDateTime createdAt) {
        return new Academy(null, name, businessNo, null, applicationId, AcademyStatus.ACTIVE, createdAt);
    }

    public static Academy restore(Long id, String name, String businessNo, Long userId, Long applicationId,
                                   AcademyStatus status, LocalDateTime createdAt) {
        return new Academy(id, name, businessNo, userId, applicationId, status, createdAt);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public AcademyStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
