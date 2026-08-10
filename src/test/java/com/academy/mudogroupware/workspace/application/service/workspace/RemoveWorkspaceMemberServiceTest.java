package com.academy.mudogroupware.workspace.application.service.workspace;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.workspace.RemoveWorkspaceMemberCommand;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceLastMemberException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceMemberNotFoundException;
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
class RemoveWorkspaceMemberServiceTest {

  @Mock private WorkspaceRepository workspaceRepository;

  private RemoveWorkspaceMemberService removeWorkspaceMemberService;

  @BeforeEach
  void setUp() {
    removeWorkspaceMemberService = new RemoveWorkspaceMemberService(workspaceRepository);
  }

  @Test
  void removesOtherMemberWhenRequesterIsCurrentMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L, 20L))));

    removeWorkspaceMemberService.removeMember(new RemoveWorkspaceMemberCommand(10L, 100L, 20L));

    verify(workspaceRepository).updateMembers(100L, Set.of(10L));
  }

  @Test
  void allowsSelfRemovalWhenMoreThanOneMemberRemains() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L, 20L))));

    removeWorkspaceMemberService.removeMember(new RemoveWorkspaceMemberCommand(20L, 100L, 20L));

    verify(workspaceRepository).updateMembers(100L, Set.of(10L));
  }

  @Test
  void rejectsSelfRemovalWhenRequesterIsTheOnlyRemainingMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L))));

    assertThatThrownBy(
            () -> removeWorkspaceMemberService.removeMember(
                new RemoveWorkspaceMemberCommand(10L, 100L, 10L)))
        .isInstanceOf(WorkspaceLastMemberException.class);

    verify(workspaceRepository, never())
        .updateMembers(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anySet());
  }

  @Test
  void rejectsRemovingUserWhoIsNotAMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L, 20L))));

    assertThatThrownBy(
            () -> removeWorkspaceMemberService.removeMember(
                new RemoveWorkspaceMemberCommand(10L, 100L, 99L)))
        .isInstanceOf(WorkspaceMemberNotFoundException.class);
  }

  @Test
  void rejectsRemovalWhenRequesterIsNotCurrentMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, 1L, "개발팀", 10L, Set.of(10L, 20L))));

    assertThatThrownBy(
            () -> removeWorkspaceMemberService.removeMember(
                new RemoveWorkspaceMemberCommand(99L, 100L, 20L)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);
  }

  @Test
  void rejectsRemovalWhenWorkspaceDoesNotExist() {
    when(workspaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> removeWorkspaceMemberService.removeMember(
                new RemoveWorkspaceMemberCommand(10L, 100L, 20L)))
        .isInstanceOf(WorkspaceNotFoundException.class);
  }
}
