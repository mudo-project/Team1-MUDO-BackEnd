package com.academy.mudogroupware.workspace.application.service;

import com.academy.mudogroupware.workspace.application.usecase.DelayOverdueTasksUseCase;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskJpaRepository;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryJpaEntity;
import com.academy.mudogroupware.workspace.infrastructure.persistence.task.TaskStatusHistoryJpaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DelayOverdueTasksService implements DelayOverdueTasksUseCase {

  private final TaskJpaRepository taskJpaRepository;
  private final TaskStatusHistoryJpaRepository taskStatusHistoryJpaRepository;
  private final Clock clock;

  @Override
  public void delayOverdueTasks() {
    LocalDate today = LocalDate.now(clock);

    List<TaskJpaEntity> overdueRegularTasks =
        taskJpaRepository.findOverdueRegularTasks(today, TaskStatus.COMPLETED, TaskStatus.DELAYED);
    List<TaskJpaEntity> overdueRecurringTasks =
        taskJpaRepository.findOverdueRecurringTasks(today, TaskStatus.COMPLETED, TaskStatus.DELAYED);

    overdueRegularTasks.forEach(this::transitionToDelayed);
    overdueRecurringTasks.forEach(this::transitionToDelayed);
  }

  private void transitionToDelayed(TaskJpaEntity task) {
    TaskStatus previousStatus = task.getStatus();
    task.markDelayed();
    taskStatusHistoryJpaRepository.save(
        TaskStatusHistoryJpaEntity.systemChanged(task, previousStatus, TaskStatus.DELAYED));
  }
}
