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
    log.info("event=workspace_delete_시작 workspaceId={}", command.workspaceId());

    Workspace workspace =
        workspaceRepository
            .findByIdForUpdate(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // 참여자 2인 이상: TODO 권한 모듈의 WORKSPACE:DELETE 권한이 준비되면 참여자 조건에 추가한다.
    // 참여자가 본인 1인뿐인 경우: 자진 탈퇴의 대체 행위이므로 앞으로도 권한 없이 허용한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    workspaceRepository.delete(command.workspaceId(), LocalDateTime.now(clock));
  }
}
