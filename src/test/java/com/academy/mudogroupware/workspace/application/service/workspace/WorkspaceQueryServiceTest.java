package com.academy.mudogroupware.workspace.application.service.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.port.WorkspaceListQueryPort;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListItem;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceListScope;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceQueryServiceTest {

  @Mock private WorkspaceListQueryPort queryPort;

  @Test
  void delegatesMineScopeToFindMine() {
    WorkspaceQueryService service = new WorkspaceQueryService(queryPort);
    List<WorkspaceListItem> expected = List.of(new WorkspaceListItem(100L, "Workspace", 2));
    when(queryPort.findMine(10L)).thenReturn(expected);

    List<WorkspaceListItem> result = service.getWorkspaces(10L, WorkspaceListScope.MINE);

    assertThat(result).isSameAs(expected);
    verify(queryPort).findMine(10L);
  }

  @Test
  void delegatesAllScopeToFindAll() {
    WorkspaceQueryService service = new WorkspaceQueryService(queryPort);
    List<WorkspaceListItem> expected = List.of(new WorkspaceListItem(100L, "Workspace", 2));
    when(queryPort.findAll(10L)).thenReturn(expected);

    List<WorkspaceListItem> result = service.getWorkspaces(10L, WorkspaceListScope.ALL);

    assertThat(result).isSameAs(expected);
    verify(queryPort).findAll(10L);
  }
}
