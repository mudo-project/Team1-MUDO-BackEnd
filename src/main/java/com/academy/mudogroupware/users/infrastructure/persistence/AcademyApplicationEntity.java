package com.academy.mudogroupware.users.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "academy_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcademyApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    @Column(name = "requested_login_id", nullable = false, length = 50)
    private String requestedLoginId;

    @Column(name = "academy_name", nullable = false, length = 100)
    private String academyName;

    @Column(name = "business_no", nullable = false, length = 20)
    private String businessNo;

    @Column(name = "representative_name", nullable = false, length = 50)
    private String representativeName;

    @Column(name = "representative_email", nullable = false, length = 100)
    private String representativeEmail;

    @Column(name = "representative_phone", nullable = false, length = 20)
    private String representativePhone;

    @Column(name = "business_license_file_id")
    private Long businessLicenseFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcademyApplicationStatus status;

    @Column(name = "reject_reason", length = 255)
    private String rejectReason;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    void markApproved(Long reviewerId, LocalDateTime reviewedAt) {
        this.status = AcademyApplicationStatus.APPROVED;
        this.reviewedByUserId = reviewerId;
        this.reviewedAt = reviewedAt;
        this.updatedAt = reviewedAt;
    }

    void markRejected(Long reviewerId, LocalDateTime reviewedAt, String reason) {
        this.status = AcademyApplicationStatus.REJECTED;
        this.reviewedByUserId = reviewerId;
        this.reviewedAt = reviewedAt;
        this.rejectReason = reason;
        this.updatedAt = reviewedAt;
    }
}
