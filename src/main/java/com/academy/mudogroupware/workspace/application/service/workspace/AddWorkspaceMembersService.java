package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.AddWorkspaceMembersCommand;
import com.academy.mudogroupware.workspace.application.port.WorkspaceMemberDirectoryPort;
import com.academy.mudogroupware.workspace.application.usecase.workspace.AddWorkspaceMembersUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.InvalidWorkspaceMemberException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddWorkspaceMembersService implements AddWorkspaceMembersUseCase {

  private final WorkspaceMemberDirectoryPort workspaceMemberDirectoryPort;
  private final WorkspaceRepository workspaceRepository;

  @Override
  @Transactional
  public Set<Long> addMembers(AddWorkspaceMembersCommand command) {
    Workspace workspace =
        workspaceRepository
            .findByIdForUpdate(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    Set<Long> requestedIds = new LinkedHashSet<>(command.memberIds());
    Set<Long> newIds = workspace.newlyAddedMemberIds(requestedIds);
    if (newIds.isEmpty()) {
      return newIds;
    }

    Set<Long> activeIds = workspaceMemberDirectoryPort.findActiveUserIds(command.academyId(), newIds);
    if (!activeIds.containsAll(newIds)) {
      throw new InvalidWorkspaceMemberException();
    }

    Workspace updated = workspace.addMembers(newIds);
    workspaceRepository.updateMembers(command.workspaceId(), updated.getMemberIds());
    return newIds;
  }
}
