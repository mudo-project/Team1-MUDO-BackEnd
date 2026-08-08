package com.academy.mudogroupware.workspace.presentation.api.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.application.command.task.CreateRecurringTaskTemplateCommand;
import com.academy.mudogroupware.workspace.application.usecase.task.CreateRecurringTaskTemplateUseCase;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceNotFoundException;
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
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

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
                .content("{\"title\":\"   \",\"recurrenceType\":\"DAILY\",\"recurrenceRule\":{}}"))
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
                .content("{\"title\":\"제목\",\"recurrenceType\":\"DAILY\",\"recurrenceRule\":{}}"))
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
                .content("{\"title\":\"제목\",\"recurrenceType\":\"DAILY\",\"recurrenceRule\":{}}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  private Authentication auth() {
    return new UsernamePasswordAuthenticationToken(AUTH_USER, null, java.util.List.of());
  }
}
