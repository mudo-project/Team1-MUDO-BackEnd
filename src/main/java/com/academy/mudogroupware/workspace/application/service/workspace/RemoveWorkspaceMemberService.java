package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.RemoveWorkspaceMemberCommand;
import com.academy.mudogroupware.workspace.application.usecase.workspace.RemoveWorkspaceMemberUseCase;
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
public class RemoveWorkspaceMemberService implements RemoveWorkspaceMemberUseCase {

  private final WorkspaceRepository workspaceRepository;

  @Override
  @Transactional
  public void removeMember(RemoveWorkspaceMemberCommand command) {
    log.info(
        "event=workspace_member_remove_시작 workspaceId={}, targetUserId={}, requesterId={}, selfWithdrawal={}",
        command.workspaceId(),
        command.targetUserId(),
        command.requesterId(),
        command.requesterId().equals(command.targetUserId()));

    Workspace workspace =
        workspaceRepository
            .findByIdForUpdate(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // 자진 탈퇴와 타인 제거 모두 참여자면 허용한다. 권한 체크는 두지 않는다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    // 마지막 1인 제거·탈퇴 거부와 대상 미참여자 404는 도메인 모델이 판단한다.
    Workspace updated = workspace.removeMember(command.targetUserId());
    workspaceRepository.updateMembers(command.workspaceId(), updated.getMemberIds());
  }
}
