package com.academy.mudogroupware.workspace.application.service.comment;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
import com.academy.mudogroupware.workspace.application.command.comment.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.usecase.comment.CreateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
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
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateTaskCommentService implements CreateTaskCommentUseCase {

  private final WorkspaceRepository workspaceRepository;
  private final TaskRepository taskRepository;
  private final TaskCommentRepository taskCommentRepository;
  private final ApplicationEventPublisher applicationEventPublisher;
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
    // 워크 스페이스 소속 검증
    if (!workspace.getMemberIds().contains(command.requesterId())) {
      throw new WorkspaceAccessDeniedException();
    }

    // 업무 조회 검증
    Task task =
        taskRepository
            .findByIdForUpdate(command.workspaceId(), command.taskId())
            .orElseThrow(TaskNotFoundException::new);

    // 멘션 대상이 워크스페이스 참여자인지 검증
    if (!workspace.getMemberIds().containsAll(command.mentionedUserIds())) {
      throw new InvalidMentionedUserException();
    }

    LocalDateTime occurredAt = LocalDateTime.now(clock);

    // 댓글 객체 생성
    TaskComment comment =
        TaskComment.create(
            command.taskId(),
            command.requesterId(),
            command.content(),
            command.mentionedUserIds(),
            occurredAt);

    // 댓글 저장
    TaskComment saved = taskCommentRepository.save(comment);

    // 멘션 대상 중 요청자 자신을 제외하고 중복 제거해 알림 수신자 목록 계산
    List<Long> recipientUserIds =
        command.mentionedUserIds().stream()
            .filter(userId -> !userId.equals(command.requesterId()))
            .distinct()
            .toList();

    if (!recipientUserIds.isEmpty()) {
      // 멘션 알림 이벤트 생성
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
                "event=task_comment_create_완료 workspaceId={}, taskId={}, commentId={}",
                command.workspaceId(),
                command.taskId(),
                saved.getId()));
    return saved;
  }
}
