package com.academy.mudogroupware.rollcall.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.rollcall.domain.exception.EtcNoteRequiredException;

public final class AttendanceEntry {

    private final Long id;
    private final Long academyId;
    private final Long lectureId;
    private final Long studentId;
    private final LocalDate date;
    private AttendanceStatus status;
    private String note;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private AttendanceEntry(Long id, Long academyId, Long lectureId, Long studentId, LocalDate date,
                             AttendanceStatus status, String note, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
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
        if (status == AttendanceStatus.ETC && (note == null || note.isBlank())) {
            throw new EtcNoteRequiredException();
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("updatedAt must not be null");
        }
        this.id = id;
        this.academyId = academyId;
        this.lectureId = lectureId;
        this.studentId = studentId;
        this.date = date;
        this.status = status;
        this.note = status == AttendanceStatus.ETC ? note : null;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AttendanceEntry create(Long academyId, Long lectureId, Long studentId, LocalDate date,
                                          AttendanceStatus status, String note, LocalDateTime now) {
        return new AttendanceEntry(null, academyId, lectureId, studentId, date, status, note, now, now);
    }

    public static AttendanceEntry restore(Long id, Long academyId, Long lectureId, Long studentId, LocalDate date,
                                           AttendanceStatus status, String note, LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {
        return new AttendanceEntry(id, academyId, lectureId, studentId, date, status, note, createdAt, updatedAt);
    }

    public void changeStatus(AttendanceStatus status, String note, LocalDateTime now) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (status == AttendanceStatus.ETC && (note == null || note.isBlank())) {
            throw new EtcNoteRequiredException();
        }
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.status = status;
        this.note = status == AttendanceStatus.ETC ? note : null;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
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

    public AttendanceStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
