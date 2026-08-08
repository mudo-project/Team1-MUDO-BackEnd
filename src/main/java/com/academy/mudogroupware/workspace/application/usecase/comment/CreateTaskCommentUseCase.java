package com.academy.mudogroupware.workspace.application.usecase.comment;

import com.academy.mudogroupware.workspace.application.command.comment.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;

public interface CreateTaskCommentUseCase {

  TaskComment createComment(CreateTaskCommentCommand command);
}
