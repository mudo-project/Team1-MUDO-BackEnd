package com.academy.mudogroupware.workspace.infrastructure.scheduler;

import com.academy.mudogroupware.workspace.application.usecase.task.GenerateRecurringTasksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecurringTaskScheduler {

  private final GenerateRecurringTasksUseCase generateRecurringTasksUseCase;

  @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
  public void generateRecurringTasks() {
    generateRecurringTasksUseCase.generateRecurringTasks();
  }
}
