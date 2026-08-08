package com.academy.mudogroupware.workspace.application.usecase.comment;

import com.academy.mudogroupware.workspace.application.command.comment.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;

public interface ToggleTaskCommentCompleteUseCase {

  TaskComment toggleComplete(ToggleTaskCommentCompleteCommand command);
}
