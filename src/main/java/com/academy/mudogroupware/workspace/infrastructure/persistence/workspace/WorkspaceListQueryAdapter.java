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
  public List<WorkspaceListItem> findMine(Long academyId, Long userId) {
    return workspaceJpaRepository.findMineWorkspaceList(academyId, userId).stream()
        .map(this::toWorkspaceListItem)
        .toList();
  }

  @Override
  public List<WorkspaceListItem> findAll(Long academyId, Long userId) {
    return workspaceJpaRepository.findAllWorkspaceList(academyId, userId).stream()
        .map(this::toWorkspaceListItem)
        .toList();
  }

  @Override
  public boolean existsAccessible(Long workspaceId, Long academyId, Long userId, boolean canReadAll) {
    if (canReadAll) {
      return workspaceJpaRepository.countActiveWorkspace(workspaceId, academyId) > 0;
    }

    return workspaceJpaRepository.countAccessibleMineWorkspace(workspaceId, academyId, userId) > 0;
  }

  private WorkspaceListItem toWorkspaceListItem(WorkspaceJpaRepository.WorkspaceListRow row) {
    return new WorkspaceListItem(row.getWorkspaceId(), row.getName(), row.getMemberCount());
  }
}
