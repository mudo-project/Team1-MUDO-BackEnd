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
  public List<WorkspaceListItem> getWorkspaces(Long userId, WorkspaceListScope scope) {
    log.info("event=workspace_list_시작 userId={}, scope={}", userId, scope);

    List<WorkspaceListItem> result =
        switch (scope) {
          case MINE -> workspaceListQueryPort.findMine(userId);
          case ALL -> workspaceListQueryPort.findAll(userId);
        };

    log.info("event=workspace_list_완료 userId={}, count={}", userId, result.size());
    return result;
  }
}
