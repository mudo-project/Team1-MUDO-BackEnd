package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.task.UpdateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.domain.exception.task.InvalidRecurrenceRuleException;
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
class UpdateRecurringTaskTemplateServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long TEMPLATE_ID = 101L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  private UpdateRecurringTaskTemplateService service() {
    return new UpdateRecurringTaskTemplateService(workspaceRepository, recurringTaskTemplateRepository);
  }

  @Test
  void updatesTitleOnlyAndKeepsExistingRecurrence() {
    givenWorkspaceWithMember();
    givenExistingTemplate();
    when(recurringTaskTemplateRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RecurringTaskTemplate updated =
        service()
            .update(
                new UpdateRecurringTaskTemplateCommand(
                    WORKSPACE_ID, TEMPLATE_ID, MEMBER_ID, "새 제목", null, null));

    assertThat(updated.getTitle()).isEqualTo("새 제목");
    assertThat(updated.getRecurrenceType()).isEqualTo(RecurrenceType.WEEKLY);
    assertThat(updated.getRecurrenceRule()).isEqualTo(Map.of("daysOfWeek", List.of(1)));
  }

  @Test
  void updatesRecurrenceAndKeepsExistingTitle() {
    givenWorkspaceWithMember();
    givenExistingTemplate();
    when(recurringTaskTemplateRepository.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    RecurringTaskTemplate updated =
        service()
            .update(
                new UpdateRecurringTaskTemplateCommand(
                    WORKSPACE_ID, TEMPLATE_ID, MEMBER_ID, null, RecurrenceType.MONTHLY, Map.of("dayOfMonth", 1)));

    assertThat(updated.getTitle()).isEqualTo("기존 제목");
    assertThat(updated.getRecurrenceType()).isEqualTo(RecurrenceType.MONTHLY);
    assertThat(updated.getRecurrenceRule()).isEqualTo(Map.of("dayOfMonth", 1));
  }

  @Test
  void rejectsMismatchedRecurrenceRule() {
    givenWorkspaceWithMember();
    givenExistingTemplate();

    assertThatThrownBy(
            () ->
                service()
                    .update(
                        new UpdateRecurringTaskTemplateCommand(
                            WORKSPACE_ID, TEMPLATE_ID, MEMBER_ID, null, RecurrenceType.MONTHLY, Map.of("dayOfMonth", 15))))
        .isInstanceOf(InvalidRecurrenceRuleException.class);

    verify(recurringTaskTemplateRepository, never()).save(any());
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .update(
                        new UpdateRecurringTaskTemplateCommand(
                            WORKSPACE_ID, TEMPLATE_ID, OUTSIDER_ID, "제목", null, null)))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(recurringTaskTemplateRepository, never()).save(any());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () ->
                service()
                    .update(
                        new UpdateRecurringTaskTemplateCommand(
                            WORKSPACE_ID, TEMPLATE_ID, OUTSIDER_ID, "제목", null, null)))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(recurringTaskTemplateRepository, never()).save(any());
  }

  @Test
  void rejectsMissingTemplate() {
    givenWorkspaceWithMember();
    when(recurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate(WORKSPACE_ID, TEMPLATE_ID))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .update(
                        new UpdateRecurringTaskTemplateCommand(
                            WORKSPACE_ID, TEMPLATE_ID, MEMBER_ID, "제목", null, null)))
        .isInstanceOf(RecurringTaskTemplateNotFoundException.class);

    verify(recurringTaskTemplateRepository, never()).save(any());
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace = Workspace.restore(WORKSPACE_ID, 1L, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }

  private void givenExistingTemplate() {
    RecurringTaskTemplate existing =
        RecurringTaskTemplate.restore(
            TEMPLATE_ID, WORKSPACE_ID, "기존 제목", RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(1)), MEMBER_ID);
    when(recurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate(WORKSPACE_ID, TEMPLATE_ID))
        .thenReturn(Optional.of(existing));
  }
}
