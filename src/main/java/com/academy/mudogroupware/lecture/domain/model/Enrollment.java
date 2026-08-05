package com.academy.mudogroupware.lecture.domain.model;

import java.time.LocalDateTime;

public final class Enrollment {

    private final Long id;
    private final Long studentId;
    private final Long lectureId;
    private final LocalDateTime enrolledAt;

    private Enrollment(Long id, Long studentId, Long lectureId, LocalDateTime enrolledAt) {
        if (studentId == null) {
            throw new IllegalArgumentException("studentId must not be null");
        }
        if (lectureId == null) {
            throw new IllegalArgumentException("lectureId must not be null");
        }
        if (enrolledAt == null) {
            throw new IllegalArgumentException("enrolledAt must not be null");
        }
        this.id = id;
        this.studentId = studentId;
        this.lectureId = lectureId;
        this.enrolledAt = enrolledAt;
    }

    public static Enrollment create(Long studentId, Long lectureId, LocalDateTime now) {
        return new Enrollment(null, studentId, lectureId, now);
    }

    public static Enrollment restore(Long id, Long studentId, Long lectureId, LocalDateTime enrolledAt) {
        return new Enrollment(id, studentId, lectureId, enrolledAt);
    }

    public Long getId() {
        return id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getLectureId() {
        return lectureId;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }
}
