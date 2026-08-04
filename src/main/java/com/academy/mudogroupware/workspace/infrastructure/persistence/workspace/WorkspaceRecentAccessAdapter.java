package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceRecentAccessPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkspaceRecentAccessAdapter implements WorkspaceRecentAccessPort {

  private final WorkspaceRecentAccessJpaRepository workspaceRecentAccessJpaRepository;
  private final WorkspaceJpaRepository workspaceJpaRepository;

  @Override
  public void upsert(Long userId, Long workspaceId, LocalDateTime accessedAt) {
    WorkspaceRecentAccessId id = WorkspaceRecentAccessId.of(userId, workspaceId);
    workspaceRecentAccessJpaRepository
        .findById(id)
        .ifPresentOrElse(
            recentAccess -> recentAccess.updateAccessedAt(accessedAt),
            () ->
                workspaceRecentAccessJpaRepository.save(
                    WorkspaceRecentAccessJpaEntity.create(
                        workspaceJpaRepository.getReferenceById(workspaceId), userId, accessedAt)));
  }
}
