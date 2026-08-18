package com.academy.mudogroupware.workspace.application.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.comment.UpdateTaskCommentCommand;
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
class UpdateTaskCommentServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-07T01:00:00Z"), KST);
  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long OTHER_TASK_ID = 102L;
  private static final long COMMENT_ID = 501L;
  private static final long MEMBER_ID = 10L;
  private static final long OTHER_MEMBER_ID = 11L;
  private static final long THIRD_MEMBER_ID = 12L;
  private static final long OUTSIDER_ID = 99L;
  private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 8, 7, 10, 0);

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskCommentRepository taskCommentRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Captor private ArgumentCaptor<TaskCommentMentionedEvent> eventCaptor;
  @Captor private ArgumentCaptor<CommentUpdatedEvent> commentUpdatedEventCaptor;

  private UpdateTaskCommentService service() {
    return new UpdateTaskCommentService(
        workspaceRepository,
        taskRepository,
        taskCommentRepository,
        applicationEventPublisher,
        FIXED_CLOCK);
  }

  @Test
  void anyMemberCanUpdateContentAndMentions() {
    givenWorkspaceWithMembers(MEMBER_ID, OTHER_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenComment(TASK_ID, MEMBER_ID);
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TaskComment result =
        service()
            .updateComment(
                new UpdateTaskCommentCommand(
                    WORKSPACE_ID, TASK_ID, COMMENT_ID, OTHER_MEMBER_ID, "수정된 내용", List.of(OTHER_MEMBER_ID)));

    assertThat(result.getContent()).isEqualTo("수정된 내용");

    verify(applicationEventPublisher).publishEvent(commentUpdatedEventCaptor.capture());
    CommentUpdatedEvent published = commentUpdatedEventCaptor.getValue();
    assertThat(published.workspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(published.commentId()).isEqualTo(COMMENT_ID);
  }

  @Test
  void updateReplacesExistingMentionsEntirely() {
    givenWorkspaceWithMembers(MEMBER_ID, OTHER_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenCommentWithMentions(TASK_ID, MEMBER_ID, List.of(MEMBER_ID));
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TaskComment result =
        service()
            .updateComment(
                new UpdateTaskCommentCommand(
                    WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID, "수정된 내용", List.of(OTHER_MEMBER_ID)));

    assertThat(result.getMentions()).extracting(m -> m.getMentionedUserId())
        .containsExactly(OTHER_MEMBER_ID);
  }

  @Test
  void publishesMentionEventOnlyForNewDistinctRecipientsExcludingRequester() {
    givenWorkspaceWithMembers(MEMBER_ID, OTHER_MEMBER_ID, THIRD_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenCommentWithMentions(TASK_ID, MEMBER_ID, List.of(MEMBER_ID, OTHER_MEMBER_ID));
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service()
        .updateComment(
            new UpdateTaskCommentCommand(
                WORKSPACE_ID,
                TASK_ID,
                COMMENT_ID,
                MEMBER_ID,
                "수정된 내용",
                List.of(OTHER_MEMBER_ID, THIRD_MEMBER_ID, MEMBER_ID, THIRD_MEMBER_ID)));

    verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
    TaskCommentMentionedEvent event = eventCaptor.getValue();
    assertThat(event.workspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(event.taskId()).isEqualTo(TASK_ID);
    assertThat(event.taskTitle()).isEqualTo("업무");
    assertThat(event.commentId()).isEqualTo(COMMENT_ID);
    assertThat(event.actorUserId()).isEqualTo(MEMBER_ID);
    assertThat(event.recipientUserIds()).containsExactly(THIRD_MEMBER_ID);
    assertThat(event.occurredAt()).isEqualTo(FIXED_NOW);
  }

  @Test
  void doesNotPublishMentionEventForRetainedRemovedOrRequesterMentions() {
    givenWorkspaceWithMembers(MEMBER_ID, OTHER_MEMBER_ID, THIRD_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenCommentWithMentions(TASK_ID, MEMBER_ID, List.of(OTHER_MEMBER_ID, THIRD_MEMBER_ID));
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service()
        .updateComment(
            new UpdateTaskCommentCommand(
                WORKSPACE_ID,
                TASK_ID,
                COMMENT_ID,
                MEMBER_ID,
                "수정된 내용",
                List.of(OTHER_MEMBER_ID, MEMBER_ID)));

    // CommentUpdatedEvent(실시간 브로드캐스트)는 항상 발행되지만, 신규 멘션 대상이 없어
    // TaskCommentMentionedEvent(알림)는 발행되지 않아야 한다.
    verify(applicationEventPublisher, never()).publishEvent(any(TaskCommentMentionedEvent.class));
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .updateComment(
                        new UpdateTaskCommentCommand(
                            WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID, "내용", List.of())))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMembers(MEMBER_ID);

    assertThatThrownBy(
            () ->
                service()
                    .updateComment(
                        new UpdateTaskCommentCommand(
                            WORKSPACE_ID, TASK_ID, COMMENT_ID, OUTSIDER_ID, "내용", List.of())))
        .isInstanceOf(WorkspaceAccessDeniedException.class);
  }

  @Test
  void rejectsMissingTask() {
    givenWorkspaceWithMembers(MEMBER_ID);
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .updateComment(
                        new UpdateTaskCommentCommand(
                            WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID, "내용", List.of())))
        .isInstanceOf(TaskNotFoundException.class);
  }

  @Test
  void rejectsCommentFromAnotherTask() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenComment(OTHER_TASK_ID, MEMBER_ID);

    assertThatThrownBy(
            () ->
                service()
                    .updateComment(
                        new UpdateTaskCommentCommand(
                            WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID, "내용", List.of())))
        .isInstanceOf(TaskCommentNotFoundException.class);
  }

  @Test
  void rejectsMissingComment() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .updateComment(
                        new UpdateTaskCommentCommand(
                            WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID, "내용", List.of())))
        .isInstanceOf(TaskCommentNotFoundException.class);
  }

  @Test
  void rejectsMentioningNonMember() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenComment(TASK_ID, MEMBER_ID);

    assertThatThrownBy(
            () ->
                service()
                    .updateComment(
                        new UpdateTaskCommentCommand(
                            WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID, "내용", List.of(OUTSIDER_ID))))
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

  private void givenComment(long owningTaskId, long authorId) {
    TaskComment comment =
        TaskComment.restore(
            COMMENT_ID, owningTaskId, authorId, "원본", false, null, null, List.of(),
            LocalDateTime.of(2026, 8, 7, 9, 0), LocalDateTime.of(2026, 8, 7, 9, 0));
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
  }

  private void givenCommentWithMentions(long owningTaskId, long authorId, List<Long> mentionedUserIds) {
    LocalDateTime createdAt = LocalDateTime.of(2026, 8, 7, 9, 0);
    List<TaskCommentMention> mentions =
        mentionedUserIds.stream().map(userId -> TaskCommentMention.create(userId, createdAt)).toList();
    TaskComment comment =
        TaskComment.restore(
            COMMENT_ID, owningTaskId, authorId, "원본", false, null, null, mentions, createdAt, createdAt);
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
  }
}
