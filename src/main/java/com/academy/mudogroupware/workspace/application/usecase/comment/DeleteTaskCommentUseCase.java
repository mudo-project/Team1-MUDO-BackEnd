package com.academy.mudogroupware.workspace.application.usecase.comment;

import com.academy.mudogroupware.workspace.application.command.comment.DeleteTaskCommentCommand;

public interface DeleteTaskCommentUseCase {

  void deleteComment(DeleteTaskCommentCommand command);
}
