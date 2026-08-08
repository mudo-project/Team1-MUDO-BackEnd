package com.academy.mudogroupware.workspace.application.service.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.workspace.RenameWorkspaceCommand;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RenameWorkspaceServiceTest {

  @Mock private WorkspaceRepository workspaceRepository;

  private RenameWorkspaceService renameWorkspaceService;

  @BeforeEach
  void setUp() {
    renameWorkspaceService = new RenameWorkspaceService(workspaceRepository);
  }

  @Test
  void renamesWorkspaceWhenRequesterIsCurrentMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L))));

    String name = renameWorkspaceService.rename(new RenameWorkspaceCommand(10L, 100L, "  운영팀  "));

    assertThat(name).isEqualTo("운영팀");
    verify(workspaceRepository).rename(100L, "운영팀");
  }

  @Test
  void rejectsRenameWhenRequesterIsNotCurrentMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L))));

    assertThatThrownBy(
            () -> renameWorkspaceService.rename(new RenameWorkspaceCommand(99L, 100L, "운영팀")))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(workspaceRepository, never()).rename(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void rejectsRenameWhenWorkspaceDoesNotExist() {
    when(workspaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> renameWorkspaceService.rename(new RenameWorkspaceCommand(10L, 100L, "운영팀")))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }
}
