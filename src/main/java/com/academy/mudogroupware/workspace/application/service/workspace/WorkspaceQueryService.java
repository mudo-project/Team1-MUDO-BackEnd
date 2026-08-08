package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListScope;
import com.academy.mudogroupware.workspace.application.usecase.workspace.WorkspaceQueryUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceQueryService implements WorkspaceQueryUseCase {

  private final WorkspaceListQueryPort workspaceListQueryPort;

  @Override
  public List<WorkspaceListItem> getWorkspaces(
      Long academyId, Long userId, WorkspaceListScope scope) {
    log.info("event=workspace_list_query_시작 academyId={}, userId={}, scope={}", academyId, userId, scope);

    List<WorkspaceListItem> workspaces =
        switch (scope) {
          case MINE -> workspaceListQueryPort.findMine(academyId, userId);
          case ALL -> workspaceListQueryPort.findAll(academyId, userId);
        };

    log.info(
        "event=workspace_list_query_완료 academyId={}, userId={}, count={}",
        academyId,
        userId,
        workspaces.size());
    return workspaces;
  }
}
