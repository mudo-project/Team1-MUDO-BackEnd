package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface WorkspaceRecentAccessJpaRepository
    extends JpaRepository<WorkspaceRecentAccessJpaEntity, WorkspaceRecentAccessId> {

  @Modifying
  @Transactional
  @Query(
      value =
          """
          insert into workspace_recent_access (user_id, workspace_id, last_accessed_at)
          values (:userId, :workspaceId, :accessedAt)
          on duplicate key update last_accessed_at = greatest(last_accessed_at, :accessedAt)
          """,
      nativeQuery = true)
  void upsert(
      @Param("userId") Long userId,
      @Param("workspaceId") Long workspaceId,
      @Param("accessedAt") LocalDateTime accessedAt);
}
