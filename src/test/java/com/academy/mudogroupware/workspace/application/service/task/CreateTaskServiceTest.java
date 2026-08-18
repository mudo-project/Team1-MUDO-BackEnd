package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.task.CreateTaskCommand;
import com.academy.mudogroupware.workspace.domain.event.TaskCreatedEvent;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
class CreateTaskServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  // UTC 2026-08-04T15:00:00Z == KST 2026-08-05T00:00:00+09:00 — KST 자정 경계를 실제로 넘긴다.
  private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-08-04T15:00:00Z"), KST);
  private static final LocalDate TODAY = LocalDate.of(2026, 8, 5);
  private static final long WORKSPACE_ID = 1L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private TaskStatusHistoryRepository taskStatusHistoryRepository;
  @Mock private ApplicationEventPublisher applicationEventPublisher;

  @Captor private ArgumentCaptor<Task> taskCaptor;
  @Captor private ArgumentCaptor<TaskStatusHistory> historyCaptor;
  @Captor private ArgumentCaptor<TaskCreatedEvent> taskCreatedEventCaptor;

  private CreateTaskService service() {
    return new CreateTaskService(
        workspaceRepository,
        taskRepository,
        taskStatusHistoryRepository,
        applicationEventPublisher,
        FIXED_CLOCK);
  }

  @Test
  void createsWaitingTaskAndSavesInitialHistory() {
    givenWorkspaceWithMember();
    when(taskRepository.save(any())).thenReturn(savedTask(TaskStatus.WAITING, TODAY.plusDays(2)));

    Long taskId =
        service().createTask(new CreateTaskCommand(WORKSPACE_ID, MEMBER_ID, "새 업무", TODAY.plusDays(2)));

    assertThat(taskId).isEqualTo(101L);
    verify(taskRepository).save(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.WAITING);

    verify(taskStatusHistoryRepository).append(historyCaptor.capture());
    TaskStatusHistory history = historyCaptor.getValue();
    assertThat(history.getTaskId()).isEqualTo(101L);
    assertThat(history.getPreviousStatus()).isNull();
    assertThat(history.getCurrentStatus()).isEqualTo(TaskStatus.WAITING);
    assertThat(history.getChangedBy()).isEqualTo(MEMBER_ID);

    verify(applicationEventPublisher).publishEvent(taskCreatedEventCaptor.capture());
    TaskCreatedEvent published = taskCreatedEventCaptor.getValue();
    assertThat(published.workspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(published.taskId()).isEqualTo(101L);
  }

  @Test
  void createsDelayedTaskWhenDueAtIsPast() {
    givenWorkspaceWithMember();
    when(taskRepository.save(any())).thenReturn(savedTask(TaskStatus.DELAYED, TODAY.minusDays(1)));

    service().createTask(new CreateTaskCommand(WORKSPACE_ID, MEMBER_ID, "지난 업무", TODAY.minusDays(1)));

    verify(taskRepository).save(taskCaptor.capture());
    assertThat(taskCaptor.getValue().getStatus()).isEqualTo(TaskStatus.DELAYED);

    verify(taskStatusHistoryRepository).append(historyCaptor.capture());
    assertThat(historyCaptor.getValue().getCurrentStatus()).isEqualTo(TaskStatus.DELAYED);
    assertThat(historyCaptor.getValue().getPreviousStatus()).isNull();
  }

  @Test
  void rejectsMissingWorkspaceBeforeCheckingMembership() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service().createTask(new CreateTaskCommand(WORKSPACE_ID, OUTSIDER_ID, "업무", TODAY)))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(taskRepository, never()).save(any());
    verify(taskStatusHistoryRepository, never()).append(any());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () -> service().createTask(new CreateTaskCommand(WORKSPACE_ID, OUTSIDER_ID, "업무", TODAY)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(taskRepository, never()).save(any());
    verify(taskStatusHistoryRepository, never()).append(any());
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace =
        Workspace.restore(WORKSPACE_ID, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private Task savedTask(TaskStatus status, LocalDate dueAt) {
    return Task.restore(101L, WORKSPACE_ID, null, "새 업무", status, dueAt, null, MEMBER_ID);
  }
}
