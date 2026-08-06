package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findWithPermissionsById(Long roleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "permissions")
    @Query("select r from RoleEntity r where r.id = :roleId")
    Optional<RoleEntity> findWithPermissionsByIdForUpdate(Long roleId);

    boolean existsByAcademyIdAndName(Long academyId, String name);
}
