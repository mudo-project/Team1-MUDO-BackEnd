package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academy.mudogroupware.users.domain.model.AcademyApplicationStatus;

public interface AcademyApplicationJpaRepository extends JpaRepository<AcademyApplicationEntity, Long> {

    boolean existsByRequestedLoginIdAndStatusIn(String requestedLoginId, List<AcademyApplicationStatus> statuses);
}
