package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.domain.model.TaskComment;

public interface CreateTaskCommentUseCase {

  TaskComment createComment(CreateTaskCommentCommand command);
}
