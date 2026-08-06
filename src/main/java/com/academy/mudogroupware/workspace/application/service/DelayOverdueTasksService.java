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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DelayOverdueTasksService implements DelayOverdueTasksUseCase {

  // TODO: Task 도메인 모델/Repository 인터페이스가 아직 없어 JPA Repository에 직접 의존한다.
  // Task 도메인 모델을 만들 때(예: Task CRUD 구현 시) Port/Adapter로 전환한다.
  private final TaskJpaRepository taskJpaRepository;
  private final TaskStatusHistoryJpaRepository taskStatusHistoryJpaRepository;
  private final Clock clock;

  @Override
  public void delayOverdueTasks() {
    LocalDate today = LocalDate.now(clock);

    List<TaskJpaEntity> overdueRegularTasks =
        taskJpaRepository.findOverdueRegularTasks(today, TaskStatus.COMPLETED, TaskStatus.DELAYED);
    List<TaskJpaEntity> overdueRecurringTasks =
        taskJpaRepository.findOverdueRecurringTasks(
            today.atStartOfDay(), TaskStatus.COMPLETED, TaskStatus.DELAYED);

    overdueRegularTasks.forEach(this::transitionToDelayed);
    overdueRecurringTasks.forEach(this::transitionToDelayed);

    log.info(
        "업무 자동 지연 처리 완료: 일반 {}건, 반복 {}건",
        overdueRegularTasks.size(),
        overdueRecurringTasks.size());
  }

  private void transitionToDelayed(TaskJpaEntity task) {
    TaskStatus previousStatus = task.getStatus();
    task.markDelayed();
    taskStatusHistoryJpaRepository.save(
        TaskStatusHistoryJpaEntity.systemChanged(task, previousStatus, TaskStatus.DELAYED));
  }
}
