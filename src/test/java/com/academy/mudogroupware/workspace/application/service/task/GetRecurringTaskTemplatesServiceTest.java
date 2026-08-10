package com.academy.mudogroupware.workspace.application.service.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
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
class GetRecurringTaskTemplatesServiceTest {

  private static final long WORKSPACE_ID = 1L;
  private static final long MEMBER_ID = 10L;
  private static final long OUTSIDER_ID = 99L;
  private static final long ACADEMY_ID = 1L;
  private static final long OTHER_ACADEMY_ID = 2L;

  @Mock private WorkspaceRepository workspaceRepository;
  @Mock private RecurringTaskTemplateRepository recurringTaskTemplateRepository;

  private GetRecurringTaskTemplatesService service() {
    return new GetRecurringTaskTemplatesService(workspaceRepository, recurringTaskTemplateRepository);
  }

  @Test
  void returnsPagedTemplatesForMember() {
    givenWorkspaceWithMember();
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, WORKSPACE_ID, "주간 출결 현황 정리", RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(1)), MEMBER_ID);
    when(recurringTaskTemplateRepository.findAllByWorkspaceId(WORKSPACE_ID, 0, 20))
        .thenReturn(PageResult.of(List.of(template), 0, 20, false));

    PageResult<RecurringTaskTemplate> result =
        service().getTemplates(WORKSPACE_ID, MEMBER_ID, 0, 20, ACADEMY_ID, false);

    assertThat(result.content()).containsExactly(template);
    assertThat(result.hasNext()).isFalse();
  }

  @Test
  void rejectsMissingWorkspace() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service().getTemplates(WORKSPACE_ID, OUTSIDER_ID, 0, 20, ACADEMY_ID, false))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(recurringTaskTemplateRepository, never()).findAllByWorkspaceId(anyLong(), anyInt(), anyInt());
  }

  @Test
  void rejectsMissingWorkspaceEvenWhenCanReadAllIsTrue() {
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service().getTemplates(WORKSPACE_ID, OUTSIDER_ID, 0, 20, ACADEMY_ID, true))
        .isInstanceOf(WorkspaceNotFoundException.class);

    verify(recurringTaskTemplateRepository, never()).findAllByWorkspaceId(anyLong(), anyInt(), anyInt());
  }

  @Test
  void rejectsNonMember() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () -> service().getTemplates(WORKSPACE_ID, OUTSIDER_ID, 0, 20, ACADEMY_ID, false))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(recurringTaskTemplateRepository, never()).findAllByWorkspaceId(anyLong(), anyInt(), anyInt());
  }

  @Test
  void rejectsOtherAcademyEvenWhenCanReadAllIsTrue() {
    givenWorkspaceWithMember();

    assertThatThrownBy(
            () ->
                service()
                    .getTemplates(WORKSPACE_ID, OUTSIDER_ID, 0, 20, OTHER_ACADEMY_ID, true))
        .isInstanceOf(WorkspaceAccessDeniedException.class);

    verify(recurringTaskTemplateRepository, never()).findAllByWorkspaceId(anyLong(), anyInt(), anyInt());
  }

  @Test
  void allowsNonMemberWhenCanReadAllIsTrue() {
    givenWorkspaceWithMember();
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, WORKSPACE_ID, "주간 출결 현황 정리", RecurrenceType.WEEKLY,
            Map.of("daysOfWeek", List.of(1)), MEMBER_ID);
    when(recurringTaskTemplateRepository.findAllByWorkspaceId(WORKSPACE_ID, 0, 20))
        .thenReturn(PageResult.of(List.of(template), 0, 20, false));

    PageResult<RecurringTaskTemplate> result =
        service().getTemplates(WORKSPACE_ID, OUTSIDER_ID, 0, 20, ACADEMY_ID, true);

    assertThat(result.content()).containsExactly(template);
  }

  private void givenWorkspaceWithMember() {
    Workspace workspace =
        Workspace.restore(WORKSPACE_ID, ACADEMY_ID, "8월 학사 운영", MEMBER_ID, Set.of(MEMBER_ID));
    when(workspaceRepository.findById(WORKSPACE_ID)).thenReturn(Optional.of(workspace));
  }
}
