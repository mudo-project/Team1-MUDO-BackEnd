package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListScope;
import com.academy.mudogroupware.workspace.application.usecase.workspace.WorkspaceQueryUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceQueryService implements WorkspaceQueryUseCase {

  private final WorkspaceListQueryPort workspaceListQueryPort;

  @Override
  public List<WorkspaceListItem> getWorkspaces(
      Long academyId, Long userId, WorkspaceListScope scope) {
    return switch (scope) {
      case MINE -> workspaceListQueryPort.findMine(academyId, userId);
      case ALL -> workspaceListQueryPort.findAll(academyId, userId);
    };
  }
}
