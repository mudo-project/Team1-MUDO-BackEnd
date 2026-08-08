package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.command.UpdateTaskCommand;
import com.academy.mudogroupware.workspace.application.usecase.UpdateTaskUseCase;
import com.academy.mudogroupware.workspace.domain.exception.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
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
public class UpdateTaskService implements UpdateTaskUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskStatusHistoryRepository taskStatusHistoryRepository;
  private final Clock clock;

  @Override
  @Transactional
  public Task updateTask(UpdateTaskCommand command) {
    Workspace workspace =
        workspaceRepository
            .findById(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    // 비관적 락으로 삭제와의 경합을 막는다. 조회 자체가 워크스페이스 범위로 제한되므로
    // 다른 워크스페이스의 taskId는 이 시점에 빈 결과로 걸러진다.
    Task task =
        taskRepository
            .findByIdForUpdate(command.workspaceId(), command.taskId())
            .orElseThrow(TaskNotFoundException::new);

    TaskStatus previousStatus = task.getStatus();
    LocalDate today = LocalDate.now(clock);
    Task updated =
        command.status() == null
            ? task.changeDueAt(command.dueAt())
            : task.changeStatus(command.status(), command.dueAt(), today);

    Task saved = taskRepository.save(updated);

    // 같은 상태로의 전이는 이력을 남기지 않는다.
    if (saved.getStatus() != previousStatus) {
      taskStatusHistoryRepository.append(
          TaskStatusHistory.userChanged(
              saved.getId(), previousStatus, saved.getStatus(), command.requesterId()));
    }

    return saved;
  }
}
