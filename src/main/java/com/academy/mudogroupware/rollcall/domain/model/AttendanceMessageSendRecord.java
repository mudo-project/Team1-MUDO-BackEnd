package com.academy.mudogroupware.rollcall.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AttendanceMessageSendRecord {

    private final Long id;
    private final Long lectureId;
    private final Long studentId;
    private final LocalDate date;
    private final AttendanceStatus attendanceStatus;
    private AttendanceMessageSendStatus status;
    private String failureReason;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AttendanceMessageSendRecord(Long id, Long lectureId, Long studentId, LocalDate date,
                                         AttendanceStatus attendanceStatus, AttendanceMessageSendStatus status,
                                         String failureReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (lectureId == null) {
            throw new IllegalArgumentException("lectureId must not be null");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId must not be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
        }
        if (attendanceStatus == null) {
            throw new IllegalArgumentException("attendanceStatus must not be null");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
        this.id = id;
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.date = date;
        this.attendanceStatus = attendanceStatus;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AttendanceMessageSendRecord createPending(Long lectureId, Long studentId, LocalDate date,
                                                              AttendanceStatus attendanceStatus, LocalDateTime now) {
        return new AttendanceMessageSendRecord(null, lectureId, studentId, date, attendanceStatus,
                AttendanceMessageSendStatus.PENDING, null, now, now);
    }

    public static AttendanceMessageSendRecord restore(Long id, Long lectureId, Long studentId, LocalDate date,
                                                        AttendanceStatus attendanceStatus,
                                                        AttendanceMessageSendStatus status, String failureReason,
                                                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new AttendanceMessageSendRecord(id, lectureId, studentId, date, attendanceStatus, status,
                failureReason, createdAt, updatedAt);
    }

    public void markResult(AttendanceMessageSendStatus status, String failureReason, LocalDateTime now) {
        if (status == null || status == AttendanceMessageSendStatus.PENDING
                || status == AttendanceMessageSendStatus.SENDING) {
            throw new IllegalArgumentException("status must be a final send outcome");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.status = status;
        this.failureReason = status == AttendanceMessageSendStatus.SENT ? null : failureReason;
        this.updatedAt = now;
    }

    public boolean isAlreadySent() {
        return status == AttendanceMessageSendStatus.SENT;
    }

    public boolean isIndeterminate() {
        return status == AttendanceMessageSendStatus.INDETERMINATE;
    }

    public Long getId() {
        return id;
    }

    public Long getLectureId() {
        return lectureId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public LocalDate getDate() {
        return date;
    }

    public AttendanceStatus getAttendanceStatus() {
        return attendanceStatus;
    }

    public AttendanceMessageSendStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
