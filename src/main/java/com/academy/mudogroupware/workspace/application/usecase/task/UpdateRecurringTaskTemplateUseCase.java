package com.academy.mudogroupware.workspace.application.usecase.task;

import com.academy.mudogroupware.workspace.application.command.task.UpdateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;

public interface UpdateRecurringTaskTemplateUseCase {

  // 변경 반영 후의 템플릿을 반환한다.
  RecurringTaskTemplate update(UpdateRecurringTaskTemplateCommand command);
}
