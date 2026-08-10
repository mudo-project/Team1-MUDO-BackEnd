package com.academy.mudogroupware.workspace.application.service.task;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.port.MyTaskListQueryPort;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.application.usecase.task.MyTaskListQueryUseCase;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MyTaskListQueryService implements MyTaskListQueryUseCase {

  // 이 API는 완료된 업무를 절대 노출하지 않는다 — status가 없거나 COMPLETED로 들어와도 이 3개로 대체한다.
  private static final List<TaskStatus> DEFAULT_STATUSES =
      List.of(TaskStatus.WAITING, TaskStatus.IN_PROGRESS, TaskStatus.DELAYED);

  private final MyTaskListQueryPort myTaskListQueryPort;

  @Override
  public PageResult<MyTaskListItem> getMyTasks(
      Long requesterId, TaskStatus status, Long workspaceId, int page, int size) {
    List<TaskStatus> statuses = resolveStatuses(status);
    return myTaskListQueryPort.findMine(requesterId, statuses, workspaceId, page, size);
  }

  private List<TaskStatus> resolveStatuses(TaskStatus status) {
    if (status == null || status == TaskStatus.COMPLETED) {
      return DEFAULT_STATUSES;
    }
    return List.of(status);
  }
}
