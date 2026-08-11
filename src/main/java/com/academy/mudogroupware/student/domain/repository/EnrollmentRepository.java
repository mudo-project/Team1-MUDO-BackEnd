package com.academy.mudogroupware.student.domain.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.academy.mudogroupware.student.domain.model.Enrollment;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    Optional<Enrollment> findByStudentIdAndLectureId(Long studentId, Long lectureId);

    Optional<Enrollment> findById(Long studentId, Long enrollmentId);

    List<Enrollment> findActiveByStudentId(Long studentId);

    Map<Long, Long> countActiveByStudentIds(List<Long> studentIds);

    List<Enrollment> findActiveByLectureId(Long lectureId);

    Map<Long, Long> countActiveByLectureIds(List<Long> lectureIds);
}
