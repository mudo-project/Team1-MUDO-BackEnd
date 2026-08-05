package com.academy.mudogroupware.lecture.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentJpaRepository extends JpaRepository<StudentEntity, Long> {
}
