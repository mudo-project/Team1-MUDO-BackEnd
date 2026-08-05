package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.lecture.domain.model.Enrollment;
import com.academy.mudogroupware.lecture.domain.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepositoryImpl implements EnrollmentRepository {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Enrollment save(Enrollment enrollment) {
        EnrollmentEntity entity = EnrollmentEntity.builder()
                .studentId(enrollment.getStudentId())
                .lectureId(enrollment.getLectureId())
                .build();
        return toDomain(enrollmentJpaRepository.save(entity));
    }

    @Override
    public List<Enrollment> findByLectureId(Long lectureId) {
        return enrollmentJpaRepository.findAllByLectureId(lectureId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Enrollment> findByStudentId(Long studentId) {
        return enrollmentJpaRepository.findAllByStudentId(studentId).stream().map(this::toDomain).toList();
    }

    private Enrollment toDomain(EnrollmentEntity entity) {
        return Enrollment.restore(entity.getId(), entity.getStudentId(), entity.getLectureId(),
                entity.getCreatedAt());
    }
}
