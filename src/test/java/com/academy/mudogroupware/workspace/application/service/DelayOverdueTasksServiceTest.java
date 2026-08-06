package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DelayOverdueTasksServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-05T00:00:00Z"), KST);

  @Mock private TaskJpaRepository taskJpaRepository;
  @Mock private TaskStatusHistoryJpaRepository taskStatusHistoryJpaRepository;

  private DelayOverdueTasksService service() {
    return new DelayOverdueTasksService(taskJpaRepository, taskStatusHistoryJpaRepository, FIXED_CLOCK);
  }

  @Test
  void delaysOverdueRegularAndRecurringTasksAndSavesOneHistoryEach() {
    LocalDate today = LocalDate.now(FIXED_CLOCK);
    TaskJpaEntity regular =
        TaskJpaEntity.create(null, null, "정기", 10L, TaskStatus.IN_PROGRESS, today.minusDays(1), null);
    TaskJpaEntity recurring =
        TaskJpaEntity.create(null, null, "반복", 10L, TaskStatus.WAITING, null, null);
    when(taskJpaRepository.findOverdueRegularTasks(eq(today), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of(regular));
    when(taskJpaRepository.findOverdueRecurringTasks(eq(today), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of(recurring));

    service().delayOverdueTasks();

    assertThat(regular.getStatus()).isEqualTo(TaskStatus.DELAYED);
    assertThat(recurring.getStatus()).isEqualTo(TaskStatus.DELAYED);
    verify(taskStatusHistoryJpaRepository, times(2)).save(any(TaskStatusHistoryJpaEntity.class));
  }

  @Test
  void savesNoHistoryWhenThereAreNoOverdueTasks() {
    LocalDate today = LocalDate.now(FIXED_CLOCK);
    when(taskJpaRepository.findOverdueRegularTasks(eq(today), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of());
    when(taskJpaRepository.findOverdueRecurringTasks(eq(today), eq(TaskStatus.COMPLETED), eq(TaskStatus.DELAYED)))
        .thenReturn(List.of());

    service().delayOverdueTasks();

    verify(taskStatusHistoryJpaRepository, never()).save(any());
  }
}
