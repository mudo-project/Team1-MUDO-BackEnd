package com.academy.mudogroupware.sharedfile.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SharedFileRootConnectionHistoryJpaRepository
        extends JpaRepository<SharedFileRootConnectionHistoryEntity, String> {
}
