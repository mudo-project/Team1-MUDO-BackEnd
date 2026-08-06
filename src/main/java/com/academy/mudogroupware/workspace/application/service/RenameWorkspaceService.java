package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.command.RenameWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.usecase.RenameWorkspaceUseCase;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RenameWorkspaceService implements RenameWorkspaceUseCase {

  private final WorkspaceRepository workspaceRepository;

  @Override
  @Transactional
  public String rename(RenameWorkspaceCommand command) {
    Workspace workspace =
        workspaceRepository
            .findByIdForUpdate(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    Workspace renamed = workspace.rename(command.name());
    workspaceRepository.rename(command.workspaceId(), renamed.getName());
    return renamed.getName();
  }
}
