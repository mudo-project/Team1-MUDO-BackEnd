package com.academy.mudogroupware.workspace.application.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.comment.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.domain.exception.comment.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ToggleTaskCommentCompleteServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-07T01:00:00Z"), KST);
  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long COMMENT_ID = 501L;
  private static final long MEMBER_ID = 10L;
  private static final long OTHER_MEMBER_ID = 11L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskCommentRepository taskCommentRepository;

  private ToggleTaskCommentCompleteService service() {
    return new ToggleTaskCommentCompleteService(
        workspaceRepository, taskRepository, taskCommentRepository, FIXED_CLOCK);
  }

  @Test
  void togglesFromIncompleteToCompletedByAnyMember() {
    givenWorkspaceWithMembers(MEMBER_ID, OTHER_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenComment(false, null, null);
    when(taskCommentRepository.updateCompletion(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TaskComment result =
        service()
            .toggleComplete(
                new ToggleTaskCommentCompleteCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, OTHER_MEMBER_ID));

    assertThat(result.isCompleted()).isTrue();
    assertThat(result.getCompletedBy()).isEqualTo(OTHER_MEMBER_ID);
    verify(taskCommentRepository).updateCompletion(any());
    verify(taskCommentRepository, never()).save(any());
  }

  @Test
  void togglesFromCompletedToIncomplete() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenComment(true, MEMBER_ID, LocalDateTime.of(2026, 8, 6, 9, 0));
    when(taskCommentRepository.updateCompletion(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TaskComment result =
        service()
            .toggleComplete(new ToggleTaskCommentCompleteCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID));

    assertThat(result.isCompleted()).isFalse();
    assertThat(result.getCompletedBy()).isNull();
    verify(taskCommentRepository).updateCompletion(any());
    verify(taskCommentRepository, never()).save(any());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMembers(MEMBER_ID);

    assertThatThrownBy(
            () ->
                service()
                    .toggleComplete(
                        new ToggleTaskCommentCompleteCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, OUTSIDER_ID)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);
  }

  @Test
  void rejectsMissingComment() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .toggleComplete(
                        new ToggleTaskCommentCompleteCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID)))
        .isInstanceOf(TaskCommentNotFoundException.class);
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

  private void givenComment(boolean completed, Long completedBy, LocalDateTime completedAt) {
    TaskComment comment =
        TaskComment.restore(
            COMMENT_ID, TASK_ID, MEMBER_ID, "내용", completed, completedBy, completedAt, List.of(),
            LocalDateTime.of(2026, 8, 6, 9, 0), LocalDateTime.of(2026, 8, 6, 9, 0));
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
  }
}
