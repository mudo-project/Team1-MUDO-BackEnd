package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.global.domain.common.exception.ForbiddenException;
import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.port.WorkspaceRecentAccessPort;
import com.academy.mudogroupware.workspace.application.usecase.workspace.RecordWorkspaceRecentAccessUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceRecentAccessService implements RecordWorkspaceRecentAccessUseCase {

  private final WorkspaceListQueryPort workspaceListQueryPort;
  private final WorkspaceRecentAccessPort workspaceRecentAccessPort;
  private final Clock clock;

  @Override
  public void recordRecentAccess(Long academyId, Long userId, Long workspaceId, boolean canReadAll) {
    log.info(
        "event=workspace_recent_access_record_시작 workspaceId={}, userId={}",
        workspaceId,
        userId);

    if (!workspaceListQueryPort.existsAccessible(workspaceId, academyId, userId, canReadAll)) {
      throw new ForbiddenException();
    }

    workspaceRecentAccessPort.upsert(userId, workspaceId, LocalDateTime.now(clock));
  }
}
