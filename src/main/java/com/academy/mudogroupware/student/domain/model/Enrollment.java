package com.academy.mudogroupware.student.domain.model;

import java.time.LocalDateTime;

import com.academy.mudogroupware.student.domain.exception.StudentErrorCode;
import com.academy.mudogroupware.student.domain.exception.StudentException;

public final class Enrollment {

    private final Long id;
    private final Long academyId;
    private final Long studentId;
    private final Long lectureId;
    private EnrollmentStatus status;
    private LocalDateTime enrolledAt;
    private LocalDateTime endedAt;

    private Enrollment(Long id, Long academyId, Long studentId, Long lectureId, EnrollmentStatus status,
                       LocalDateTime enrolledAt, LocalDateTime endedAt) {
        if (academyId == null) {
            throw new IllegalArgumentException("academyId must not be null");
        }
        if (studentId == null) {
            throw new IllegalArgumentException("studentId must not be null");
        }
        if (lectureId == null) {
            throw new StudentException(StudentErrorCode.LECTURE_REQUIRED);
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (enrolledAt == null) {
            throw new IllegalArgumentException("enrolledAt must not be null");
        }
        this.id = id;
        this.academyId = academyId;
        this.studentId = studentId;
        this.lectureId = lectureId;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.endedAt = endedAt;
    }

    public static Enrollment create(Long academyId, Long studentId, Long lectureId, LocalDateTime enrolledAt) {
        return new Enrollment(null, academyId, studentId, lectureId, EnrollmentStatus.ACTIVE, enrolledAt, null);
    }

    public static Enrollment restore(Long id, Long academyId, Long studentId, Long lectureId,
                                     EnrollmentStatus status, LocalDateTime enrolledAt, LocalDateTime endedAt) {
        return new Enrollment(id, academyId, studentId, lectureId, status, enrolledAt, endedAt);
    }

    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }

    public void reactivate(LocalDateTime now) {
        if (now == null) {
            throw new IllegalArgumentException("now must not be null");
        }
        this.status = EnrollmentStatus.ACTIVE;
        this.enrolledAt = now;
        this.endedAt = null;
    }

    public void end(LocalDateTime endedAt) {
        if (endedAt == null) {
            throw new IllegalArgumentException("endedAt must not be null");
        }
        this.status = EnrollmentStatus.ENDED;
        this.endedAt = endedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getAcademyId() {
        return academyId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getLectureId() {
        return lectureId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }
}
