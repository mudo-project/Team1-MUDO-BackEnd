package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.DeleteTaskCommand;

public interface DeleteTaskUseCase {

  void deleteTask(DeleteTaskCommand command);
}
