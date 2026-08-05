package com.academy.mudogroupware.lecture.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;

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
@Table(name = "enrollment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enrollment_id")
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    @Builder
    private EnrollmentEntity(Long studentId, Long lectureId) {
        this.studentId = studentId;
        this.lectureId = lectureId;
    }
}
