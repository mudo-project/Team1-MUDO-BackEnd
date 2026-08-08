package com.academy.mudogroupware.workspace.presentation.api.comment;

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
import com.academy.mudogroupware.workspace.application.command.comment.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.command.comment.DeleteTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.command.comment.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.application.command.comment.UpdateTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.usecase.comment.CreateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.DeleteTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.ToggleTaskCommentCompleteUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.UpdateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.domain.exception.comment.InvalidMentionedUserException;
import com.academy.mudogroupware.workspace.domain.exception.comment.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceTaskCommentController.class)
class WorkspaceTaskCommentControllerTest {

  private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 1L, 3L, "MEMBER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateTaskCommentUseCase createTaskCommentUseCase;
  @MockitoBean private UpdateTaskCommentUseCase updateTaskCommentUseCase;
  @MockitoBean private DeleteTaskCommentUseCase deleteTaskCommentUseCase;
  @MockitoBean private ToggleTaskCommentCompleteUseCase toggleTaskCommentCompleteUseCase;
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

  private TaskComment sampleComment(boolean completed) {
    return TaskComment.restore(
        501L, 101L, 10L, "확인 부탁드립니다", completed, completed ? 10L : null,
        completed ? LocalDateTime.of(2026, 8, 7, 11, 0) : null, List.of(),
        LocalDateTime.of(2026, 8, 7, 10, 0), LocalDateTime.of(2026, 8, 7, 10, 0));
  }

  @Test
  void createCommentReturnsCreated() throws Exception {
    when(createTaskCommentUseCase.createComment(any(CreateTaskCommentCommand.class)))
        .thenReturn(sampleComment(false));

    mockMvc
        .perform(
            post("/api/workspaces/1/tasks/101/comments")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"확인 부탁드립니다\",\"mentionedUserIds\":[]}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.commentId").value(501))
        .andExpect(jsonPath("$.data.content").value("확인 부탁드립니다"))
        .andExpect(jsonPath("$.data.completed").value(false));

    verify(createTaskCommentUseCase).createComment(any(CreateTaskCommentCommand.class));
  }

  @Test
  void createCommentRejectsBlankContent() throws Exception {
    mockMvc
        .perform(
            post("/api/workspaces/1/tasks/101/comments")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"   \"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(createTaskCommentUseCase);
  }

  @Test
  void createCommentPropagatesInvalidMentionedUser() throws Exception {
    when(createTaskCommentUseCase.createComment(any(CreateTaskCommentCommand.class)))
        .thenThrow(new InvalidMentionedUserException());

    mockMvc
        .perform(
            post("/api/workspaces/1/tasks/101/comments")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"내용\",\"mentionedUserIds\":[99]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WORKSPACE_400_6"));
  }

  @Test
  void updateCommentReturnsOk() throws Exception {
    when(updateTaskCommentUseCase.updateComment(any(UpdateTaskCommentCommand.class)))
        .thenReturn(sampleComment(false));

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101/comments/501")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"수정\",\"mentionedUserIds\":[]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.commentId").value(501));

    verify(updateTaskCommentUseCase).updateComment(any(UpdateTaskCommentCommand.class));
  }

  @Test
  void updateCommentPropagatesCommentNotFound() throws Exception {
    when(updateTaskCommentUseCase.updateComment(any(UpdateTaskCommentCommand.class)))
        .thenThrow(new TaskCommentNotFoundException());

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101/comments/501")
                .with(authentication(auth()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"수정\",\"mentionedUserIds\":[]}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_4"));
  }

  @Test
  void deleteCommentReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            delete("/api/workspaces/1/tasks/101/comments/501")
                .with(authentication(auth()))
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(deleteTaskCommentUseCase)
        .deleteComment(new DeleteTaskCommentCommand(1L, 101L, 501L, 10L));
  }

  @Test
  void deleteCommentPropagatesAccessDenied() throws Exception {
    org.mockito.Mockito.doThrow(new WorkspaceAccessDeniedException())
        .when(deleteTaskCommentUseCase)
        .deleteComment(any(DeleteTaskCommentCommand.class));

    mockMvc
        .perform(
            delete("/api/workspaces/1/tasks/101/comments/501")
                .with(authentication(auth()))
                .with(csrf()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void toggleCompleteReturnsOk() throws Exception {
    when(toggleTaskCommentCompleteUseCase.toggleComplete(any(ToggleTaskCommentCompleteCommand.class)))
        .thenReturn(sampleComment(true));

    mockMvc
        .perform(
            patch("/api/workspaces/1/tasks/101/comments/501/complete")
                .with(authentication(auth()))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.completed").value(true))
        .andExpect(jsonPath("$.data.completedBy").value(10));

    verify(toggleTaskCommentCompleteUseCase)
        .toggleComplete(new ToggleTaskCommentCompleteCommand(1L, 101L, 501L, 10L));
  }

  private Authentication auth() {
    return new UsernamePasswordAuthenticationToken(AUTH_USER, null, java.util.List.of());
  }
}
