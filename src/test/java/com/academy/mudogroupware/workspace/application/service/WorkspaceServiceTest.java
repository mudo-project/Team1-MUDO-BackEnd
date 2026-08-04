package com.academy.mudogroupware.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import java.util.LinkedHashSet;
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
    WorkspaceCreationTransaction creationTransaction =
        new WorkspaceCreationTransaction(workspaceRepository);
    workspaceService = new WorkspaceService(workspaceMemberDirectoryPort, creationTransaction);
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
    assertThat(memberIdsCaptor.getValue()).isInstanceOf(LinkedHashSet.class);
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
  void appendsSmallestAvailableSuffixToAnExistingActiveName() {
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(10L)))
        .thenReturn(Set.of(10L));
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀")).thenReturn(true);
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀 (1)")).thenReturn(true);
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀 (2)")).thenReturn(false);
    stubSuccessfulSave(101L);

    workspaceService.createWorkspace(
        new CreateWorkspaceCommand(1L, 10L, "개발팀", List.of()));

    ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
    verify(workspaceRepository).save(workspaceCaptor.capture());
    assertThat(workspaceCaptor.getValue().getName()).isEqualTo("개발팀 (2)");
  }

  @Test
  void shortensBaseNameSoSuffixKeepsGeneratedNameWithinOneHundredCharacters() {
    String baseName = "a".repeat(100);
    String suffixedName = "a".repeat(96) + " (1)";
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(10L)))
        .thenReturn(Set.of(10L));
    when(workspaceRepository.existsByAcademyIdAndName(1L, baseName)).thenReturn(true);
    when(workspaceRepository.existsByAcademyIdAndName(1L, suffixedName)).thenReturn(false);
    stubSuccessfulSave(101L);

    workspaceService.createWorkspace(
        new CreateWorkspaceCommand(1L, 10L, baseName, List.of()));

    ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
    verify(workspaceRepository).save(workspaceCaptor.capture());
    assertThat(workspaceCaptor.getValue().getName()).isEqualTo(suffixedName).hasSize(100);
  }

  @Test
  void retriesOnceWithAResolvedNameAfterPersistenceNameCollision() {
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(10L)))
        .thenReturn(Set.of(10L));
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀"))
        .thenReturn(false, true);
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀 (1)")).thenReturn(false);
    when(workspaceRepository.save(any(Workspace.class)))
        .thenThrow(new WorkspaceNameConflictException())
        .thenAnswer(invocation -> persisted(invocation.getArgument(0), 101L));

    Long workspaceId =
        workspaceService.createWorkspace(
            new CreateWorkspaceCommand(1L, 10L, "개발팀", List.of()));

    assertThat(workspaceId).isEqualTo(101L);
    ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
    verify(workspaceRepository, times(2)).save(workspaceCaptor.capture());
    assertThat(workspaceCaptor.getAllValues())
        .extracting(Workspace::getName)
        .containsExactly("개발팀", "개발팀 (1)");
  }

  @Test
  void propagatesSecondPersistenceNameCollisionWithoutThirdAttempt() {
    when(workspaceMemberDirectoryPort.findActiveUserIds(1L, Set.of(10L)))
        .thenReturn(Set.of(10L));
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀"))
        .thenReturn(false, true);
    when(workspaceRepository.existsByAcademyIdAndName(1L, "개발팀 (1)")).thenReturn(false);
    when(workspaceRepository.save(any(Workspace.class)))
        .thenThrow(new WorkspaceNameConflictException())
        .thenThrow(new WorkspaceNameConflictException());

    assertThatThrownBy(
            () ->
                workspaceService.createWorkspace(
                    new CreateWorkspaceCommand(1L, 10L, "개발팀", List.of())))
        .isInstanceOf(WorkspaceNameConflictException.class)
        .extracting("errorCode")
        .isEqualTo(WorkspaceErrorCode.NAME_CONFLICT);

    verify(workspaceRepository, times(2)).save(any(Workspace.class));
    verify(workspaceRepository, never()).existsByAcademyIdAndName(1L, "개발팀 (2)");
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
