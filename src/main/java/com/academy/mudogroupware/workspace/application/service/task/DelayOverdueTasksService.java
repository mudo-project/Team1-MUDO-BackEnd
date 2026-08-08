package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.workspace.application.usecase.task.DelayOverdueTasksUseCase;
import com.academy.mudogroupware.workspace.domain.model.task.Task;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatusHistory;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskRepository;
import com.academy.mudogroupware.workspace.domain.repository.task.TaskStatusHistoryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DelayOverdueTasksService implements DelayOverdueTasksUseCase {

  private final TaskRepository taskRepository;
  private final TaskStatusHistoryRepository taskStatusHistoryRepository;
  private final Clock clock;

  @Override
  public void delayOverdueTasks() {
    LocalDate today = LocalDate.now(clock);

    List<Task> overdueRegularTasks = taskRepository.findOverdueRegularTasks(today);
    List<Task> overdueRecurringTasks =
        taskRepository.findOverdueRecurringTasks(today.atStartOfDay());

    overdueRegularTasks.forEach(task -> transitionToDelayed(task, today));
    overdueRecurringTasks.forEach(task -> transitionToDelayed(task, today));

    log.info(
        "업무 자동 지연 처리 완료: 일반 {}건, 반복 {}건",
        overdueRegularTasks.size(),
        overdueRecurringTasks.size());
  }

  private void transitionToDelayed(Task task, LocalDate today) {
    TaskStatus previousStatus = task.getStatus();
    Task delayed = task.changeStatus(TaskStatus.DELAYED, null, today);
    taskRepository.save(delayed);
    taskStatusHistoryRepository.append(
        TaskStatusHistory.systemChanged(delayed.getId(), previousStatus, TaskStatus.DELAYED));
  }
}
