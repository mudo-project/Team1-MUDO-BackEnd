package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.UpdateTaskCommentCommand;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;

public interface UpdateTaskCommentUseCase {

  TaskComment updateComment(UpdateTaskCommentCommand command);
}
