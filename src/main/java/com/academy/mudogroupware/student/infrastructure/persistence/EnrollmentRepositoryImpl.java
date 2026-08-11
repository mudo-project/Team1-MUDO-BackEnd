package com.academy.mudogroupware.student.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.academy.mudogroupware.student.domain.model.Enrollment;
import com.academy.mudogroupware.student.domain.model.EnrollmentStatus;
import com.academy.mudogroupware.student.domain.repository.EnrollmentRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class EnrollmentRepositoryImpl implements EnrollmentRepository {

    private final EnrollmentJpaRepository enrollmentJpaRepository;

    @Override
    public Enrollment save(Enrollment enrollment) {
        EnrollmentEntity entity = enrollment.getId() == null ? toNewEntity(enrollment) : updateExisting(enrollment);
        return toDomain(enrollmentJpaRepository.save(entity));
    }

    @Override
    public Optional<Enrollment> findByStudentIdAndLectureId(Long studentId, Long lectureId) {
        return enrollmentJpaRepository.findByStudentIdAndLectureId(studentId, lectureId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Enrollment> findById(Long studentId, Long enrollmentId) {
        return enrollmentJpaRepository.findByStudentIdAndId(studentId, enrollmentId)
                .map(this::toDomain);
    }

    @Override
    public List<Enrollment> findActiveByStudentId(Long studentId) {
        return enrollmentJpaRepository.findAllByStudentIdAndStatusOrderByEnrolledAtDesc(
                        studentId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Long> countActiveByStudentIds(List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        return enrollmentJpaRepository.countByStudentIdsAndStatus(studentIds, EnrollmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        EnrollmentJpaRepository.StudentEnrollmentCount::getStudentId,
                        EnrollmentJpaRepository.StudentEnrollmentCount::getCount
                ));
    }

    @Override
    public List<Enrollment> findActiveByLectureId(Long lectureId) {
        return enrollmentJpaRepository.findAllByLectureIdAndStatus(
                        lectureId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Long> countActiveByLectureIds(List<Long> lectureIds) {
        if (lectureIds == null || lectureIds.isEmpty()) {
            return Map.of();
        }
        return enrollmentJpaRepository.countByLectureIdsAndStatus(lectureIds, EnrollmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        EnrollmentJpaRepository.LectureEnrollmentCount::getLectureId,
                        EnrollmentJpaRepository.LectureEnrollmentCount::getCount
                ));
    }

    private EnrollmentEntity toNewEntity(Enrollment enrollment) {
        return EnrollmentEntity.builder()
                .studentId(enrollment.getStudentId())
                .lectureId(enrollment.getLectureId())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .endedAt(enrollment.getEndedAt())
                .build();
    }

    private EnrollmentEntity updateExisting(Enrollment enrollment) {
        EnrollmentEntity entity = enrollmentJpaRepository.getReferenceById(enrollment.getId());
        entity.setStatus(enrollment.getStatus());
        entity.setEnrolledAt(enrollment.getEnrolledAt());
        entity.setEndedAt(enrollment.getEndedAt());
        return entity;
    }

    private Enrollment toDomain(EnrollmentEntity entity) {
        return Enrollment.restore(entity.getId(), entity.getStudentId(), entity.getLectureId(),
                entity.getStatus(), entity.getEnrolledAt(), entity.getEndedAt());
    }
}
