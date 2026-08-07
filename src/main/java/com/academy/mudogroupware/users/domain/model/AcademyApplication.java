package com.academy.mudogroupware.users.domain.model;

import java.time.LocalDateTime;

public final class AcademyApplication {

    private final Long id;
    private final String requestedLoginId;
    private final String academyName;
    private final String businessNo;
    private final String representativeName;
    private final String representativeEmail;
    private final String representativePhone;
    private final Long businessLicenseFileId;
    private final AcademyApplicationStatus status;
    private final String rejectReason;
    private final Long reviewedByUserId;
    private final LocalDateTime reviewedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private AcademyApplication(Long id, String requestedLoginId, String academyName, String businessNo,
                                String representativeName, String representativeEmail, String representativePhone,
                                Long businessLicenseFileId, AcademyApplicationStatus status, String rejectReason,
                                Long reviewedByUserId, LocalDateTime reviewedAt, LocalDateTime createdAt,
                                LocalDateTime updatedAt) {
        this.id = id;
        this.requestedLoginId = requestedLoginId;
        this.academyName = academyName;
        this.businessNo = businessNo;
        this.representativeName = representativeName;
        this.representativeEmail = representativeEmail;
        this.representativePhone = representativePhone;
        this.businessLicenseFileId = businessLicenseFileId;
        this.status = status;
        this.rejectReason = rejectReason;
        this.reviewedByUserId = reviewedByUserId;
        this.reviewedAt = reviewedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AcademyApplication restore(Long id, String requestedLoginId, String academyName,
                                              String businessNo, String representativeName,
                                              String representativeEmail, String representativePhone,
                                              Long businessLicenseFileId, AcademyApplicationStatus status,
                                              String rejectReason, Long reviewedByUserId, LocalDateTime reviewedAt,
                                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AcademyApplication(id, requestedLoginId, academyName, businessNo, representativeName,
                representativeEmail, representativePhone, businessLicenseFileId, status, rejectReason,
                reviewedByUserId, reviewedAt, createdAt, updatedAt);
    }

    public Long getId() {
        return id;
    }

    public String getRequestedLoginId() {
        return requestedLoginId;
    }

    public String getAcademyName() {
        return academyName;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public String getRepresentativeName() {
        return representativeName;
    }

    public String getRepresentativeEmail() {
        return representativeEmail;
    }

    public String getRepresentativePhone() {
        return representativePhone;
    }

    public Long getBusinessLicenseFileId() {
        return businessLicenseFileId;
    }

    public AcademyApplicationStatus getStatus() {
        return status;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public Long getReviewedByUserId() {
        return reviewedByUserId;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
