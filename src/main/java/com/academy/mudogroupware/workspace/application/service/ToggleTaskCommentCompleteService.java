package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.command.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.application.usecase.ToggleTaskCommentCompleteUseCase;
import com.academy.mudogroupware.workspace.domain.exception.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ToggleTaskCommentCompleteService implements ToggleTaskCommentCompleteUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskCommentRepository taskCommentRepository;
  private final Clock clock;

  @Override
  @Transactional
  public TaskComment toggleComplete(ToggleTaskCommentCompleteCommand command) {
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

    TaskComment comment =
        taskCommentRepository
            .findById(command.commentId())
            .orElseThrow(TaskCommentNotFoundException::new);
    if (!comment.belongsTo(command.taskId())) {
      throw new TaskCommentNotFoundException();
    }

    TaskComment toggled = comment.toggleComplete(command.requesterId(), LocalDateTime.now(clock));

    return taskCommentRepository.save(toggled);
  }
}
