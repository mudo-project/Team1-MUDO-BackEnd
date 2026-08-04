package com.academy.mudogroupware.attendance.domain.repository;

import java.util.Optional;

import com.academy.mudogroupware.attendance.domain.model.OwnedAcademy;

public interface AcademyRepository {
    Optional<OwnedAcademy> findByOwnerUserId(Long userId);
}
