package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.domain.exception.InvalidMentionedUserException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class CreateTaskCommentServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-07T01:00:00Z"), KST);
  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long MEMBER_ID = 10L;
  private static final long MENTIONED_MEMBER_ID = 11L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskCommentRepository taskCommentRepository;

  @Captor private ArgumentCaptor<TaskComment> commentCaptor;

  private CreateTaskCommentService service() {
    return new CreateTaskCommentService(
        workspaceRepository, taskRepository, taskCommentRepository, FIXED_CLOCK);
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
}
