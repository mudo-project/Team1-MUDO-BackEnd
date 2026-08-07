package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.DeleteTaskCommentCommand;
import com.academy.mudogroupware.workspace.domain.exception.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.TaskCommentRepository;
import com.academy.mudogroupware.workspace.domain.repository.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteTaskCommentServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long OTHER_TASK_ID = 102L;
  private static final long COMMENT_ID = 501L;
  private static final long MEMBER_ID = 10L;
  private static final long OTHER_MEMBER_ID = 11L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskCommentRepository taskCommentRepository;

  private DeleteTaskCommentService service() {
    return new DeleteTaskCommentService(workspaceRepository, taskRepository, taskCommentRepository);
  }

  @Test
  void anyMemberCanDeleteComment() {
    givenWorkspaceWithMembers(MEMBER_ID, OTHER_MEMBER_ID);
    givenTask(WORKSPACE_ID);
    givenComment(TASK_ID, MEMBER_ID);

    service()
        .deleteComment(new DeleteTaskCommentCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, OTHER_MEMBER_ID));

    verify(taskCommentRepository).deleteById(COMMENT_ID);
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .deleteComment(new DeleteTaskCommentCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID)))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(taskCommentRepository, never()).deleteById(any());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMembers(MEMBER_ID);

    assertThatThrownBy(
            () ->
                service()
                    .deleteComment(new DeleteTaskCommentCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, OUTSIDER_ID)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(taskCommentRepository, never()).deleteById(any());
  }

  @Test
  void rejectsMissingTask() {
    givenWorkspaceWithMembers(MEMBER_ID);
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .deleteComment(new DeleteTaskCommentCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID)))
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
                    .deleteComment(new DeleteTaskCommentCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID)))
        .isInstanceOf(TaskCommentNotFoundException.class);

    verify(taskCommentRepository, never()).deleteById(any());
  }

  @Test
  void rejectsMissingComment() {
    givenWorkspaceWithMembers(MEMBER_ID);
    givenTask(WORKSPACE_ID);
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .deleteComment(new DeleteTaskCommentCommand(WORKSPACE_ID, TASK_ID, COMMENT_ID, MEMBER_ID)))
        .isInstanceOf(TaskCommentNotFoundException.class);

    verify(taskCommentRepository, never()).deleteById(any());
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
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }

  private void givenComment(long owningTaskId, long authorId) {
    TaskComment comment =
        TaskComment.restore(
            COMMENT_ID, owningTaskId, authorId, "내용", false, null, null, List.of(),
            LocalDateTime.of(2026, 8, 7, 9, 0), LocalDateTime.of(2026, 8, 7, 9, 0));
    when(taskCommentRepository.findById(COMMENT_ID)).thenReturn(Optional.of(comment));
  }
}
