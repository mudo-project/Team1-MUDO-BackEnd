package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.RenameWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.usecase.workspace.RenameWorkspaceUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RenameWorkspaceService implements RenameWorkspaceUseCase {

  private final WorkspaceRepository workspaceRepository;

  @Override
  @Transactional
  public String rename(RenameWorkspaceCommand command) {
    log.info("event=workspace_rename_시작 workspaceId={}", command.workspaceId());

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

    log.info(
        "event=workspace_rename_완료 workspaceId={}, name={}",
        command.workspaceId(),
        renamed.getName());
    return renamed.getName();
  }
}
