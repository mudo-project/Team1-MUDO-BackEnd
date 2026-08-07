package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.domain.exception.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
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
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TaskComment result =
        service()
            .toggleComplete(
                new ToggleTaskCommentCompleteCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, OTHER_MEMBER_ID));

    assertThat(result.isCompleted()).isTrue();
    assertThat(result.getCompletedBy()).isEqualTo(OTHER_MEMBER_ID);
  }

  @Test
  void togglesFromCompletedToIncomplete() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenComment(true, MEMBER_ID, LocalDateTime.of(2026, 8, 6, 9, 0));
    when(taskCommentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    TaskComment result =
        service()
            .toggleComplete(new ToggleTaskCommentCompleteCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID));

    assertThat(result.isCompleted()).isFalse();
    assertThat(result.getCompletedBy()).isNull();
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
    Workspace workspace = Workspace.restore(WORKSPACE_ID, 1L, "8월 학사 운영", MEMBER_ID, members);
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private void givenTask(long owningWorkspaceId) {
    Task task =
        Task.restore(
            TASK_ID, owningWorkspaceId, null, "업무", TaskStatus.WAITING, LocalDate.of(2026, 8, 10),
            null, MEMBER_ID);
    when(taskRepository.findByIdForUpdate(TASK_ID)).thenReturn(Optional.of(task));
  }

  private void givenComment(boolean completed, Long completedBy, LocalDateTime completedAt) {
    TaskComment comment =
        TaskComment.restore(
            COMMENT_ID, TASK_ID, MEMBER_ID, "내용", completed, completedBy, completedAt, List.of(),
            LocalDateTime.of(2026, 8, 6, 9, 0), LocalDateTime.of(2026, 8, 6, 9, 0));
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
  }
}
