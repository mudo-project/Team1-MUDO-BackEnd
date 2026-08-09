package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.workspace.application.command.task.DeleteTaskCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.DeleteTaskUseCase;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskSkipRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteTaskService implements DeleteTaskUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final RecurringTaskSkipRepository recurringTaskSkipRepository;

  @Override
  @Transactional
  public void deleteTask(DeleteTaskCommand command) {
    log.info(
        "event=task_delete_시작 workspaceId={}, taskId={}",
        command.workspaceId(),
        command.taskId());

    Workspace workspace =
        workspaceRepository
            .findById(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

    // TODO: 권한 모듈의 WORKSPACE:CREATE 권한이 준비되면 참여자 조건에 추가한다.
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    Task task =
        taskRepository
            .findByIdForUpdate(command.workspaceId(), command.taskId())
            .orElseThrow(TaskNotFoundException::new);

    // 반복 업무의 특정 회차를 지우면 스케줄러가 같은 회차를 다시 만들지 않도록 기록을 남긴다.
    // 이 기록 저장과 삭제는 같은 트랜잭션에서 처리한다.
    if (task.isRecurring()) {
      recurringTaskSkipRepository.saveIfAbsent(
          task.getRecurringTemplateId(), task.getScheduledFor());
    }

    taskRepository.delete(command.taskId());
  }
}
