package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.UpdateTaskCommand;
import com.academy.mudogroupware.workspace.domain.exception.IllegalTaskDueAtException;
import com.academy.mudogroupware.workspace.domain.exception.TaskDueAtRequiredException;
import com.academy.mudogroupware.workspace.domain.exception.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.InvalidTaskStatusTransitionException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.TaskStatusHistoryRepository;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateTaskServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-04T15:00:00Z"), KST);
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);
  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long TEMPLATE_ID = 100L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;
  private static final LocalDateTime SCHEDULED_FOR = LocalDateTime.of(2026, 8, 5, 9, 0);

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskStatusHistoryRepository taskStatusHistoryRepository;

  @Captor private ArgumentCaptor<Task> taskCaptor;
  @Captor private ArgumentCaptor<TaskStatusHistory> historyCaptor;

  private UpdateTaskService service() {
    return new UpdateTaskService(
        workspaceRepository, taskRepository, taskStatusHistoryRepository, FIXED_CLOCK);
  }

  @Test
  void changesStatusAndSavesHistory() {
    givenWorkspaceWithMember();
    givenTask(TaskStatus.WAITING, TODAY.plusDays(2), WORKSPACE_ID);
    when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Task result =
        service()
            .updateTask(
                new UpdateTaskCommand(
                    WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.IN_PROGRESS, null));

    assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    verify(taskStatusHistoryRepository).append(historyCaptor.capture());
    TaskStatusHistory history = historyCaptor.getValue();
    assertThat(history.getTaskId()).isEqualTo(TASK_ID);
    assertThat(history.getPreviousStatus()).isEqualTo(TaskStatus.WAITING);
    assertThat(history.getCurrentStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(history.getChangedBy()).isEqualTo(MEMBER_ID);
  }

  @Test
  void changesDueAtOnlyWithoutTouchingStatusOrHistory() {
    givenWorkspaceWithMember();
    givenTask(TaskStatus.DELAYED, TODAY.minusDays(1), WORKSPACE_ID);
    when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    Task result =
        service()
            .updateTask(
                new UpdateTaskCommand(WORKSPACE_ID, TASK_ID, MEMBER_ID, null, TODAY.plusDays(3)));

    assertThat(result.getStatus()).isEqualTo(TaskStatus.DELAYED);
    assertThat(result.getDueAt()).isEqualTo(TODAY.plusDays(3));
    verify(taskStatusHistoryRepository, never()).append(any());
  }

  @Test
  void savesNoHistoryWhenStatusIsUnchanged() {
    givenWorkspaceWithMember();
    givenTask(TaskStatus.IN_PROGRESS, TODAY.plusDays(2), WORKSPACE_ID);
    when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service()
        .updateTask(
            new UpdateTaskCommand(WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.IN_PROGRESS, null));

    verify(taskStatusHistoryRepository, never()).append(any());
  }

  @Test
  void appliesNewDueAtWhenReopeningPastDueTask() {
    givenWorkspaceWithMember();
    givenTask(TaskStatus.DELAYED, TODAY.minusDays(1), WORKSPACE_ID);
    when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service()
        .updateTask(
            new UpdateTaskCommand(
                WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.IN_PROGRESS, TODAY.plusDays(5)));

    verify(taskRepository).save(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    assertThat(taskCaptor.getValue().getDueAt()).isEqualTo(TODAY.plusDays(5));
  }

  @Test
  void rejectsReopeningPastDueTaskWithoutNewDueAt() {
    givenWorkspaceWithMember();
    givenTask(TaskStatus.DELAYED, TODAY.minusDays(1), WORKSPACE_ID);

    assertThatThrownBy(
            () ->
                service()
                    .updateTask(
                        new UpdateTaskCommand(
                            WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.IN_PROGRESS, null)))
        .isInstanceOf(TaskDueAtRequiredException.class);

    verify(taskRepository, never()).save(any());
    verify(taskStatusHistoryRepository, never()).append(any());
  }

  @Test
  void rejectsCompletedToDelayed() {
    givenWorkspaceWithMember();
    givenTask(TaskStatus.COMPLETED, TODAY.plusDays(2), WORKSPACE_ID);

    assertThatThrownBy(
            () ->
                service()
                    .updateTask(
                        new UpdateTaskCommand(
                            WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.DELAYED, null)))
        .isInstanceOf(InvalidTaskStatusTransitionException.class);
  }

  @Test
  void rejectsMissingWorkspaceBeforeLoadingTask() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .updateTask(
                        new UpdateTaskCommand(
                            WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.COMPLETED, null)))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(taskRepository, never()).findByIdForUpdate(any(), any());
  }

  @Test
  void rejectsNonMemberBeforeLoadingTask() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () ->
                service()
                    .updateTask(
                        new UpdateTaskCommand(
                            WORKSPACE_ID, TASK_ID, OUTSIDER_ID, TaskStatus.COMPLETED, null)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(taskRepository, never()).findByIdForUpdate(any(), any());
  }

  @Test
  void rejectsMissingTask() {
    givenWorkspaceWithMember();
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .updateTask(
                        new UpdateTaskCommand(
                            WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.COMPLETED, null)))
        .isInstanceOf(TaskNotFoundException.class);
  }

  // --- 반복 업무 ---

  @Test
  void rejectsDueAtChangeOnRecurringTask() {
    givenWorkspaceWithMember();
    givenRecurringTask(TaskStatus.WAITING, WORKSPACE_ID);

    assertThatThrownBy(
            () ->
                service()
                    .updateTask(
                        new UpdateTaskCommand(
                            WORKSPACE_ID, TASK_ID, MEMBER_ID, null, TODAY.plusDays(5))))
        .isInstanceOf(IllegalTaskDueAtException.class);

    verify(taskRepository, never()).save(any());
  }

  @Test
  void changesRecurringTaskStatusWithoutDueAt() {
    givenWorkspaceWithMember();
    givenRecurringTask(TaskStatus.DELAYED, WORKSPACE_ID);
    when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service()
        .updateTask(
            new UpdateTaskCommand(
                WORKSPACE_ID, TASK_ID, MEMBER_ID, TaskStatus.IN_PROGRESS, null));

    verify(taskRepository).save(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace =
        Workspace.restore(WORKSPACE_ID, 1L, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private void givenTask(TaskStatus status, LocalDate dueAt, long owningWorkspaceId) {
    Task task =
        Task.restore(TASK_ID, owningWorkspaceId, null, "업무", status, dueAt, null, MEMBER_ID);
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }

  private void givenRecurringTask(TaskStatus status, long owningWorkspaceId) {
    Task task =
        Task.restore(
            TASK_ID, owningWorkspaceId, TEMPLATE_ID, "반복 업무", status, null, SCHEDULED_FOR,
            MEMBER_ID);
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }
}
