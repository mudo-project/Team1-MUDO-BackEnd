package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.repository.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.TaskStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelayOverdueTasksServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  // UTC 2026-08-04T15:00:00Z == KST 2026-08-05T00:00:00+09:00: UTC is still on Aug 4
  // while KST has already rolled over to Aug 5, so this actually exercises the
  // KST day-boundary instead of coincidentally agreeing with a UTC-naive LocalDate.now().
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-04T15:00:00Z"), KST);
  private static final long WORKSPACE_ID = 1L;
  private static final long CREATOR_ID = 10L;

  @Mock private TaskRepository taskRepository;
  @Mock private TaskStatusHistoryRepository taskStatusHistoryRepository;

  @Captor private ArgumentCaptor<Task> taskCaptor;
  @Captor private ArgumentCaptor<TaskStatusHistory> historyCaptor;

  private DelayOverdueTasksService service() {
    return new DelayOverdueTasksService(taskRepository, taskStatusHistoryRepository, FIXED_CLOCK);
  }

  @Test
  void delaysOverdueRegularAndRecurringTasksAndSavesOneHistoryEach() {
    LocalDate today = LocalDate.now(FIXED_CLOCK);
    assertThat(today).isEqualTo(LocalDate.of(2026, 8, 5));

    Task regular =
        Task.restore(1L, WORKSPACE_ID, null, "정기", TaskStatus.IN_PROGRESS, today.minusDays(1),
            null, CREATOR_ID);
    Task recurring =
        Task.restore(2L, WORKSPACE_ID, 100L, "반복", TaskStatus.WAITING, null,
            today.minusDays(1).atTime(9, 0), CREATOR_ID);
    when(taskRepository.findOverdueRegularTasks(eq(today))).thenReturn(List.of(regular));
    when(taskRepository.findOverdueRecurringTasks(eq(today.atStartOfDay())))
        .thenReturn(List.of(recurring));

    service().delayOverdueTasks();

    verify(taskRepository, times(2)).save(taskCaptor.capture());
    assertThat(taskCaptor.getAllValues())
        .extracting(Task::getStatus)
        .containsOnly(TaskStatus.DELAYED);

    verify(taskStatusHistoryRepository, times(2)).append(historyCaptor.capture());
    List<TaskStatusHistory> savedHistories = historyCaptor.getAllValues();
    assertThat(savedHistories)
        .allSatisfy(
            history -> {
              assertThat(history.getCurrentStatus()).isEqualTo(TaskStatus.DELAYED);
              assertThat(history.getChangedBy()).isNull();
            });
    assertThat(savedHistories)
        .filteredOn(history -> history.getTaskId().equals(1L))
        .extracting(TaskStatusHistory::getPreviousStatus)
        .containsExactly(TaskStatus.IN_PROGRESS);
    assertThat(savedHistories)
        .filteredOn(history -> history.getTaskId().equals(2L))
        .extracting(TaskStatusHistory::getPreviousStatus)
        .containsExactly(TaskStatus.WAITING);
  }

  @Test
  void savesNoHistoryWhenThereAreNoOverdueTasks() {
    LocalDate today = LocalDate.now(FIXED_CLOCK);
    when(taskRepository.findOverdueRegularTasks(eq(today))).thenReturn(List.of());
    when(taskRepository.findOverdueRecurringTasks(eq(today.atStartOfDay()))).thenReturn(List.of());

    service().delayOverdueTasks();

    verify(taskRepository, never()).save(any());
    verify(taskStatusHistoryRepository, never()).append(any());
  }
}
