package com.academy.mudogroupware.workspace.presentation.api.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.application.command.task.CreateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.CreateRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.application.usecase.task.GetRecurringTaskTemplatesUseCase;
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

  private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 1L, 3L, "MEMBER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateRecurringTaskTemplateUseCase createRecurringTaskTemplateUseCase;
  @MockitoBean private GetRecurringTaskTemplatesUseCase getRecurringTaskTemplatesUseCase;
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Test
  void getTemplatesReturnsPagedList() throws Exception {
    RecurringTaskTemplate template =
        RecurringTaskTemplate.restore(
            1L, 1L, "주간 출결 현황 정리", RecurrenceType.WEEKLY, Map.of("daysOfWeek", List.of(1)), 10L);
    when(getRecurringTaskTemplatesUseCase.getTemplates(1L, AUTH_USER.userId(), 0, 20))
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
  void getTemplatesPropagatesWorkspaceNotFound() throws Exception {
    when(getRecurringTaskTemplatesUseCase.getTemplates(anyLong(), anyLong(), anyInt(), anyInt()))
        .thenThrow(new WorkspaceNotFoundException());

    mockMvc
        .perform(get("/api/workspaces/1/recurring-templates").with(authentication(auth())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_1"));
  }

  @Test
  void getTemplatesPropagatesAccessDenied() throws Exception {
    when(getRecurringTaskTemplatesUseCase.getTemplates(anyLong(), anyLong(), anyInt(), anyInt()))
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

  private Authentication auth() {
    return new UsernamePasswordAuthenticationToken(AUTH_USER, null, java.util.List.of());
  }
}
