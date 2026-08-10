package com.academy.mudogroupware.workspace.application.service.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.workspace.RecoverWorkspaceCommand;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAlreadyActiveException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecoverWorkspaceServiceTest {

  @Mock private WorkspaceRepository workspaceRepository;

  private final Clock clock =
      Clock.fixed(Instant.parse("2026-08-06T15:30:12Z"), ZoneOffset.UTC);

  private RecoverWorkspaceService recoverWorkspaceService;

  @BeforeEach
  void setUp() {
    recoverWorkspaceService = new RecoverWorkspaceService(workspaceRepository, clock);
  }

  @Test
  void recoversWithOriginalNameWhenNoConflict() {
    when(workspaceRepository.findDeletedByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L))));
    when(workspaceRepository.existsByName("개발팀")).thenReturn(false);

    String name = recoverWorkspaceService.recover(new RecoverWorkspaceCommand(10L, 100L));

    assertThat(name).isEqualTo("개발팀");
    verify(workspaceRepository).recover(100L, "개발팀");
  }

  @Test
  void recoversWithTimestampSuffixWhenNameConflicts() {
    when(workspaceRepository.findDeletedByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L))));
    when(workspaceRepository.existsByName("개발팀")).thenReturn(true);

    String name = recoverWorkspaceService.recover(new RecoverWorkspaceCommand(10L, 100L));

    assertThat(name).isEqualTo("개발팀(20260806153012)");
    verify(workspaceRepository).recover(100L, "개발팀(20260806153012)");
  }

  @Test
  void truncatesOriginalNameSoSuffixedNameStaysWithin100Characters() {
    String longName = "가".repeat(100);
    when(workspaceRepository.findDeletedByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, longName, 10L, Set.of(10L))));
    when(workspaceRepository.existsByName(longName)).thenReturn(true);

    String name = recoverWorkspaceService.recover(new RecoverWorkspaceCommand(10L, 100L));

    assertThat(name).hasSize(100);
    assertThat(name).endsWith("(20260806153012)");
  }

  @Test
  void rejectsRecoverWhenRequesterWasNotAMemberAtDeletionTime() {
    when(workspaceRepository.findDeletedByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L))));

    assertThatThrownBy(
            () -> recoverWorkspaceService.recover(new RecoverWorkspaceCommand(99L, 100L)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(workspaceRepository, never())
        .existsByName(org.mockito.ArgumentMatchers.anyString());
    verify(workspaceRepository, never())
        .recover(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void rejectsRecoverWhenWorkspaceDoesNotExist() {
    when(workspaceRepository.findDeletedByIdForUpdate(100L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> recoverWorkspaceService.recover(new RecoverWorkspaceCommand(10L, 100L)))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }

  @Test
  void propagatesAlreadyActiveExceptionWhenWorkspaceIsNotDeleted() {
    when(workspaceRepository.findDeletedByIdForUpdate(100L))
        .thenThrow(new WorkspaceAlreadyActiveException());

    assertThatThrownBy(
            () -> recoverWorkspaceService.recover(new RecoverWorkspaceCommand(10L, 100L)))
        .isInstanceOf(WorkspaceAlreadyActiveException.class);
  }

  @Test
  void propagatesNameConflictExceptionWhenRecoverWriteRacesWithAnotherRequest() {
    when(workspaceRepository.findDeletedByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L))));
    when(workspaceRepository.existsByName("개발팀")).thenReturn(false);
    org.mockito.Mockito.doThrow(new WorkspaceNameConflictException())
        .when(workspaceRepository)
        .recover(100L, "개발팀");

    assertThatThrownBy(
            () -> recoverWorkspaceService.recover(new RecoverWorkspaceCommand(10L, 100L)))
        .isInstanceOf(WorkspaceNameConflictException.class);
  }
}
