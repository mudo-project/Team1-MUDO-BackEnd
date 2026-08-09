package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.task.DeleteRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.domain.exception.task.RecurringTaskTemplateNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import com.academy.mudogroupware.workspace.domain.model.workspace.Workspace;
import com.academy.mudogroupware.workspace.domain.repository.task.RecurringTaskTemplateRepository;
import com.academy.mudogroupware.workspace.domain.repository.workspace.WorkspaceRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeleteRecurringTaskTemplateServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long TEMPLATE_ID = 101L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  private DeleteRecurringTaskTemplateService service() {
    return new DeleteRecurringTaskTemplateService(workspaceRepository, recurringTaskTemplateRepository);
  }

  @Test
  void deletesTemplateWhenMemberAndTemplateExist() {
    givenWorkspaceWithMember();
    givenExistingTemplate();

    service()
        .delete(new DeleteRecurringTaskTemplateCommand(WORKSPACE_ID, TEMPLATE_ID, MEMBER_ID));

    verify(recurringTaskTemplateRepository).delete(TEMPLATE_ID);
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .delete(new DeleteRecurringTaskTemplateCommand(WORKSPACE_ID, TEMPLATE_ID, OUTSIDER_ID)))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(recurringTaskTemplateRepository, never()).delete(any());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () ->
                service()
                    .delete(new DeleteRecurringTaskTemplateCommand(WORKSPACE_ID, TEMPLATE_ID, OUTSIDER_ID)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(recurringTaskTemplateRepository, never()).delete(any());
  }

  @Test
  void rejectsMissingTemplate() {
    givenWorkspaceWithMember();
    when(recurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate(WORKSPACE_ID, TEMPLATE_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .delete(new DeleteRecurringTaskTemplateCommand(WORKSPACE_ID, TEMPLATE_ID, MEMBER_ID)))
        .isInstanceOf(RecurringTaskTemplateNotFoundException.class);

    verify(recurringTaskTemplateRepository, never()).delete(any());
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace = Workspace.restore(WORKSPACE_ID, 1L, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private void givenExistingTemplate() {
    RecurringTaskTemplate existing =
        RecurringTaskTemplate.restore(
            TEMPLATE_ID, WORKSPACE_ID, "삭제 대상", RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(1)), MEMBER_ID);
    when(recurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate(WORKSPACE_ID, TEMPLATE_ID))
        .thenReturn(Optional.of(existing));
  }
}
