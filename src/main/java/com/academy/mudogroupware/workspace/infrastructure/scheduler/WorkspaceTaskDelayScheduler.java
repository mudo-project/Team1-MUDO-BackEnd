package com.academy.mudogroupware.workspace.infrastructure.scheduler;

import com.academy.mudogroupware.workspace.application.usecase.DelayOverdueTasksUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkspaceTaskDelayScheduler {

  private final DelayOverdueTasksUseCase delayOverdueTasksUseCase;

  @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
  public void delayOverdueTasks() {
    delayOverdueTasksUseCase.delayOverdueTasks();
  }
}
