package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.task.DeleteTaskCommand;
import com.academy.mudogroupware.workspace.domain.event.TaskDeletedEvent;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskSkipRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class DeleteTaskServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long TASK_ID = 101L;
  private static final long TEMPLATE_ID = 100L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;
  private static final LocalDateTime SCHEDULED_FOR = LocalDateTime.of(2026, 8, 5, 9, 0);

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private RecurringTaskSkipRepository recurringTaskSkipRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Captor private ArgumentCaptor<TaskDeletedEvent> taskDeletedEventCaptor;

  private DeleteTaskService service() {
    return new DeleteTaskService(
        workspaceRepository, taskRepository, recurringTaskSkipRepository, applicationEventPublisher);
  }

  @Test
  void deletesRegularTaskWithoutSkipRecord() {
    givenWorkspaceWithMember();
    givenRegularTask(WORKSPACE_ID);

    service().deleteTask(new DeleteTaskCommand(WORKSPACE_ID, TASK_ID, MEMBER_ID));

    verify(taskRepository).delete(TASK_ID);
    verifyNoInteractions(recurringTaskSkipRepository);

    verify(applicationEventPublisher).publishEvent(taskDeletedEventCaptor.capture());
    assertThat(taskDeletedEventCaptor.getValue().workspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(taskDeletedEventCaptor.getValue().taskId()).isEqualTo(TASK_ID);
  }

  @Test
  void deletesRecurringTaskAndRecordsSkip() {
    givenWorkspaceWithMember();
    givenRecurringTask(WORKSPACE_ID);

    service().deleteTask(new DeleteTaskCommand(WORKSPACE_ID, TASK_ID, MEMBER_ID));

    verify(recurringTaskSkipRepository).saveIfAbsent(eq(TEMPLATE_ID), eq(SCHEDULED_FOR));
    verify(taskRepository).delete(TASK_ID);
  }

  @Test
  void rejectsMissingWorkspaceBeforeLoadingTask() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service().deleteTask(new DeleteTaskCommand(WORKSPACE_ID, TASK_ID, MEMBER_ID)))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(taskRepository, never()).findByIdForUpdate(any(), any());
    verify(taskRepository, never()).delete(any());
  }

  @Test
  void rejectsNonMemberBeforeLoadingTask() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () -> service().deleteTask(new DeleteTaskCommand(WORKSPACE_ID, TASK_ID, OUTSIDER_ID)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(taskRepository, never()).findByIdForUpdate(any(), any());
    verify(taskRepository, never()).delete(any());
  }

  @Test
  void rejectsMissingTask() {
    givenWorkspaceWithMember();
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service().deleteTask(new DeleteTaskCommand(WORKSPACE_ID, TASK_ID, MEMBER_ID)))
        .isInstanceOf(TaskNotFoundException.class);

    verify(taskRepository, never()).delete(any());
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace =
        Workspace.restore(WORKSPACE_ID, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private void givenRegularTask(long owningWorkspaceId) {
    Task task =
        Task.restore(
            TASK_ID, owningWorkspaceId, null, "일반 업무", TaskStatus.WAITING,
            LocalDate.of(2026, 8, 10), null, MEMBER_ID);
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }

  private void givenRecurringTask(long owningWorkspaceId) {
    Task task =
        Task.restore(
            TASK_ID, owningWorkspaceId, TEMPLATE_ID, "반복 업무", TaskStatus.WAITING, null,
            SCHEDULED_FOR, MEMBER_ID);
    when(taskRepository.findByIdForUpdate(WORKSPACE_ID, TASK_ID)).thenReturn(Optional.of(task));
  }
}
