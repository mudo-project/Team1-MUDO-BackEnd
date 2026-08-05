package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubjectJpaRepository extends JpaRepository<SubjectEntity, Long> {

    Optional<SubjectEntity> findByAcademyIdAndName(Long academyId, String name);
}
