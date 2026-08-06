package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.DeleteWorkspaceCommand;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteWorkspaceServiceTest {

  @Mock private WorkspaceRepository workspaceRepository;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-08-06T12:00:00Z"), ZoneOffset.UTC);

  private DeleteWorkspaceService deleteWorkspaceService;

  @BeforeEach
  void setUp() {
    deleteWorkspaceService = new DeleteWorkspaceService(workspaceRepository, clock);
  }

  @Test
  void deletesWorkspaceWhenRequesterIsCurrentMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L, 20L))));

    deleteWorkspaceService.delete(new DeleteWorkspaceCommand(10L, 100L));

    verify(workspaceRepository).delete(100L, LocalDateTime.of(2026, 8, 6, 12, 0));
  }

  @Test
  void deletesWorkspaceWhenRequesterIsTheOnlyRemainingMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L))));

    deleteWorkspaceService.delete(new DeleteWorkspaceCommand(10L, 100L));

    verify(workspaceRepository).delete(100L, LocalDateTime.of(2026, 8, 6, 12, 0));
  }

  @Test
  void rejectsDeleteWhenRequesterIsNotCurrentMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L))));

    assertThatThrownBy(() -> deleteWorkspaceService.delete(new DeleteWorkspaceCommand(99L, 100L)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(workspaceRepository, never())
        .delete(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void rejectsDeleteWhenWorkspaceDoesNotExist() {
    when(workspaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> deleteWorkspaceService.delete(new DeleteWorkspaceCommand(10L, 100L)))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }
}
