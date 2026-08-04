package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRecentAccessJpaRepository
    extends JpaRepository<WorkspaceRecentAccessJpaEntity, WorkspaceRecentAccessId> {}
