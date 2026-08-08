package com.academy.mudogroupware.workspace.application.query.task;

import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;

public record WorkspaceTaskItem(
    Long taskId,
    String title,
    TaskStatus status,
    WorkspaceMemberInfo creator,
    LocalDate dueAt,
    Long completedCommentCount,
    Long commentCount
) {}
