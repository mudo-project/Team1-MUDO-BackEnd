package com.academy.mudogroupware.workspace.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.application.command.CreateTaskCommand;
import com.academy.mudogroupware.workspace.application.command.DeleteTaskCommand;
import com.academy.mudogroupware.workspace.application.command.UpdateTaskCommand;
import com.academy.mudogroupware.workspace.application.usecase.CreateTaskUseCase;
import com.academy.mudogroupware.workspace.application.usecase.DeleteTaskUseCase;
import com.academy.mudogroupware.workspace.application.usecase.UpdateTaskUseCase;
import com.academy.mudogroupware.workspace.domain.exception.IllegalTaskDueAtException;
import com.academy.mudogroupware.workspace.domain.exception.InvalidTaskStatusTransitionException;
import com.academy.mudogroupware.workspace.domain.exception.TaskDueAtRequiredException;
import com.academy.mudogroupware.workspace.domain.exception.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import com.academy.mudogroupware.workspace.domain.model.Task;
import com.academy.mudogroupware.workspace.domain.model.TaskStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceTaskController.class)
class WorkspaceTaskControllerTest {

  private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 1L, 3L, "MEMBER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateTaskUseCase createTaskUseCase;
  @MockitoBean private UpdateTaskUseCase updateTaskUseCase;
  @MockitoBean private DeleteTaskUseCase deleteTaskUseCase;
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Test
  void createTaskReturnsCreatedWithTaskId() throws Exception {
    when(createTaskUseCase.createTask(any(CreateTaskCommand.class))).thenReturn(101L);

    mockMvc
        .perform(
            post("/api/workspaces/1/tasks")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"8월 원생 청구서 발송\",\"dueAt\":\"2026-08-10\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("WORKSPACE_201_2"))
        .andExpect(jsonPath("$.data.taskId").value(101));

    verify(createTaskUseCase).createTask(any(CreateTaskCommand.class));
  }

  @Test
  void createTaskRejectsBlankTitle() throws Exception {
    mockMvc
        .perform(
            post("/api/workspaces/1/tasks")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"   \",\"dueAt\":\"2026-08-10\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(createTaskUseCase);
  }

  @Test
  void createTaskTrimsTitleAndAcceptsExactly200CharactersAfterTrim() throws Exception {
    String title = "제".repeat(200);
    CreateTaskCommand expectedCommand =
        new CreateTaskCommand(1L, 10L, title, LocalDate.of(2026, 8, 10));
    when(createTaskUseCase.createTask(expectedCommand)).thenReturn(101L);

    mockMvc
        .perform(
            post("/api/workspaces/1/tasks")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"  " + title + "  \",\"dueAt\":\"2026-08-10\"}"))
        .andExpect(status().isCreated());

    verify(createTaskUseCase).createTask(expectedCommand);
  }

  @Test
  void createTaskRejectsTitleLongerThan200CharactersAfterTrim() throws Exception {
    String title = "제".repeat(201);

    mockMvc
        .perform(
            post("/api/workspaces/1/tasks")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\",\"dueAt\":\"2026-08-10\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(createTaskUseCase);
  }

  @Test
  void createTaskRejectsMissingDueAt() throws Exception {
    mockMvc
        .perform(
            post("/api/workspaces/1/tasks")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"업무\"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(createTaskUseCase);
  }

  @Test
  void createTaskPropagatesWorkspaceNotFound() throws Exception {
    when(createTaskUseCase.createTask(any(CreateTaskCommand.class)))
        .thenThrow(new WorkspaceNotFoundException());

    mockMvc
        .perform(
            post("/api/workspaces/1/tasks")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"업무\",\"dueAt\":\"2026-08-10\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_1"));
  }

  @Test
  void createTaskPropagatesAccessDenied() throws Exception {
    when(createTaskUseCase.createTask(any(CreateTaskCommand.class)))
        .thenThrow(new WorkspaceAccessDeniedException());

    mockMvc
        .perform(
            post("/api/workspaces/1/tasks")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"업무\",\"dueAt\":\"2026-08-10\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void updateTaskReturnsAppliedStatusAndDueAt() throws Exception {
    Task updated =
        Task.restore(101L, 1L, null, "업무", TaskStatus.IN_PROGRESS, LocalDate.of(2026, 8, 20), null, 10L);
    when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class))).thenReturn(updated);

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"dueAt\":\"2026-08-20\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_6"))
        .andExpect(jsonPath("$.data.taskId").value(101))
        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.data.dueAt").value("2026-08-20"));
  }

  @Test
  void updateTaskRejectsEmptyBody() throws Exception {
    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(updateTaskUseCase);
  }

  @Test
  void updateTaskPropagatesInvalidTransition() throws Exception {
    when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class)))
        .thenThrow(new InvalidTaskStatusTransitionException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DELAYED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WORKSPACE_400_3"));
  }

  @Test
  void updateTaskPropagatesDueAtRequired() throws Exception {
    when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class)))
        .thenThrow(new TaskDueAtRequiredException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WORKSPACE_400_4"));
  }

  @Test
  void updateTaskPropagatesRecurringDueAtRejected() throws Exception {
    when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class)))
        .thenThrow(new IllegalTaskDueAtException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dueAt\":\"2026-08-20\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WORKSPACE_400_5"));
  }

  @Test
  void updateTaskPropagatesTaskNotFound() throws Exception {
    when(updateTaskUseCase.updateTask(any(UpdateTaskCommand.class)))
        .thenThrow(new TaskNotFoundException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_3"));
  }

  @Test
  void deleteTaskReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            delete("/api/workspaces/1/tasks/101").with(authentication(auth())).with(csrf()))
        .andExpect(status().isNoContent());

    verify(deleteTaskUseCase).deleteTask(new DeleteTaskCommand(1L, 101L, 10L));
  }

  @Test
  void deleteTaskPropagatesTaskNotFound() throws Exception {
    org.mockito.Mockito.doThrow(new TaskNotFoundException())
        .when(deleteTaskUseCase)
        .deleteTask(any(DeleteTaskCommand.class));

    mockMvc
        .perform(
            delete("/api/workspaces/1/tasks/101").with(authentication(auth())).with(csrf()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_3"));
  }

  @Test
  void deleteTaskPropagatesAccessDenied() throws Exception {
    org.mockito.Mockito.doThrow(new WorkspaceAccessDeniedException())
        .when(deleteTaskUseCase)
        .deleteTask(any(DeleteTaskCommand.class));

    mockMvc
        .perform(
            delete("/api/workspaces/1/tasks/101").with(authentication(auth())).with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  private Authentication auth() {
    return new UsernamePasswordAuthenticationToken(AUTH_USER, null, java.util.List.of());
  }
}
