package com.academy.mudogroupware.workspace.application.query.comment;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import java.time.LocalDateTime;

public record TaskCommentListItem(
    Long commentId, String content, WorkspaceMemberInfo author, boolean completed, LocalDateTime createdAt) {}
