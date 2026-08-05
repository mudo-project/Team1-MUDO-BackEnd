package com.academy.mudogroupware.workspace.application.query;

import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import java.time.LocalDate;

public record WorkspaceTaskItem(
    Long taskId,
    String title,
    TaskStatus status,
    WorkspaceMemberInfo creator,
    LocalDate dueAt,
    Long completedCommentCount,
    Long commentCount) {}
