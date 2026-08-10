package com.academy.mudogroupware.workspace.application.service.comment;

import com.academy.mudogroupware.workspace.application.command.comment.DeleteTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.usecase.comment.DeleteTaskCommentUseCase;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeleteTaskCommentService implements DeleteTaskCommentUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskCommentRepository taskCommentRepository;

  @Override
  @Transactional
  public void deleteComment(DeleteTaskCommentCommand command) {
    log.info(
        "event=task_comment_delete_시작 workspaceId={}, taskId={}, commentId={}, requesterId={}",
        command.workspaceId(),
        command.taskId(),
        command.commentId(),
        command.requesterId());

    Workspace workspace =
        workspaceRepository
            .findById(command.workspaceId())
            .orElseThrow(WorkspaceNotFoundException::new);

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

    taskCommentRepository.deleteById(command.commentId());
  }
}
