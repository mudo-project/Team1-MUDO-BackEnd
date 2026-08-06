package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.model.LeaveRequestStatus;

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
@Table(name = "leave_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveRequestJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_request_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "used_days", nullable = false)
    private int usedDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LeaveRequestStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private LeaveRequestJpaEntity(Long id, Long academyId, Long userId, Long documentId, LocalDate startDate,
                                  LocalDate endDate, int usedDays, LeaveRequestStatus status, LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        this.id = id;
        this.academyId = academyId;
        this.userId = userId;
        this.documentId = documentId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.usedDays = usedDays;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
