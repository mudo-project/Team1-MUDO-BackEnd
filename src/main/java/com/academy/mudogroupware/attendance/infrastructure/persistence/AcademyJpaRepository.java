package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademyJpaRepository extends JpaRepository<AcademyJpaEntity, Long> {
    Optional<AcademyJpaEntity> findByUserId(Long userId);
}
