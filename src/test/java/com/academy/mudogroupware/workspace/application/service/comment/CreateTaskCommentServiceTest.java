package com.academy.mudogroupware.workspace.application.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.comment.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.domain.event.TaskCommentMentionedEvent;
import com.academy.mudogroupware.workspace.domain.exception.comment.InvalidMentionedUserException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.comment.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class CreateTaskCommentServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-07T01:00:00Z"), KST);
  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long COMMENT_ID = 501L;
  private static final long MEMBER_ID = 10L;
  private static final long MENTIONED_MEMBER_ID = 11L;
  private static final long THIRD_MEMBER_ID = 12L;
  private static final long OUTSIDER_ID = 99L;
  private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 7, 10, 0);

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskCommentRepository taskCommentRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Captor private ArgumentCaptor<TaskComment> commentCaptor;
  @Captor private ArgumentCaptor<TaskCommentMentionedEvent> eventCaptor;

  private CreateTaskCommentService service() {
    return new CreateTaskCommentService(
        workspaceRepository,
        taskRepository,
        taskCommentRepository,
        applicationEventPublisher,
        FIXED_CLOCK);
  }

  @Test
  void createsCommentWithValidMentions() {
    givenWorkspaceWithMembers(MEMBER_ID, MENTIONED_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TaskComment result =
        service()
            .createComment(
                new CreateTaskCommentCommand(
                    WORKSPACE_ID, TASK_ID, MEMBER_ID, "확인 부탁드립니다", List.of(MENTIONED_MEMBER_ID)));

    assertThat(result.getContent()).isEqualTo("확인 부탁드립니다");
    verify(taskCommentRepository).save(commentCaptor.capture());
    assertThat(commentCaptor.getValue().getMentions()).extracting(m -> m.getMentionedUserId())
        .containsExactly(MENTIONED_MEMBER_ID);
  }

  @Test
  void publishesMentionEventForDistinctRecipientsExcludingRequester() {
    givenWorkspaceWithMembers(MEMBER_ID, MENTIONED_MEMBER_ID, THIRD_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    when(taskCommentRepository.save(any()))
        .thenAnswer(invocation -> savedComment(invocation.getArgument(0)));

    service()
        .createComment(
            new CreateTaskCommentCommand(
                WORKSPACE_ID,
                TASK_ID,
                MEMBER_ID,
                "확인 부탁드립니다",
                List.of(MEMBER_ID, MENTIONED_MEMBER_ID, THIRD_MEMBER_ID, MENTIONED_MEMBER_ID)));

    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    TaskCommentMentionedEvent event = eventCaptor.getValue();
    assertThat(event.workspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(event.taskId()).isEqualTo(TASK_ID);
    assertThat(event.taskTitle()).isEqualTo("업무");
    assertThat(event.commentId()).isEqualTo(COMMENT_ID);
    assertThat(event.actorUserId()).isEqualTo(MEMBER_ID);
    assertThat(event.recipientUserIds()).containsExactly(MENTIONED_MEMBER_ID, THIRD_MEMBER_ID);
    assertThat(event.occurredAt()).isEqualTo(FIXED_NOW);
  }

  @Test
  void doesNotPublishMentionEventWhenOnlyRequesterIsMentioned() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service()
        .createComment(
            new CreateTaskCommentCommand(
                WORKSPACE_ID, TASK_ID, MEMBER_ID, "내용", List.of(MEMBER_ID)));

    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .createComment(
                        new CreateTaskCommentCommand(WORKSPACE_ID, TASK_ID, MEMBER_ID, "내용", List.of())))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(taskRepository, never()).findByIdForUpdate(any(), any());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMembers(MEMBER_ID);

    assertThatThrownBy(
            () ->
                service()
                    .createComment(
                        new CreateTaskCommentCommand(WORKSPACE_ID, TASK_ID, OUTSIDER_ID, "내용", List.of())))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(taskRepository, never()).findByIdForUpdate(any(), any());
  }

  @Test
  void rejectsMentioningNonMember() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);

    assertThatThrownBy(
            () ->
                service()
                    .createComment(
                        new CreateTaskCommentCommand(
                            WORKSPACE_ID, TASK_ID, MEMBER_ID, "내용", List.of(OUTSIDER_ID))))
        .isInstanceOf(InvalidMentionedUserException.class);

    verify(taskCommentRepository, never()).save(any());
    verify(applicationEventPublisher, never()).publishEvent(any());
  }

  private void givenWorkspaceWithMembers(long... memberIds) {
    Set<Long> members = new LinkedHashSet<>();
    for (long id : memberIds) {
      members.add(id);
    }
    Workspace workspace = Workspace.restore(WORKSPACE_ID, "8월 학사 운영", MEMBER_ID, members);
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private void givenTask(long owningWorkspaceId) {
    Task task =
        Task.restore(
            TASK_ID, owningWorkspaceId, null, "업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 10),
            null, MEMBER_ID);
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }

  private TaskComment savedComment(TaskComment comment) {
    return TaskComment.restore(
        COMMENT_ID,
        comment.getTaskId(),
        comment.getAuthorId(),
        comment.getContent(),
        comment.isCompleted(),
        comment.getCompletedBy(),
        comment.getCompletedAt(),
        comment.getMentions(),
        comment.getCreatedAt(),
        comment.getUpdatedAt());
  }
}
