package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
import com.academy.mudogroupware.workspace.application.command.task.CreateTaskCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.CreateTaskUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    log.info(
        "event=task_create_시작 workspaceId={}, title={}, requesterId={}",
        command.workspaceId(),
        command.title(),
        command.requesterId());

    // 존재 확인을 권한 확인보다 먼저 한다 (기존 워크스페이스 API와 동일한 순서).
    Workspace workspace =
        workspaceRepository
            .findById(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

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

    AfterCommitLogger.run(
        () ->
            log.info(
                "event=task_create_완료 workspaceId={}, taskId={}",
                command.workspaceId(),
                saved.getId()));
    return saved.getId();
  }
}
