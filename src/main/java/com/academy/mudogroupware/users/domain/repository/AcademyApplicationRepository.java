package com.academy.mudogroupware.users.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.academy.mudogroupware.users.domain.model.AcademyApplication;

public interface AcademyApplicationRepository {

    List<AcademyApplication> findAll();

    Optional<AcademyApplication> findById(Long id);

    void markApproved(Long id, Long reviewerId, LocalDateTime reviewedAt);

    void markRejected(Long id, Long reviewerId, LocalDateTime reviewedAt, String reason);
}
