package com.academy.mudogroupware.workspace.application.service.comment;

import com.academy.mudogroupware.workspace.application.command.comment.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.usecase.comment.CreateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.domain.exception.comment.InvalidMentionedUserException;
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
public class CreateTaskCommentService implements CreateTaskCommentUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskCommentRepository taskCommentRepository;
  private final Clock clock;

  @Override
  @Transactional
  public TaskComment createComment(CreateTaskCommentCommand command) {
    log.info(
        "event=task_comment_create_시작 workspaceId={}, taskId={}, requesterId={}",
        command.workspaceId(),
        command.taskId(),
        command.requesterId());

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

    if (!workspace.getMemberIds().containsAll(command.mentionedUserIds())) {
      throw new InvalidMentionedUserException();
    }

    TaskComment comment =
        TaskComment.create(
            command.taskId(),
            command.requesterId(),
            command.content(),
            command.mentionedUserIds(),
            LocalDateTime.now(clock));

    TaskComment saved = taskCommentRepository.save(comment);

    log.info(
        "event=task_comment_create_완료 workspaceId={}, taskId={}, commentId={}",
        command.workspaceId(),
        command.taskId(),
        saved.getId());
    return saved;
  }
}
