package com.academy.mudogroupware.users.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<RoleEntity, Long> {

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findWithPermissionsById(Long roleId);

    boolean existsByAcademyIdAndName(Long academyId, String name);
}
