package com.academy.mudogroupware.lecture.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassroomJpaRepository extends JpaRepository<ClassroomEntity, Long> {

    Optional<ClassroomEntity> findByName(String name);
}
