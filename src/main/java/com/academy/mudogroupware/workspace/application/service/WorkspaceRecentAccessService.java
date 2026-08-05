package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceRecentAccessPort;
import com.academy.mudogroupware.workspace.application.usecase.RecordWorkspaceRecentAccessUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceRecentAccessService implements RecordWorkspaceRecentAccessUseCase {

  private final WorkspaceListQueryPort workspaceListQueryPort;
  private final WorkspaceRecentAccessPort workspaceRecentAccessPort;
  private final Clock clock;

  @Override
  public void recordRecentAccess(Long academyId, Long userId, Long workspaceId, boolean canReadAll) {
    if (!workspaceListQueryPort.existsAccessible(workspaceId, academyId, userId, canReadAll)) {
      throw new ForbiddenException();
    }

    workspaceRecentAccessPort.upsert(userId, workspaceId, LocalDateTime.now(clock));
  }
}
