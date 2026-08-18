package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedFileRootHistoryJpaRepository extends JpaRepository<SharedFileRootHistoryEntity, Long> {

    Optional<SharedFileRootHistoryEntity> findByGoogleEmail(String googleEmail);
}
