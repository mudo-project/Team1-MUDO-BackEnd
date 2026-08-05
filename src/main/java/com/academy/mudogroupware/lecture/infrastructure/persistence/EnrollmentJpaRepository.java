package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentJpaRepository extends JpaRepository<EnrollmentEntity, Long> {

    List<EnrollmentEntity> findAllByLectureId(Long lectureId);

    List<EnrollmentEntity> findAllByStudentId(Long studentId);
}
