package com.academy.mudogroupware.users.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademyManagementJpaRepository extends JpaRepository<AcademyEntity, Long> {
}
