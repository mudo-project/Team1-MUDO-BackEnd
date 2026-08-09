package com.academy.mudogroupware.workspace.application.service.workspace;

import com.academy.mudogroupware.workspace.application.command.workspace.CreateWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.port.WorkspaceMemberDirectoryPort;
import com.academy.mudogroupware.workspace.application.usecase.workspace.CreateWorkspaceUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.InvalidWorkspaceMemberException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService implements CreateWorkspaceUseCase {

  private final WorkspaceMemberDirectoryPort workspaceMemberDirectoryPort;
  private final WorkspaceRepository workspaceRepository;

  @Override
  @Transactional
  public Long createWorkspace(CreateWorkspaceCommand command) {
    log.info(
        "event=workspace_create_시작 academyId={}, name={}, creatorId={}",
        command.academyId(),
        command.name(),
        command.creatorId());

    // ws 이름 저장
    String name = command.name().trim();
    // Domain 모델 생성
    Workspace workspace = Workspace.create(
        command.academyId(),
        name,
        command.creatorId(),
        requestedAdditionalMemberIds(command));

    // 참여자 검증
    Set<Long> activeUserIds =
        workspaceMemberDirectoryPort.findActiveUserIds(
                command.academyId(), workspace.getMemberIds());

    // 추가 참여자 + 생성자도 학원 포함 검증
    if (!activeUserIds.containsAll(workspace.getMemberIds())) {
      throw new InvalidWorkspaceMemberException();
    }
    // 중복 이름 예외 처리
    if (workspaceRepository.existsByAcademyIdAndName(command.academyId(), name)) {
      throw new WorkspaceNameConflictException();
    }

    Long workspaceId = workspaceRepository.save(workspace).getId();

    log.info(
        "event=workspace_create_완료 academyId={}, workspaceId={}",
        command.academyId(),
        workspaceId);
    return workspaceId;
  }

  // 참여자 처리
  private Set<Long> requestedAdditionalMemberIds(CreateWorkspaceCommand command) {
    // 중복 제거, 순서 유지, null 허용
    Set<Long> memberIds = new LinkedHashSet<>();
    if (command.memberIds() != null) {
      memberIds.addAll(command.memberIds());
    }
    return memberIds;
  }
}
