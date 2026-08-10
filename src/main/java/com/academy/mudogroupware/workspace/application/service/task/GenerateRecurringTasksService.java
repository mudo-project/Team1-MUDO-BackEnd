package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.global.infrastructure.logging.AfterCommitLogger;
import com.academy.mudogroupware.workspace.application.usecase.task.GenerateRecurringTasksUseCase;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskSkipRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskTemplateRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class GenerateRecurringTasksService implements GenerateRecurringTasksUseCase {

  private final RecurringTaskTemplateRepository recurringTaskTemplateRepository;
  private final TaskRepository taskRepository;
  private final RecurringTaskSkipRepository recurringTaskSkipRepository;
  private final TaskStatusHistoryRepository taskStatusHistoryRepository;
  private final Clock clock;

  @Override
  public void generateRecurringTasks() {
    LocalDate today = LocalDate.now(clock);
    LocalDateTime scheduledFor = today.atStartOfDay();

    List<RecurringTaskTemplate> templates = recurringTaskTemplateRepository.findAll();

    int generatedCount = 0;
    for (RecurringTaskTemplate template : templates) {
      if (generateIfDue(template, today, scheduledFor)) {
        generatedCount++;
      }
    }

    int finalCount = generatedCount;
    AfterCommitLogger.run(
        () -> log.info("event=recurring_task_generate_완료 생성건수={}", finalCount));
  }

  private boolean generateIfDue(
      RecurringTaskTemplate template, LocalDate today, LocalDateTime scheduledFor) {
    if (!template.isDueOn(today)) {
      return false;
    }
    if (taskRepository.existsByRecurringTemplateIdAndScheduledFor(template.getId(), scheduledFor)) {
      return false;
    }
    if (recurringTaskSkipRepository.exists(template.getId(), scheduledFor)) {
      return false;
    }

    Task saved =
        taskRepository.save(
            Task.createRecurring(
                template.getWorkspaceId(),
                template.getId(),
                template.getTitle(),
                scheduledFor,
                template.getCreatedBy()));
    taskStatusHistoryRepository.append(
        TaskStatusHistory.systemChanged(saved.getId(), null, TaskStatus.WAITING));
    return true;
  }
}
