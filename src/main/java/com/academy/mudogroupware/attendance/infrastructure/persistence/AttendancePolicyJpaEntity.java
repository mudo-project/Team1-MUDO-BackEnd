package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDateTime;
import java.time.LocalTime;

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
@Table(name = "attendance_policies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendancePolicyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Column(name = "default_start_time", nullable = false)
    private LocalTime defaultStartTime;

    @Column(name = "default_end_time", nullable = false)
    private LocalTime defaultEndTime;

    @Column(name = "late_grace_minutes", nullable = false)
    private int lateGraceMinutes;

    @Column(name = "weekday_exception_enabled", nullable = false)
    private boolean weekdayExceptionEnabled;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private AttendancePolicyJpaEntity(Long id,
                                      LocalTime defaultStartTime, LocalTime defaultEndTime,
                                      int lateGraceMinutes, boolean weekdayExceptionEnabled,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.defaultStartTime = defaultStartTime;
        this.defaultEndTime = defaultEndTime;
        this.lateGraceMinutes = lateGraceMinutes;
        this.weekdayExceptionEnabled = weekdayExceptionEnabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
