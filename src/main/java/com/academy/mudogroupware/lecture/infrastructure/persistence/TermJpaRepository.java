package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TermJpaRepository extends JpaRepository<TermEntity, Long> {

    Optional<TermEntity> findByName(String name);
}
