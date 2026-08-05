package com.academy.mudogroupware.workspace.presentation.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.application.query.WorkspaceListItem;
import com.academy.mudogroupware.workspace.application.query.WorkspaceListScope;
import com.academy.mudogroupware.workspace.application.usecase.CreateWorkspaceUseCase;
import com.academy.mudogroupware.workspace.application.usecase.RecordWorkspaceRecentAccessUseCase;
import com.academy.mudogroupware.workspace.application.usecase.WorkspaceQueryUseCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WorkspaceController.class)
@Import(WorkspaceControllerTest.MethodSecurityConfiguration.class)
class WorkspaceControllerTest {

  private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 1L, 3L, "MEMBER");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CreateWorkspaceUseCase createWorkspaceUseCase;
  @MockitoBean private WorkspaceQueryUseCase workspaceQueryUseCase;
  @MockitoBean private RecordWorkspaceRecentAccessUseCase recordWorkspaceRecentAccessUseCase;
  @MockitoBean private JwtTokenProvider jwtTokenProvider;
  @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

  @Test
  void defaultsToMineAndMapsAuthenticatedUsersWorkspaceList() throws Exception {
    when(workspaceQueryUseCase.getWorkspaces(1L, 10L, WorkspaceListScope.MINE))
        .thenReturn(List.of(new WorkspaceListItem(100L, "8월 학사 운영", 3L)));

    mockMvc
        .perform(get("/api/workspaces").with(authentication(authenticatedUser())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_1"))
        .andExpect(jsonPath("$.message").value("워크스페이스 목록 조회에 성공했습니다."))
        .andExpect(jsonPath("$.data[0].workspaceId").value(100))
        .andExpect(jsonPath("$.data[0].name").value("8월 학사 운영"))
        .andExpect(jsonPath("$.data[0].memberCount").value(3));

    verify(workspaceQueryUseCase).getWorkspaces(1L, 10L, WorkspaceListScope.MINE);
  }

  @Test
  void returnsEmptyDataListWhenMineHasNoWorkspaces() throws Exception {
    when(workspaceQueryUseCase.getWorkspaces(1L, 10L, WorkspaceListScope.MINE))
        .thenReturn(List.of());

    mockMvc
        .perform(get("/api/workspaces").with(authentication(authenticatedUser())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data").isEmpty());
  }

  @Test
  void rejectsAllBeforePermissionModuleIntegrationEvenWithReadAllAuthority() throws Exception {
    mockMvc
        .perform(
            get("/api/workspaces")
                .param("scope", "ALL")
                .with(authentication(authenticatedUser("WORKSPACE:READ_ALL"))))
        .andExpect(status().isForbidden());

    verifyNoInteractions(workspaceQueryUseCase);
  }

  @Test
  void rejectsAllWithoutAuthorityBeforeCallingQueryUseCase() throws Exception {
    mockMvc
        .perform(
            get("/api/workspaces")
                .param("scope", "ALL")
                .with(authentication(authenticatedUser())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("COMMON_403_1"));

    verifyNoInteractions(workspaceQueryUseCase);
  }

  @Test
  void recordsRecentAccessWithAuthenticatedUserAndReturnsNoContent() throws Exception {
    mockMvc
        .perform(
            put("/api/workspaces/{workspaceId}/recent-access", 100L)
                .with(authentication(authenticatedUser()))
                .with(csrf()))
        .andExpect(status().isNoContent())
        .andExpect(content().string(""));

    verify(recordWorkspaceRecentAccessUseCase).recordRecentAccess(1L, 10L, 100L, false);
  }

  @Test
  void forwardsReadAllAuthorityWhenRecordingRecentAccess() throws Exception {
    mockMvc
        .perform(
            put("/api/workspaces/{workspaceId}/recent-access", 100L)
                .with(authentication(authenticatedUser("WORKSPACE:READ_ALL")))
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(recordWorkspaceRecentAccessUseCase).recordRecentAccess(1L, 10L, 100L, true);
  }

  private Authentication authenticatedUser(String... authorities) {
    return new UsernamePasswordAuthenticationToken(
        AUTH_USER,
        null,
        List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
  }

  @EnableMethodSecurity
  static class MethodSecurityConfiguration {}
}
