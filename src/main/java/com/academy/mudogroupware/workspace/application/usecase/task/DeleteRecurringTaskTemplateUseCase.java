package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.workspace.application.command.task.DeleteRecurringTaskTemplateCommand;

public interface DeleteRecurringTaskTemplateUseCase {

  void delete(DeleteRecurringTaskTemplateCommand command);
}
