package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceMessageSendStatus;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attendance_message_send_record", uniqueConstraints = @UniqueConstraint(
        name = "uk_attendance_message_send_record_lecture_student_date",
        columnNames = {"lecture_id", "student_id", "entry_date", "attendance_status"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceMessageSendRecordEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long id;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "entry_date", nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_status", nullable = false, length = 20)
    private AttendanceStatus attendanceStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceMessageSendStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Builder
    private AttendanceMessageSendRecordEntity(Long lectureId, Long studentId, LocalDate date,
                                               AttendanceStatus attendanceStatus, AttendanceMessageSendStatus status) {
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.date = date;
        this.attendanceStatus = attendanceStatus;
        this.status = status;
    }

    public void changeStatus(AttendanceMessageSendStatus status, String failureReason) {
        this.status = status;
        this.failureReason = failureReason;
    }
}
