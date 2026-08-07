package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.command.CreateTaskCommand;
import com.academy.mudogroupware.workspace.application.usecase.CreateTaskUseCase;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.TaskStatusHistoryRepository;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateTaskService implements CreateTaskUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskStatusHistoryRepository taskStatusHistoryRepository;
  private final Clock clock;

  @Override
  @Transactional
  public Long createTask(CreateTaskCommand command) {
    // 존재 확인을 권한 확인보다 먼저 한다 (기존 워크스페이스 API와 동일한 순서).
    Workspace workspace =
        workspaceRepository
            .findById(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    LocalDate today = LocalDate.now(clock);
    Task saved =
        taskRepository.save(
            Task.create(
                command.workspaceId(),
                command.title(),
                command.dueAt(),
                command.requesterId(),
                today));

    taskStatusHistoryRepository.append(
        TaskStatusHistory.userChanged(
            saved.getId(), null, saved.getStatus(), command.requesterId()));

    return saved.getId();
  }
}
