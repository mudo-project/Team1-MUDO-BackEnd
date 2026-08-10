package com.academy.mudogroupware.workspace.presentation.api.task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.global.domain.common.page.PageResult;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.application.query.task.MyTaskListItem;
import com.academy.mudogroupware.workspace.application.usecase.task.MyTaskListQueryUseCase;
import com.academy.mudogroupware.workspace.domain.model.task.TaskStatus;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MyTaskController.class)
class MyTaskControllerTest {

  private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 1L, 3L, "MEMBER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private MyTaskListQueryUseCase myTaskListQueryUseCase;
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Test
  void getMyTasksReturnsListWithDefaultPaging() throws Exception {
    PageResult<MyTaskListItem> page =
        PageResult.of(
            List.of(
                new MyTaskListItem(
                    101L, 1L, "8월 학사 운영", "9월 시간표 초안 작성", LocalDate.of(2026, 8, 10),
                    TaskStatus.WAITING)),
            0,
            20,
            false);
    when(myTaskListQueryUseCase.getMyTasks(10L, null, null, 0, 20)).thenReturn(page);

    mockMvc
        .perform(get("/api/tasks/me").with(authentication(auth())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_18"))
        .andExpect(jsonPath("$.data.content[0].taskId").value(101))
        .andExpect(jsonPath("$.data.content[0].workspaceId").value(1))
        .andExpect(jsonPath("$.data.content[0].workspaceName").value("8월 학사 운영"))
        .andExpect(jsonPath("$.data.content[0].title").value("9월 시간표 초안 작성"))
        .andExpect(jsonPath("$.data.content[0].dueAt").value("2026-08-10"))
        .andExpect(jsonPath("$.data.content[0].status").value("WAITING"))
        .andExpect(jsonPath("$.data.hasNext").value(false));
  }

  @Test
  void getMyTasksForwardsStatusAndWorkspaceIdFilters() throws Exception {
    PageResult<MyTaskListItem> page = PageResult.of(List.of(), 0, 20, false);
    when(myTaskListQueryUseCase.getMyTasks(10L, TaskStatus.DELAYED, 5L, 0, 20)).thenReturn(page);

    mockMvc
        .perform(get("/api/tasks/me?status=DELAYED&workspaceId=5").with(authentication(auth())))
        .andExpect(status().isOk());

    verify(myTaskListQueryUseCase).getMyTasks(10L, TaskStatus.DELAYED, 5L, 0, 20);
  }

  @Test
  void getMyTasksForwardsCustomPageAndSize() throws Exception {
    PageResult<MyTaskListItem> page = PageResult.of(List.of(), 1, 10, true);
    when(myTaskListQueryUseCase.getMyTasks(10L, null, null, 1, 10)).thenReturn(page);

    mockMvc
        .perform(get("/api/tasks/me?page=1&size=10").with(authentication(auth())))
        .andExpect(status().isOk());

    verify(myTaskListQueryUseCase).getMyTasks(10L, null, null, 1, 10);
  }

  @Test
  void getMyTasksRejectsSizeAboveMax() throws Exception {
    mockMvc
        .perform(get("/api/tasks/me?size=101").with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getMyTasksRejectsNegativePage() throws Exception {
    mockMvc
        .perform(get("/api/tasks/me?page=-1").with(authentication(auth())))
        .andExpect(status().isBadRequest());
  }

  private Authentication auth() {
    return new UsernamePasswordAuthenticationToken(AUTH_USER, null, List.of());
  }
}
