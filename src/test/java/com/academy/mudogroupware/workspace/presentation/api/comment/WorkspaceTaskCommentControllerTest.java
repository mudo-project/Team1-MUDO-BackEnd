package com.academy.mudogroupware.workspace.presentation.api.comment;

import static org.mockito.ArgumentMatchers.any;
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
import com.academy.mudogroupware.workspace.application.command.comment.CreateTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.command.comment.DeleteTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.command.comment.ToggleTaskCommentCompleteCommand;
import com.academy.mudogroupware.workspace.application.command.comment.UpdateTaskCommentCommand;
import com.academy.mudogroupware.workspace.application.query.comment.TaskCommentListItem;
import com.academy.mudogroupware.workspace.application.query.workspace.WorkspaceMemberInfo;
import com.academy.mudogroupware.workspace.application.usecase.comment.CreateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.DeleteTaskCommentUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.TaskCommentListQueryUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.ToggleTaskCommentCompleteUseCase;
import com.academy.mudogroupware.workspace.application.usecase.comment.UpdateTaskCommentUseCase;
import com.academy.mudogroupware.workspace.domain.exception.comment.InvalidMentionedUserException;
import com.academy.mudogroupware.workspace.domain.exception.comment.TaskCommentNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.task.TaskNotFoundException;
import com.academy.mudogroupware.workspace.domain.exception.workspace.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.model.comment.TaskComment;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceTaskCommentController.class)
class WorkspaceTaskCommentControllerTest {

  private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 3L, "MEMBER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateTaskCommentUseCase createTaskCommentUseCase;
  @MockitoBean private TaskCommentListQueryUseCase taskCommentListQueryUseCase;
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
  void getCommentsReturnsPagedList() throws Exception {
    TaskCommentListItem item =
        new TaskCommentListItem(
            1L, "수학A반 완료", new WorkspaceMemberInfo(10L, "윤예진"), true,
            LocalDateTime.of(2026, 8, 1, 16, 0));
    when(taskCommentListQueryUseCase.getComments(1L, 101L, 10L, 0, 20, false))
        .thenReturn(PageResult.of(List.of(item), 0, 20, false));

    mockMvc
        .perform(get("/api/workspaces/1/tasks/101/comments").with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_17"))
        .andExpect(jsonPath("$.data.content[0].commentId").value(1))
        .andExpect(jsonPath("$.data.content[0].content").value("수학A반 완료"))
        .andExpect(jsonPath("$.data.content[0].author.name").value("윤예진"))
        .andExpect(jsonPath("$.data.content[0].completed").value(true))
        .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-08-01T16:00:00"))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void getCommentsRejectsNegativePage() throws Exception {
    mockMvc
        .perform(
            get("/api/workspaces/1/tasks/101/comments?page=-1").with(authentication(auth())))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(taskCommentListQueryUseCase);
  }

  @Test
  void getCommentsRejectsZeroSize() throws Exception {
    mockMvc
        .perform(
            get("/api/workspaces/1/tasks/101/comments?size=0").with(authentication(auth())))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(taskCommentListQueryUseCase);
  }

  @Test
  void getCommentsRejectsSizeAbove100() throws Exception {
    mockMvc
        .perform(
            get("/api/workspaces/1/tasks/101/comments?size=101").with(authentication(auth())))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(taskCommentListQueryUseCase);
  }

  @Test
  void getCommentsUsesRequestedPageAndSize() throws Exception {
    when(taskCommentListQueryUseCase.getComments(1L, 101L, 10L, 2, 5, false))
        .thenReturn(PageResult.of(List.of(), 2, 5, false));

    mockMvc
        .perform(
            get("/api/workspaces/1/tasks/101/comments?page=2&size=5").with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.page").value(2))
        .andExpect(jsonPath("$.data.size").value(5));

    verify(taskCommentListQueryUseCase).getComments(1L, 101L, 10L, 2, 5, false);
  }

  @Test
  void getCommentsPropagatesTaskNotFound() throws Exception {
    when(taskCommentListQueryUseCase.getComments(1L, 101L, 10L, 0, 20, false))
        .thenThrow(new TaskNotFoundException());

    mockMvc
        .perform(get("/api/workspaces/1/tasks/101/comments").with(authentication(auth())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_3"));
  }

  @Test
  void getCommentsPropagatesAccessDenied() throws Exception {
    when(taskCommentListQueryUseCase.getComments(1L, 101L, 10L, 0, 20, false))
        .thenThrow(new WorkspaceAccessDeniedException());

    mockMvc
        .perform(get("/api/workspaces/1/tasks/101/comments").with(authentication(auth())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void getCommentsForwardsReadAllAuthority() throws Exception {
    when(taskCommentListQueryUseCase.getComments(1L, 101L, 10L, 0, 20, true))
        .thenReturn(PageResult.of(List.of(), 0, 20, false));

    mockMvc
        .perform(
            get("/api/workspaces/1/tasks/101/comments")
                .with(authentication(auth("WORKSPACE:READ_ALL"))))
        .andExpect(status().isOk());

    verify(taskCommentListQueryUseCase).getComments(1L, 101L, 10L, 0, 20, true);
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
  void deleteCommentReturnsOkWithSuccessMessage() throws Exception {
    mockMvc
        .perform(
            delete("/api/workspaces/1/tasks/101/comments/501")
                .with(authentication(auth()))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_14"))
        .andExpect(jsonPath("$.message").value("업무 댓글 삭제에 성공했습니다."));

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

  private Authentication auth(String... authorities) {
    return new UsernamePasswordAuthenticationToken(
        AUTH_USER,
        null,
        java.util.List.of(authorities).stream()
            .map(org.springframework.security.core.authority.SimpleGrantedAuthority::new)
            .toList());
  }
}
