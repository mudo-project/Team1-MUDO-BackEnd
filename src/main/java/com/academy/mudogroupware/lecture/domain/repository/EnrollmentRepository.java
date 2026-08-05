package com.academy.mudogroupware.lecture.domain.repository;

import java.util.List;

import com.academy.mudogroupware.lecture.domain.model.Enrollment;

public interface EnrollmentRepository {

    Enrollment save(Enrollment enrollment);

    List<Enrollment> findByLectureId(Long lectureId);

    List<Enrollment> findByStudentId(Long studentId);
}
