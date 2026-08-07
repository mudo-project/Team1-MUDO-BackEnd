package com.academy.mudogroupware.users.domain.repository;

import java.time.LocalDateTime;

import com.academy.mudogroupware.users.domain.model.Academy;

public interface AcademyRepository {

    Academy save(Academy academy);

    void assignUser(Long academyId, Long userId, LocalDateTime updatedAt);
}
