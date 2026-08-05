package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceRecentAccessPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkspaceRecentAccessAdapter implements WorkspaceRecentAccessPort {

  private final WorkspaceRecentAccessJpaRepository workspaceRecentAccessJpaRepository;

  @Override
  public void upsert(Long userId, Long workspaceId, LocalDateTime accessedAt) {
    workspaceRecentAccessJpaRepository.upsert(userId, workspaceId, accessedAt);
  }
}
