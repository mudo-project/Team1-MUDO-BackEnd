package com.academy.mudogroupware.rollcall.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class AttendanceMessageSendRecord {

    private final Long id;
    private final Long lectureId;
    private final Long studentId;
    private final LocalDate date;
    private AttendanceMessageSendStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AttendanceMessageSendRecord(Long id, Long lectureId, Long studentId, LocalDate date,
                                         AttendanceMessageSendStatus status, LocalDateTime createdAt,
                                         LocalDateTime updatedAt) {
        if (lectureId == null) {
            throw new IllegalArgumentException("lectureId must not be null");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId must not be null");
        }
        if (date == null) {
            throw new IllegalArgumentException("date must not be null");
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
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AttendanceMessageSendRecord createPending(Long lectureId, Long studentId, LocalDate date,
                                                              LocalDateTime now) {
        return new AttendanceMessageSendRecord(null, lectureId, studentId, date,
                AttendanceMessageSendStatus.PENDING, now, now);
    }

    public static AttendanceMessageSendRecord restore(Long id, Long lectureId, Long studentId, LocalDate date,
                                                        AttendanceMessageSendStatus status, LocalDateTime createdAt,
                                                        LocalDateTime updatedAt) {
        return new AttendanceMessageSendRecord(id, lectureId, studentId, date, status, createdAt, updatedAt);
    }

    public void markResult(AttendanceMessageSendStatus status, LocalDateTime now) {
        if (status == null || status == AttendanceMessageSendStatus.PENDING) {
            throw new IllegalArgumentException("status must be a final send outcome");
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.status = status;
        this.updatedAt = now;
    }

    public boolean isAlreadySent() {
        return status == AttendanceMessageSendStatus.SENT;
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

    public AttendanceMessageSendStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
