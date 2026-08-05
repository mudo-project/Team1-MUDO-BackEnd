package com.academy.mudogroupware.student.domain.repository;

import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.student.domain.model.Enrollment;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findByStudentIdAndLectureId(Long academyId, Long studentId, Long lectureId);

    Optional<Enrollment> findById(Long academyId, Long studentId, Long enrollmentId);

    List<Enrollment> findActiveByStudentId(Long academyId, Long studentId);

    List<Enrollment> findActiveByLectureId(Long academyId, Long lectureId);
}
