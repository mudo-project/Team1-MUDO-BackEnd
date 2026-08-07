package com.academy.mudogroupware.google.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleAccountConnectionJpaRepository extends JpaRepository<GoogleAccountConnectionEntity, Long> {

    Optional<GoogleAccountConnectionEntity> findByAcademyId(Long academyId);

    void deleteByAcademyId(Long academyId);
}
