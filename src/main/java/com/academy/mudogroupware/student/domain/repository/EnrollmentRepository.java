package com.academy.mudogroupware.student.domain.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.academy.mudogroupware.student.domain.model.Enrollment;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findByStudentIdAndLectureId(Long academyId, Long studentId, Long lectureId);

    Optional<Enrollment> findById(Long academyId, Long studentId, Long enrollmentId);

    List<Enrollment> findActiveByStudentId(Long academyId, Long studentId);

    Map<Long, Long> countActiveByStudentIds(Long academyId, List<Long> studentIds);

    List<Enrollment> findActiveByLectureId(Long academyId, Long lectureId);

    Map<Long, Long> countActiveByLectureIds(Long academyId, List<Long> lectureIds);
}
