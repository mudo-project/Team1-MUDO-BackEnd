package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.DeleteWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.usecase.workspace.DeleteWorkspaceUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteWorkspaceService implements DeleteWorkspaceUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final Clock clock;

  @Override
  @Transactional
  public void delete(DeleteWorkspaceCommand command) {
    log.info(
        "event=workspace_delete_시작 workspaceId={}, requesterId={}",
        command.workspaceId(),
        command.requesterId());

    Workspace workspace =
        workspaceRepository
            .findByIdForUpdate(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // 참여자면 인원수와 무관하게 삭제를 허용한다(소프트 삭제라 복구 가능, 내부 직원용 기능이라 저위험).
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    workspaceRepository.delete(command.workspaceId(), LocalDateTime.now(clock));
  }
}
