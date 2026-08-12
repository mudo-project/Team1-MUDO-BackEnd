package com.academy.mudogroupware.workspace.presentation.api.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.application.command.task.CreateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.command.task.DeleteRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.command.task.UpdateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.CreateRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.DeleteRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.GetRecurringTaskTemplatesUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.UpdateRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.domain.exception.task.InvalidRecurrenceRuleException;
import com.academy.mudogroupware.workspace.domain.exception.task.RecurringTaskTemplateNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.task.RecurrenceType;
import com.academy.mudogroupware.workspace.domain.model.task.RecurringTaskTemplate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceRecurringTaskTemplateController.class)
class WorkspaceRecurringTaskTemplateControllerTest {

  private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 3L, "MEMBER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateRecurringTaskTemplateUseCase createRecurringTaskTemplateUseCase;
  @MockitoBean private GetRecurringTaskTemplatesUseCase getRecurringTaskTemplatesUseCase;
  @MockitoBean private UpdateRecurringTaskTemplateUseCase updateRecurringTaskTemplateUseCase;
  @MockitoBean private DeleteRecurringTaskTemplateUseCase deleteRecurringTaskTemplateUseCase;
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Test
  void getTemplatesReturnsPagedList() throws Exception {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, 1L, "주간 출결 현황 정리", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), 10L);
    when(getRecurringTaskTemplatesUseCase.getTemplates(1L, AUTH_USER.userId(), 0, 20, false))
        .thenReturn(PageResult.of(List.of(template), 0, 20, false));

    mockMvc
        .perform(get("/api/workspaces/1/recurring-templates").with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_9"))
        .andExpect(jsonPath("$.data.content[0].templateId").value(1))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void getTemplatesRejectsInvalidPageAndSize() throws Exception {
    mockMvc
        .perform(
            get("/api/workspaces/1/recurring-templates")
                .param("page", "-1")
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            get("/api/workspaces/1/recurring-templates")
                .param("size", "0")
                .with(authentication(auth())))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(getRecurringTaskTemplatesUseCase);
  }

  @Test
  void getTemplatesForwardsReadAllAuthority() throws Exception {
    when(getRecurringTaskTemplatesUseCase.getTemplates(1L, AUTH_USER.userId(), 0, 20, true))
        .thenReturn(PageResult.of(List.of(), 0, 20, false));

    mockMvc
        .perform(
            get("/api/workspaces/1/recurring-templates")
                .with(authentication(auth("WORKSPACE:READ_ALL"))))
        .andExpect(status().isOk());

    verify(getRecurringTaskTemplatesUseCase).getTemplates(1L, AUTH_USER.userId(), 0, 20, true);
  }

  @Test
  void getTemplatesPropagatesWorkspaceNotFound() throws Exception {
    when(getRecurringTaskTemplatesUseCase.getTemplates(anyLong(), anyLong(), anyInt(), anyInt(), anyBoolean()))
        .thenThrow(new WorkspaceNotFoundException());

    mockMvc
        .perform(get("/api/workspaces/1/recurring-templates").with(authentication(auth())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_1"));
  }

  @Test
  void getTemplatesPropagatesAccessDenied() throws Exception {
    when(getRecurringTaskTemplatesUseCase.getTemplates(anyLong(), anyLong(), anyInt(), anyInt(), anyBoolean()))
        .thenThrow(new WorkspaceAccessDeniedException());

    mockMvc
        .perform(get("/api/workspaces/1/recurring-templates").with(authentication(auth())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void createTemplateReturnsCreatedWithTemplateId() throws Exception {
    when(createRecurringTaskTemplateUseCase.create(any(CreateRecurringTaskTemplateCommand.class)))
        .thenReturn(1L);

    mockMvc
        .perform(
            post("/api/workspaces/1/recurring-templates")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"주간 출결 현황 정리\",\"recurrenceType\":\"WEEKLY\",\"recurrenceRule\":{\"daysOfWeek\":[1]}}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("WORKSPACE_201_4"))
        .andExpect(jsonPath("$.data.templateId").value(1));

    verify(createRecurringTaskTemplateUseCase).create(any(CreateRecurringTaskTemplateCommand.class));
  }

  @Test
  void createTemplateRejectsBlankTitle() throws Exception {
    mockMvc
        .perform(
            post("/api/workspaces/1/recurring-templates")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"   \",\"recurrenceType\":\"WEEKLY\",\"recurrenceRule\":{\"daysOfWeek\":[1]}}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(createRecurringTaskTemplateUseCase);
  }

  @Test
  void createTemplateRejectsMissingRecurrenceType() throws Exception {
    mockMvc
        .perform(
            post("/api/workspaces/1/recurring-templates")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목\",\"recurrenceRule\":{}}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(createRecurringTaskTemplateUseCase);
  }

  @Test
  void createTemplatePropagatesWorkspaceNotFound() throws Exception {
    when(createRecurringTaskTemplateUseCase.create(any(CreateRecurringTaskTemplateCommand.class)))
        .thenThrow(new WorkspaceNotFoundException());

    mockMvc
        .perform(
            post("/api/workspaces/1/recurring-templates")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"제목\",\"recurrenceType\":\"WEEKLY\",\"recurrenceRule\":{\"daysOfWeek\":[1]}}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_1"));
  }

  @Test
  void createTemplatePropagatesAccessDenied() throws Exception {
    when(createRecurringTaskTemplateUseCase.create(any(CreateRecurringTaskTemplateCommand.class)))
        .thenThrow(new WorkspaceAccessDeniedException());

    mockMvc
        .perform(
            post("/api/workspaces/1/recurring-templates")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"title\":\"제목\",\"recurrenceType\":\"WEEKLY\",\"recurrenceRule\":{\"daysOfWeek\":[1]}}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void updateTemplateReturnsUpdatedFields() throws Exception {
    RecurringTaskTemplate updated =
        RecurringTaskTemplate.restore(
            1L, 1L, "새 제목", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), 10L);
    when(updateRecurringTaskTemplateUseCase.update(any(UpdateRecurringTaskTemplateCommand.class)))
        .thenReturn(updated);

    mockMvc
        .perform(
            patch("/api/workspaces/1/recurring-templates/1")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"새 제목\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_10"))
        .andExpect(jsonPath("$.data.templateId").value(1))
        .andExpect(jsonPath("$.data.title").value("새 제목"));

    verify(updateRecurringTaskTemplateUseCase).update(any(UpdateRecurringTaskTemplateCommand.class));
  }

  @Test
  void updateTemplateRejectsEmptyBody() throws Exception {
    mockMvc
        .perform(
            patch("/api/workspaces/1/recurring-templates/1")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_400_1"));

    verifyNoInteractions(updateRecurringTaskTemplateUseCase);
  }

  @Test
  void updateTemplateRejectsRecurrenceTypeWithoutRule() throws Exception {
    mockMvc
        .perform(
            patch("/api/workspaces/1/recurring-templates/1")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recurrenceType\":\"WEEKLY\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("COMMON_400_1"));

    verifyNoInteractions(updateRecurringTaskTemplateUseCase);
  }

  @Test
  void updateTemplateRejectsBlankTitle() throws Exception {
    mockMvc
        .perform(
            patch("/api/workspaces/1/recurring-templates/1")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(updateRecurringTaskTemplateUseCase);
  }

  @Test
  void updateTemplatePropagatesTemplateNotFound() throws Exception {
    when(updateRecurringTaskTemplateUseCase.update(any(UpdateRecurringTaskTemplateCommand.class)))
        .thenThrow(new RecurringTaskTemplateNotFoundException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/recurring-templates/1")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"새 제목\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_5"));
  }

  @Test
  void updateTemplatePropagatesAccessDenied() throws Exception {
    when(updateRecurringTaskTemplateUseCase.update(any(UpdateRecurringTaskTemplateCommand.class)))
        .thenThrow(new WorkspaceAccessDeniedException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/recurring-templates/1")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"새 제목\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void updateTemplatePropagatesInvalidRecurrenceRule() throws Exception {
    when(updateRecurringTaskTemplateUseCase.update(any(UpdateRecurringTaskTemplateCommand.class)))
        .thenThrow(new InvalidRecurrenceRuleException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/recurring-templates/1")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recurrenceType\":\"MONTHLY\",\"recurrenceRule\":{\"dayOfMonth\":15}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WORKSPACE_400_7"));
  }

  @Test
  void deleteTemplateReturnsOkWithSuccessMessage() throws Exception {
    mockMvc
        .perform(
            delete("/api/workspaces/{workspaceId}/recurring-templates/{templateId}", 1L, 101L)
                .with(authentication(auth()))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_15"))
        .andExpect(jsonPath("$.message").value("반복 업무 템플릿 삭제에 성공했습니다."))
        .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));

    verify(deleteRecurringTaskTemplateUseCase)
        .delete(new DeleteRecurringTaskTemplateCommand(1L, 101L, 10L));
  }

  @Test
  void deleteTemplateReturns401WhenUnauthenticated() throws Exception {
    mockMvc
        .perform(
            delete("/api/workspaces/{workspaceId}/recurring-templates/{templateId}", 1L, 101L)
                .with(csrf()))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(deleteRecurringTaskTemplateUseCase);
  }

  @Test
  void deleteTemplatePropagatesAccessDenied() throws Exception {
    org.mockito.Mockito.doThrow(new WorkspaceAccessDeniedException())
        .when(deleteRecurringTaskTemplateUseCase)
        .delete(any(DeleteRecurringTaskTemplateCommand.class));

    mockMvc
        .perform(
            delete("/api/workspaces/{workspaceId}/recurring-templates/{templateId}", 1L, 101L)
                .with(authentication(auth()))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void deleteTemplatePropagatesTemplateNotFound() throws Exception {
    org.mockito.Mockito.doThrow(new RecurringTaskTemplateNotFoundException())
        .when(deleteRecurringTaskTemplateUseCase)
        .delete(any(DeleteRecurringTaskTemplateCommand.class));

    mockMvc
        .perform(
            delete("/api/workspaces/{workspaceId}/recurring-templates/{templateId}", 1L, 101L)
                .with(authentication(auth()))
                .with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_5"));
  }

  private Authentication auth(String... authorities) {
    return new UsernamePasswordAuthenticationToken(
        AUTH_USER,
        null,
        java.util.List.of(authorities).stream()
            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
            .toList());
  }
}
