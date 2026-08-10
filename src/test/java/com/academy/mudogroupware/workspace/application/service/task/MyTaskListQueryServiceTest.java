package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.workspace.application.port.MyTaskListQueryPort;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyTaskListQueryServiceTest {

  private static final long REQUESTER_ID = 10L;
  private static final List<TaskStatus> DEFAULT_STATUSES =
      List.of(TaskStatus.WAITING, TaskStatus.IN_PROGRESS, TaskStatus.DELAYED);

  @Mock private MyTaskListQueryPort myTaskListQueryPort;

  private MyTaskListQueryService service() {
    return new MyTaskListQueryService(myTaskListQueryPort);
  }

  @Test
  void usesDefaultThreeStatusesWhenStatusFilterIsNull() {
    PageResult<MyTaskListItem> expected = PageResult.of(List.of(), 0, 20, false);
    when(myTaskListQueryPort.findMine(REQUESTER_ID, DEFAULT_STATUSES, null, 0, 20))
        .thenReturn(expected);

    PageResult<MyTaskListItem> result = service().getMyTasks(REQUESTER_ID, null, null, 0, 20);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void usesDefaultThreeStatusesWhenStatusFilterIsCompleted() {
    // COMPLETED는 이 기능에서 절대 노출하지 않기로 했으므로, 요청되어도 기본 3개로 대체한다.
    PageResult<MyTaskListItem> expected = PageResult.of(List.of(), 0, 20, false);
    when(myTaskListQueryPort.findMine(REQUESTER_ID, DEFAULT_STATUSES, null, 0, 20))
        .thenReturn(expected);

    PageResult<MyTaskListItem> result =
        service().getMyTasks(REQUESTER_ID, TaskStatus.COMPLETED, null, 0, 20);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void usesOnlyTheGivenStatusWhenStatusFilterIsAnIncompleteStatus() {
    PageResult<MyTaskListItem> expected = PageResult.of(List.of(), 0, 20, false);
    when(myTaskListQueryPort.findMine(REQUESTER_ID, List.of(TaskStatus.DELAYED), null, 0, 20))
        .thenReturn(expected);

    PageResult<MyTaskListItem> result =
        service().getMyTasks(REQUESTER_ID, TaskStatus.DELAYED, null, 0, 20);

    assertThat(result).isSameAs(expected);
  }

  @Test
  void forwardsWorkspaceIdPageAndSizeToPort() {
    PageResult<MyTaskListItem> expected = PageResult.of(List.of(), 1, 10, true);
    when(myTaskListQueryPort.findMine(REQUESTER_ID, DEFAULT_STATUSES, 5L, 1, 10))
        .thenReturn(expected);

    PageResult<MyTaskListItem> result = service().getMyTasks(REQUESTER_ID, null, 5L, 1, 10);

    assertThat(result).isSameAs(expected);
  }
}
