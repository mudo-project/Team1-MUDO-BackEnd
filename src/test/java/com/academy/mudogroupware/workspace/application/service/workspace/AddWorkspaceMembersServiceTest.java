package com.academy.mudogroupware.workspace.application.service.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.workspace.AddWorkspaceMembersCommand;
import com.academy.mudogroupware.workspace.application.port.WorkspaceMemberDirectoryPort;
import com.academy.mudogroupware.workspace.domain.exception.workspace.InvalidWorkspaceMemberException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddWorkspaceMembersServiceTest {

  @Mock private WorkspaceMemberDirectoryPort workspaceMemberDirectoryPort;
  @Mock private WorkspaceRepository workspaceRepository;

  private AddWorkspaceMembersService addWorkspaceMembersService;

  @BeforeEach
  void setUp() {
    addWorkspaceMembersService =
        new AddWorkspaceMembersService(workspaceMemberDirectoryPort, workspaceRepository);
  }

  @Test
  void addsOnlyNewActiveMembersAndReturnsThem() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L, 20L))));
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(30L)))
        .thenReturn(Set.of(30L));

    Set<Long> added =
        addWorkspaceMembersService.addMembers(
            new AddWorkspaceMembersCommand(1L, 10L, 100L, List.of(20L, 30L)));

    assertThat(added).containsExactly(30L);
    verify(workspaceRepository).updateMembers(100L, Set.of(10L, 20L, 30L));
  }

  @Test
  void skipsRepositoryAndDirectoryLookupWhenAllRequestedMembersAlreadyJoined() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L, 20L))));

    Set<Long> added =
        addWorkspaceMembersService.addMembers(
            new AddWorkspaceMembersCommand(1L, 10L, 100L, List.of(20L)));

    assertThat(added).isEmpty();
    verifyNoInteractions(workspaceMemberDirectoryPort);
    verify(workspaceRepository, never())
        .updateMembers(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anySet());
  }

  @Test
  void rejectsAddingMemberWhoIsNotActiveInSameAcademy() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L))));
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(30L))).thenReturn(Set.of());

    assertThatThrownBy(
            () ->
                addWorkspaceMembersService.addMembers(
                    new AddWorkspaceMembersCommand(1L, 10L, 100L, List.of(30L))))
        .isInstanceOf(InvalidWorkspaceMemberException.class);

    verify(workspaceRepository, never())
        .updateMembers(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anySet());
  }

  @Test
  void rejectsAddingMembersWhenRequesterIsNotCurrentMember() {
    when(workspaceRepository.findByIdForUpdate(100L))
        .thenReturn(Optional.of(Workspace.restore(100L, "개발팀", 10L, Set.of(10L))));

    assertThatThrownBy(
            () ->
                addWorkspaceMembersService.addMembers(
                    new AddWorkspaceMembersCommand(1L, 99L, 100L, List.of(30L))))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verifyNoInteractions(workspaceMemberDirectoryPort);
  }
}
