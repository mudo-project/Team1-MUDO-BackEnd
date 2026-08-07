package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.UpdateTaskCommand;
import com.academy.mudogroupware.workspace.domain.model.Task;

public interface UpdateTaskUseCase {

  // 반영된 업무를 반환한다.
  Task updateTask(UpdateTaskCommand command);
}
