package com.academy.mudogroupware.workspace.infrastructure.persistence.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.port.MyTaskListQueryPort;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MyTaskListQueryAdapter implements MyTaskListQueryPort {

  private final TaskJpaRepository taskJpaRepository;

  @Override
  public PageResult<MyTaskListItem> findMine(
      Long userId, List<TaskStatus> statuses, Long workspaceId, int page, int size) {
    Slice<TaskJpaRepository.MyTaskRow> slice =
        taskJpaRepository.findMine(userId, statuses, workspaceId, PageRequest.of(page, size));
    List<MyTaskListItem> content = slice.getContent().stream().map(this::toItem).toList();
    return PageResult.of(content, slice.getNumber(), slice.getSize(), slice.hasNext());
  }

  private MyTaskListItem toItem(TaskJpaRepository.MyTaskRow row) {
    return new MyTaskListItem(
        row.getTaskId(),
        row.getWorkspaceId(),
        row.getWorkspaceName(),
        row.getTitle(),
        row.getDueAt(),
        row.getStatus());
  }
}
