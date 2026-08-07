package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;

public interface ToggleTaskCommentCompleteUseCase {

  TaskComment toggleComplete(ToggleTaskCommentCompleteCommand command);
}
