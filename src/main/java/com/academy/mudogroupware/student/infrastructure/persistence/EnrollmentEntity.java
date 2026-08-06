package com.academy.mudogroupware.student.infrastructure.persistence;

import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;

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
import lombok.Setter;

@Entity
@Table(name = "student_enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status;

    @Setter
    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @Setter
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Builder
    private EnrollmentEntity(Long id, Long academyId, Long studentId, Long lectureId, EnrollmentStatus status,
                             LocalDateTime enrolledAt, LocalDateTime endedAt) {
        this.id = id;
        this.academyId = academyId;
        this.studentId = studentId;
        this.lectureId = lectureId;
        this.status = status;
        this.enrolledAt = enrolledAt;
        this.endedAt = endedAt;
    }
}
