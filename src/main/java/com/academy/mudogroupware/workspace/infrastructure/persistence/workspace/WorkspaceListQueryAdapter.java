package com.academy.mudogroupware.workspace.infrastructure.persistence.workspace;

import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class WorkspaceListQueryAdapter implements WorkspaceListQueryPort {

  private final WorkspaceJpaRepository workspaceJpaRepository;

  @Override
  public List<WorkspaceListItem> findMine(Long userId) {
    return workspaceJpaRepository.findMineWorkspaceList(userId).stream()
        .map(this::toWorkspaceListItem)
        .toList();
  }

  @Override
  public List<WorkspaceListItem> findAll(Long userId) {
    return workspaceJpaRepository.findAllWorkspaceList(userId).stream()
        .map(this::toWorkspaceListItem)
        .toList();
  }

  @Override
  public boolean existsAccessible(Long workspaceId, Long userId, boolean canReadAll) {
    if (canReadAll) {
      return workspaceJpaRepository.countActiveWorkspace(workspaceId) > 0;
    }

    return workspaceJpaRepository.countAccessibleMineWorkspace(workspaceId, userId) > 0;
  }

  private WorkspaceListItem toWorkspaceListItem(WorkspaceJpaRepository.WorkspaceListRow row) {
    return new WorkspaceListItem(row.getWorkspaceId(), row.getName(), row.getMemberCount());
  }
}
