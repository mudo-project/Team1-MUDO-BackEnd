package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "leave_grant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveGrantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "leave_grant_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "grant_date", nullable = false)
    private LocalDate grantDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Column(name = "granted_days", nullable = false)
    private int grantedDays;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    private LeaveGrantJpaEntity(Long id, Long academyId, Long userId, LocalDate grantDate,
                                LocalDate expirationDate, int grantedDays, LocalDateTime createdAt) {
        this.id = id;
        this.academyId = academyId;
        this.userId = userId;
        this.grantDate = grantDate;
        this.expirationDate = expirationDate;
        this.grantedDays = grantedDays;
        this.createdAt = createdAt;
    }
}
