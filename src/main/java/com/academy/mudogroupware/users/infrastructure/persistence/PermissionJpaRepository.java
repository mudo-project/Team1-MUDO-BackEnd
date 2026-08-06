package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionJpaRepository extends JpaRepository<PermissionEntity, Long> {

    List<PermissionEntity> findAllByCodeIn(Collection<String> codes);
}
