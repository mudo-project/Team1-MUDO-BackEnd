package com.academy.mudogroupware.workspace.application.service.comment;

import com.academy.mudogroupware.workspace.application.command.comment.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.application.usecase.comment.ToggleTaskCommentCompleteUseCase;
import com.academy.mudogroupware.workspace.domain.exception.comment.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.comment.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
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
public class ToggleTaskCommentCompleteService implements ToggleTaskCommentCompleteUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskCommentRepository taskCommentRepository;
  private final Clock clock;

  @Override
  @Transactional
  public TaskComment toggleComplete(ToggleTaskCommentCompleteCommand command) {
    log.info(
        "event=task_comment_toggle_complete_시작 workspaceId={}, taskId={}, commentId={}",
        command.workspaceId(),
        command.taskId(),
        command.commentId());

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

    TaskComment saved = taskCommentRepository.save(toggled);

    log.info(
        "event=task_comment_toggle_complete_완료 workspaceId={}, taskId={}, commentId={}, completed={}",
        command.workspaceId(),
        command.taskId(),
        command.commentId(),
        saved.isCompleted());
    return saved;
  }
}
