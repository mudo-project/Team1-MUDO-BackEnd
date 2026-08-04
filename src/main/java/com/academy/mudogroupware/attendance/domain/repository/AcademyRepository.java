package com.academy.mudogroupware.attendance.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.Academy;

public interface AcademyRepository {
    Optional<Academy> findByOwnerUserId(Long userId);
}
