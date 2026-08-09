package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.workspace.application.command.task.CreateRecurringTaskTemplateCommand;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreateRecurringTaskTemplateServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  @Captor private ArgumentCaptor<RecurringTaskTemplate> templateCaptor;

  private CreateRecurringTaskTemplateService service() {
    return new CreateRecurringTaskTemplateService(workspaceRepository, recurringTaskTemplateRepository);
  }

  @Test
  void createsTemplateAndReturnsId() {
    givenWorkspaceWithMember();
    when(recurringTaskTemplateRepository.save(any()))
        .thenReturn(
            RecurringTaskTemplate.restore(
                101L, WORKSPACE_ID, "주간 출결 현황 정리", RecurrenceType.WEEKLY,
                Map.of("daysOfWeek", List.of(1)), MEMBER_ID));

    Long templateId =
        service()
            .create(
                new CreateRecurringTaskTemplateCommand(
                    WORKSPACE_ID, MEMBER_ID, "주간 출결 현황 정리", RecurrenceType.WEEKLY,
                    Map.of("daysOfWeek", List.of(1))));

    assertThat(templateId).isEqualTo(101L);
    verify(recurringTaskTemplateRepository).save(templateCaptor.capture());
    assertThat(templateCaptor.getValue().getTitle()).isEqualTo("주간 출결 현황 정리");
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .create(
                        new CreateRecurringTaskTemplateCommand(
                            WORKSPACE_ID, OUTSIDER_ID, "제목", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)))))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(recurringTaskTemplateRepository, never()).save(any());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () ->
                service()
                    .create(
                        new CreateRecurringTaskTemplateCommand(
                            WORKSPACE_ID, OUTSIDER_ID, "제목", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)))))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(recurringTaskTemplateRepository, never()).save(any());
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace = Workspace.restore(WORKSPACE_ID, 1L, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }
}
