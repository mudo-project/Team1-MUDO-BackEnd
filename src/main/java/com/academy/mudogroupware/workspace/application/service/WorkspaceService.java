package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.command.CreateWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.port.WorkspaceMemberDirectoryPort;
import com.academy.mudogroupware.workspace.application.usecase.CreateWorkspaceUseCase;
import com.academy.mudogroupware.workspace.domain.exception.InvalidWorkspaceMemberException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNameConflictException;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceService implements CreateWorkspaceUseCase {

  private final WorkspaceMemberDirectoryPort workspaceMemberDirectoryPort;
  private final WorkspaceCreationTransaction workspaceCreationTransaction;

  @Override
  public Long createWorkspace(CreateWorkspaceCommand command) {
    //
    String baseName = command.name().trim();
    //
    Set<Long> memberIds = requestedMemberIds(command);
    //
    Set<Long> activeUserIds =
        workspaceMemberDirectoryPort.findActiveUserIds(command.academyId(), memberIds);

    if (!activeUserIds.containsAll(memberIds)) {
      throw new InvalidWorkspaceMemberException();
    }

    try {
      return workspaceCreationTransaction.create(
          command.academyId(), command.creatorId(), baseName, memberIds);
    } catch (WorkspaceNameConflictException exception) {
      return workspaceCreationTransaction.create(
          command.academyId(), command.creatorId(), baseName, memberIds);
    }
  }

  private Set<Long> requestedMemberIds(CreateWorkspaceCommand command) {
    Set<Long> memberIds = new LinkedHashSet<>();
    if (command.memberIds() != null) {
      memberIds.addAll(command.memberIds());
    }
    memberIds.add(command.creatorId());
    return memberIds;
  }
}
