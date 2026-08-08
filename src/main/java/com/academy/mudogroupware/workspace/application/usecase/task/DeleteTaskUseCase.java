package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.workspace.application.command.task.DeleteTaskCommand;

public interface DeleteTaskUseCase {

  void deleteTask(DeleteTaskCommand command);
}
