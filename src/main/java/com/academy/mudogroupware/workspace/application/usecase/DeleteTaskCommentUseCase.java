package com.academy.mudogroupware.workspace.application.usecase;

import com.academy.mudogroupware.workspace.application.command.DeleteTaskCommentCommand;

public interface DeleteTaskCommentUseCase {

  void deleteComment(DeleteTaskCommentCommand command);
}
