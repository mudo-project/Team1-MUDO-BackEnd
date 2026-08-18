package com.academy.mudogroupware.workspace.application.service.comment;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
import com.academy.mudogroupware.workspace.application.command.comment.UpdateTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.usecase.comment.UpdateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.domain.event.CommentUpdatedEvent;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
import com.academy.mudogroupware.workspace.domain.exception.comment.InvalidMentionedUserException;
import com.academy.mudogroupware.workspace.domain.exception.comment.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskCommentMention;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.comment.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateTaskCommentService implements UpdateTaskCommentUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskCommentRepository taskCommentRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
  private final Clock clock;

  @Override
  @Transactional
  public TaskComment updateComment(UpdateTaskCommentCommand command) {
    log.info(
        "event=task_comment_update_시작 workspaceId={}, taskId={}, commentId={}, requesterId={}",
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

    if (!workspace.getMemberIds().containsAll(command.mentionedUserIds())) {
      throw new InvalidMentionedUserException();
    }

    Set<Long> previousMentionedUserIds =
        comment.getMentions().stream()
            .map(TaskCommentMention::getMentionedUserId)
            .collect(Collectors.toSet());

    LocalDateTime occurredAt = LocalDateTime.now(clock);
    TaskComment updated =
        comment.updateContent(command.content(), command.mentionedUserIds(), occurredAt);
    TaskComment saved = taskCommentRepository.save(updated);

    applicationEventPublisher.publishEvent(
        new CommentUpdatedEvent(
            command.workspaceId(), command.taskId(), saved.getId(), saved.getContent(), saved.getUpdatedAt()));

    List<Long> recipientUserIds =
        command.mentionedUserIds().stream()
            .filter(userId -> !previousMentionedUserIds.contains(userId))
            .filter(userId -> !userId.equals(command.requesterId()))
            .distinct()
            .toList();

    if (!recipientUserIds.isEmpty()) {
      applicationEventPublisher.publishEvent(
          new TaskCommentMentionedEvent(
              command.workspaceId(),
              command.taskId(),
              task.getTitle(),
              saved.getId(),
              command.requesterId(),
              recipientUserIds,
              occurredAt));
    }

    AfterCommitLogger.run(
        () ->
            log.info(
                "event=task_comment_update_완료 workspaceId={}, taskId={}, commentId={}",
                command.workspaceId(),
                command.taskId(),
                saved.getId()));
    return saved;
  }
}
