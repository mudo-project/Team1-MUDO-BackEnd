package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.workspace.application.command.task.UpdateTaskCommand;
import com.academy.mudogroupware.workspace.domain.model.task.Task;

public interface UpdateTaskUseCase {

  // 반영된 업무를 반환한다.
  Task updateTask(UpdateTaskCommand command);
}
