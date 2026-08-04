package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.CreateWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.port.WorkspaceMemberDirectoryPort;
import com.academy.mudogroupware.workspace.domain.exception.InvalidWorkspaceMemberException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceErrorCode;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNameConflictException;
import com.academy.mudogroupware.workspace.domain.model.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.WorkspaceRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

  @Mock private WorkspaceMemberDirectoryPort workspaceMemberDirectoryPort;
  @Mock private WorkspaceRepository workspaceRepository;

  private WorkspaceService workspaceService;

  @BeforeEach
  void setUp() {
    workspaceService = new WorkspaceService(workspaceMemberDirectoryPort, workspaceRepository);
  }

  @Test
  void includesCreatorWhenAdditionalMemberListIsEmpty() {
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(10L)))
        .thenReturn(Set.of(10L));
    stubSuccessfulSave(101L);

    Long workspaceId =
        workspaceService.createWorkspace(
            new CreateWorkspaceCommand(1L, 10L, "  개발팀  ", List.of()));

    assertThat(workspaceId).isEqualTo(101L);
    ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
    verify(workspaceRepository).save(workspaceCaptor.capture());
    assertThat(workspaceCaptor.getValue().getName()).isEqualTo("개발팀");
    assertThat(workspaceCaptor.getValue().getMemberIds()).containsExactly(10L);
  }

  @Test
  void collapsesDuplicateMemberIdsBeforeValidationAndCreation() {
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(20L, 10L)))
        .thenReturn(Set.of(20L, 10L));
    stubSuccessfulSave(101L);

    workspaceService.createWorkspace(
        new CreateWorkspaceCommand(1L, 10L, "개발팀", List.of(20L, 20L, 10L, 20L)));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<Long>> memberIdsCaptor = ArgumentCaptor.forClass(Set.class);
    verify(workspaceMemberDirectoryPort).findActiveUserIds(any(), memberIdsCaptor.capture());
    assertThat(memberIdsCaptor.getValue()).containsExactly(20L, 10L);

    ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
    verify(workspaceRepository).save(workspaceCaptor.capture());
    assertThat(workspaceCaptor.getValue().getMemberIds()).containsExactlyInAnyOrder(20L, 10L);
  }

  @Test
  void rejectsCreationWhenAnyRequestedMemberIsNotActiveInAcademy() {
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(10L, 20L)))
        .thenReturn(Set.of(10L));

    assertThatThrownBy(
            () ->
                workspaceService.createWorkspace(
                    new CreateWorkspaceCommand(1L, 10L, "개발팀", List.of(20L))))
        .isInstanceOf(InvalidWorkspaceMemberException.class)
        .extracting("errorCode")
        .isEqualTo(WorkspaceErrorCode.INVALID_MEMBER);

    verifyNoInteractions(workspaceRepository);
  }

  @Test
  void rejectsCreationWhenActiveWorkspaceNameAlreadyExists() {
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(10L)))
        .thenReturn(Set.of(10L));
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀")).thenReturn(true);

    assertThatThrownBy(
            () ->
                workspaceService.createWorkspace(
                    new CreateWorkspaceCommand(1L, 10L, "개발팀", List.of())))
        .isInstanceOf(WorkspaceNameConflictException.class)
        .extracting("errorCode")
        .isEqualTo(WorkspaceErrorCode.NAME_CONFLICT);

    verify(workspaceRepository, never()).save(any());
  }

  private void stubSuccessfulSave(Long workspaceId) {
    when(workspaceRepository.save(any(Workspace.class)))
        .thenAnswer(invocation -> persisted(invocation.getArgument(0), workspaceId));
  }

  private Workspace persisted(Workspace workspace, Long workspaceId) {
    return Workspace.builder()
        .id(workspaceId)
        .academyId(workspace.getAcademyId())
        .name(workspace.getName())
        .createdBy(workspace.getCreatedBy())
        .memberIds(workspace.getMemberIds())
        .build();
  }
}
