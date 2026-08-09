package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskSkipRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskTemplateRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
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
class GenerateRecurringTasksServiceTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  // KST 2026-08-10T00:05:00+09:00 (월요일)
  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-09T15:05:00Z"), KST);
  private static final long WORKSPACE_ID = 1L;
  private static final long TEMPLATE_ID = 100L;
  private static final long CREATOR_ID = 10L;
  private static final LocalDateTime SCHEDULED_FOR = LocalDateTime.of(2026, 8, 10, 0, 0);

  @Mock private RecurringTaskTemplateRepository recurringTaskTemplateRepository;
  @Mock private TaskRepository taskRepository;
  @Mock private RecurringTaskSkipRepository recurringTaskSkipRepository;
  @Mock private TaskStatusHistoryRepository taskStatusHistoryRepository;

  @Captor private ArgumentCaptor<Task> taskCaptor;
  @Captor private ArgumentCaptor<TaskStatusHistory> historyCaptor;

  private GenerateRecurringTasksService service() {
    return new GenerateRecurringTasksService(
        recurringTaskTemplateRepository,
        taskRepository,
        recurringTaskSkipRepository,
        taskStatusHistoryRepository,
        FIXED_CLOCK);
  }

  private RecurringTaskTemplate mondayTemplate() {
    // 월요일(daysOfWeek=1)마다 발생 — FIXED_CLOCK 기준 2026-08-10은 월요일.
    return RecurringTaskTemplate.restore(
        TEMPLATE_ID, WORKSPACE_ID, "주간 정리", RecurrenceType.WEEKLY,
        Map.of("daysOfWeek", List.of(1)), CREATOR_ID);
  }

  @Test
  void generatesTaskWhenDueAndNotYetCreatedOrSkipped() {
    when(recurringTaskTemplateRepository.findAll()).thenReturn(List.of(mondayTemplate()));
    when(taskRepository.existsByRecurringTemplateIdAndScheduledFor(TEMPLATE_ID, SCHEDULED_FOR))
        .thenReturn(false);
    when(recurringTaskSkipRepository.exists(TEMPLATE_ID, SCHEDULED_FOR)).thenReturn(false);
    when(taskRepository.save(any(Task.class)))
        .thenAnswer(
            invocation -> {
              Task passed = invocation.getArgument(0);
              return Task.restore(
                  999L, passed.getWorkspaceId(), passed.getRecurringTemplateId(),
                  passed.getTitle(), passed.getStatus(), passed.getDueAt(),
                  passed.getScheduledFor(), passed.getCreatedBy());
            });

    service().generateRecurringTasks();

    verify(taskRepository).save(taskCaptor.capture());
    Task saved = taskCaptor.getValue();
    assertThat(saved.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
    assertThat(saved.getRecurringTemplateId()).isEqualTo(TEMPLATE_ID);
    assertThat(saved.getTitle()).isEqualTo("주간 정리");
    assertThat(saved.getStatus()).isEqualTo(TaskStatus.WAITING);
    assertThat(saved.getScheduledFor()).isEqualTo(SCHEDULED_FOR);
    assertThat(saved.getCreatedBy()).isEqualTo(CREATOR_ID);

    verify(taskStatusHistoryRepository).append(historyCaptor.capture());
    TaskStatusHistory history = historyCaptor.getValue();
    assertThat(history.getTaskId()).isEqualTo(999L);
    assertThat(history.getPreviousStatus()).isNull();
    assertThat(history.getCurrentStatus()).isEqualTo(TaskStatus.WAITING);
    assertThat(history.getChangedBy()).isNull();
  }

  @Test
  void skipsTemplateNotDueToday() {
    RecurringTaskTemplate tuesdayTemplate =
        RecurringTaskTemplate.restore(
            TEMPLATE_ID, WORKSPACE_ID, "화요일 정리", RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(2)), CREATOR_ID);
    when(recurringTaskTemplateRepository.findAll()).thenReturn(List.of(tuesdayTemplate));

    service().generateRecurringTasks();

    verify(taskRepository, never()).save(any());
    verify(taskStatusHistoryRepository, never()).append(any());
  }

  @Test
  void skipsWhenTaskAlreadyGenerated() {
    when(recurringTaskTemplateRepository.findAll()).thenReturn(List.of(mondayTemplate()));
    when(taskRepository.existsByRecurringTemplateIdAndScheduledFor(TEMPLATE_ID, SCHEDULED_FOR))
        .thenReturn(true);

    service().generateRecurringTasks();

    verify(taskRepository, never()).save(any());
    verify(recurringTaskSkipRepository, never()).exists(any(), any());
  }

  @Test
  void skipsWhenOccurrenceWasDeletedAndMarkedSkip() {
    when(recurringTaskTemplateRepository.findAll()).thenReturn(List.of(mondayTemplate()));
    when(taskRepository.existsByRecurringTemplateIdAndScheduledFor(TEMPLATE_ID, SCHEDULED_FOR))
        .thenReturn(false);
    when(recurringTaskSkipRepository.exists(TEMPLATE_ID, SCHEDULED_FOR)).thenReturn(true);

    service().generateRecurringTasks();

    verify(taskRepository, never()).save(any());
  }

  @Test
  void generatesNothingWhenNoTemplatesExist() {
    when(recurringTaskTemplateRepository.findAll()).thenReturn(List.of());

    service().generateRecurringTasks();

    verify(taskRepository, never()).save(any());
    verify(taskStatusHistoryRepository, never()).append(any());
  }
}
