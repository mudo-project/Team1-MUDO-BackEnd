package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceJpaRepository extends JpaRepository<WorkspaceJpaEntity, Long> {

  boolean existsByAcademyIdAndNameAndDeletedAtIsNull(Long academyId, String name);
}
