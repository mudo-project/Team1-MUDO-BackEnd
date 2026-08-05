package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;

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
@Table(name = "attendance_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceRecordJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "clock_in_at")
    private LocalDateTime clockInAt;

    @Column(name = "clock_in_note")
    private String clockInNote;

    @Column(name = "clock_out_at")
    private LocalDateTime clockOutAt;

    @Column(name = "clock_out_note")
    private String clockOutNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private AttendanceRecordJpaEntity(Long id, Long academyId, Long userId,
                                      LocalDate workDate, LocalDateTime clockInAt,
                                      String clockInNote, LocalDateTime clockOutAt,
                                      String clockOutNote, AttendanceStatus status,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.academyId = academyId;
        this.userId = userId;
        this.workDate = workDate;
        this.clockInAt = clockInAt;
        this.clockInNote = clockInNote;
        this.clockOutAt = clockOutAt;
        this.clockOutNote = clockOutNote;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
