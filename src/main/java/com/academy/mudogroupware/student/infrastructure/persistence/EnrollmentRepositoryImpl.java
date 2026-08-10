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
    public Optional<Enrollment> findByStudentIdAndLectureId(Long academyId, Long studentId, Long lectureId) {
        return enrollmentJpaRepository.findByAcademyIdAndStudentIdAndLectureId(academyId, studentId, lectureId)
                .map(this::toDomain);
    }

    @Override
    public Optional<Enrollment> findById(Long academyId, Long studentId, Long enrollmentId) {
        return enrollmentJpaRepository.findByAcademyIdAndStudentIdAndId(academyId, studentId, enrollmentId)
                .map(this::toDomain);
    }

    @Override
    public List<Enrollment> findActiveByStudentId(Long academyId, Long studentId) {
        return enrollmentJpaRepository.findAllByAcademyIdAndStudentIdAndStatusOrderByEnrolledAtDesc(
                        academyId, studentId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Long> countActiveByStudentIds(Long academyId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Map.of();
        }
        return enrollmentJpaRepository.countByStudentIdsAndStatus(academyId, studentIds, EnrollmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        EnrollmentJpaRepository.StudentEnrollmentCount::getStudentId,
                        EnrollmentJpaRepository.StudentEnrollmentCount::getCount
                ));
    }

    @Override
    public List<Enrollment> findActiveByLectureId(Long academyId, Long lectureId) {
        return enrollmentJpaRepository.findAllByAcademyIdAndLectureIdAndStatus(
                        academyId, lectureId, EnrollmentStatus.ACTIVE)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Map<Long, Long> countActiveByLectureIds(Long academyId, List<Long> lectureIds) {
        if (lectureIds == null || lectureIds.isEmpty()) {
            return Map.of();
        }
        return enrollmentJpaRepository.countByLectureIdsAndStatus(academyId, lectureIds, EnrollmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        EnrollmentJpaRepository.LectureEnrollmentCount::getLectureId,
                        EnrollmentJpaRepository.LectureEnrollmentCount::getCount
                ));
    }

    private EnrollmentEntity toNewEntity(Enrollment enrollment) {
        return EnrollmentEntity.builder()
                .academyId(enrollment.getAcademyId())
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
        return Enrollment.restore(entity.getId(), entity.getAcademyId(), entity.getStudentId(), entity.getLectureId(),
                entity.getStatus(), entity.getEnrolledAt(), entity.getEndedAt());
    }
}
