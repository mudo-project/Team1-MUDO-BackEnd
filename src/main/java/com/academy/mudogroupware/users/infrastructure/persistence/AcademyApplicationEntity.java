package com.academy.mudogroupware.users.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;
import com.academy.mudogroupware.users.domain.model.Plan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
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

    @Column(name = "business_no", length = 20)
    private String businessNo;

    @Column(name = "representative_name", nullable = false, length = 50)
    private String representativeName;

    @Column(name = "representative_email", nullable = false, length = 100)
    private String representativeEmail;

    @Column(name = "representative_phone", nullable = false, length = 20)
    private String representativePhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Plan plan;

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

    @Builder
    private AcademyApplicationEntity(String requestedLoginId, String academyName, Plan plan,
                                      String representativeName, String representativeEmail,
                                      String representativePhone, AcademyApplicationStatus status,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.requestedLoginId = requestedLoginId;
        this.academyName = academyName;
        this.plan = plan;
        this.representativeName = representativeName;
        this.representativeEmail = representativeEmail;
        this.representativePhone = representativePhone;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    void markRejected(Long reviewerId, LocalDateTime reviewedAt, String reason) {
        this.status = AcademyApplicationStatus.REJECTED;
        this.reviewedByUserId = reviewerId;
        this.reviewedAt = reviewedAt;
        this.rejectReason = reason;
        this.updatedAt = reviewedAt;
    }
}
