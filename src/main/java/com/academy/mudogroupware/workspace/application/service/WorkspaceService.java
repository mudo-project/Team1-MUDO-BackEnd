package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.command.CreateWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.port.WorkspaceMemberDirectoryPort;
import com.academy.mudogroupware.workspace.application.usecase.CreateWorkspaceUseCase;
import com.academy.mudogroupware.workspace.domain.exception.InvalidWorkspaceMemberException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WorkspaceService implements CreateWorkspaceUseCase {

  private final WorkspaceMemberDirectoryPort workspaceMemberDirectoryPort;
  private final WorkspaceRepository workspaceRepository;

  @Override
  @Transactional
  public Long createWorkspace(CreateWorkspaceCommand command) {
    String name = command.name().trim();
    Workspace workspace = Workspace.builder()
        .academyId(command.academyId())
        .name(name)
        .createdBy(command.creatorId())
        .memberIds(requestedAdditionalMemberIds(command))
        .build();
    Set<Long> activeUserIds =
        workspaceMemberDirectoryPort.findActiveUserIds(command.academyId(), workspace.getMemberIds());

    if (!activeUserIds.containsAll(workspace.getMemberIds())) {
      throw new InvalidWorkspaceMemberException();
    }

    if (workspaceRepository.existsByAcademyIdAndName(command.academyId(), name)) {
      throw new WorkspaceNameConflictException();
    }

    return workspaceRepository.save(workspace).getId();
  }

  private Set<Long> requestedAdditionalMemberIds(CreateWorkspaceCommand command) {
    Set<Long> memberIds = new LinkedHashSet<>();
    if (command.memberIds() != null) {
      memberIds.addAll(command.memberIds());
    }
    return memberIds;
  }
}
