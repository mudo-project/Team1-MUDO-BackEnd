package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.domain.model.RecurrenceType;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.RecurringTaskTemplateJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
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

  @Mock private TaskJpaRepository taskJpaRepository;
  @Mock private TaskStatusHistoryJpaRepository taskStatusHistoryJpaRepository;

  @Captor private ArgumentCaptor<TaskStatusHistoryJpaEntity> historyCaptor;

  private DelayOverdueTasksService service() {
    return new DelayOverdueTasksService(taskJpaRepository, taskStatusHistoryJpaRepository, FIXED_CLOCK);
  }

  @Test
  void delaysOverdueRegularAndRecurringTasksAndSavesOneHistoryEach() {
    LocalDate today = LocalDate.now(FIXED_CLOCK);
    assertThat(today).isEqualTo(LocalDate.of(2026, 8, 5));

    TaskJpaEntity regular =
        TaskJpaEntity.create(null, null, "정기", 10L, TaskStatus.IN_PROGRESS, today.minusDays(1), null);
    // 실제 반복 업무 계약(recurringTemplate != null, 과거 scheduledFor)을 그대로 갖춘 fixture
    RecurringTaskTemplateJpaEntity template =
        RecurringTaskTemplateJpaEntity.create(null, "daily", RecurrenceType.DAILY, Map.of(), 10L);
    TaskJpaEntity recurring =
        TaskJpaEntity.create(
            null, template, "반복", 10L, TaskStatus.WAITING, null, today.minusDays(1).atTime(9, 0));
    when(taskJpaRepository.findOverdueRegularTasks(eq(today), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of(regular));
    when(taskJpaRepository.findOverdueRecurringTasks(
            eq(today.atStartOfDay()), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of(recurring));

    service().delayOverdueTasks();

    assertThat(regular.getStatus()).isEqualTo(TaskStatus.DELAYED);
    assertThat(recurring.getStatus()).isEqualTo(TaskStatus.DELAYED);
    verify(taskStatusHistoryJpaRepository, times(2)).save(historyCaptor.capture());

    List<TaskStatusHistoryJpaEntity> savedHistories = historyCaptor.getAllValues();
    assertThat(savedHistories)
        .allSatisfy(
            history -> {
              assertThat(history.getCurrentStatus()).isEqualTo(TaskStatus.DELAYED);
              assertThat(history.getChangedBy()).isNull();
            });
    assertThat(savedHistories)
        .filteredOn(history -> history.getTask() == regular)
        .extracting(TaskStatusHistoryJpaEntity::getPreviousStatus)
        .containsExactly(TaskStatus.IN_PROGRESS);
    assertThat(savedHistories)
        .filteredOn(history -> history.getTask() == recurring)
        .extracting(TaskStatusHistoryJpaEntity::getPreviousStatus)
        .containsExactly(TaskStatus.WAITING);
  }

  @Test
  void savesNoHistoryWhenThereAreNoOverdueTasks() {
    LocalDate today = LocalDate.now(FIXED_CLOCK);
    when(taskJpaRepository.findOverdueRegularTasks(eq(today), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of());
    when(taskJpaRepository.findOverdueRecurringTasks(
            eq(today.atStartOfDay()), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of());

    service().delayOverdueTasks();

    verify(taskStatusHistoryJpaRepository, never()).save(any());
  }
}
