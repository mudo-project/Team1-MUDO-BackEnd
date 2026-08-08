package com.academy.mudogroupware.workspace.application.usecase.comment;

import com.academy.mudogroupware.workspace.application.command.comment.UpdateTaskCommentCommand;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;

public interface UpdateTaskCommentUseCase {

  TaskComment updateComment(UpdateTaskCommentCommand command);
}
