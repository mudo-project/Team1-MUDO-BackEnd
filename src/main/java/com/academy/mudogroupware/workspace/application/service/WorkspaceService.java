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

    return workspaceRepository.save(workspace).getId();
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
