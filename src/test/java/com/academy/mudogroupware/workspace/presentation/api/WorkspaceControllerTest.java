package com.academy.mudogroupware.workspace.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.workspace.application.query.WorkspaceDetail;
import com.academy.mudogroupware.workspace.application.query.WorkspaceListItem;
import com.academy.mudogroupware.workspace.application.query.WorkspaceListScope;
import com.academy.mudogroupware.workspace.application.command.DeleteWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.command.RenameWorkspaceCommand;
import com.academy.mudogroupware.workspace.application.usecase.CreateWorkspaceUseCase;
import com.academy.mudogroupware.workspace.application.usecase.DeleteWorkspaceUseCase;
import com.academy.mudogroupware.workspace.application.usecase.RecordWorkspaceRecentAccessUseCase;
import com.academy.mudogroupware.workspace.application.usecase.RenameWorkspaceUseCase;
import com.academy.mudogroupware.workspace.application.usecase.WorkspaceDetailQueryUseCase;
import com.academy.mudogroupware.workspace.application.usecase.WorkspaceQueryUseCase;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceAccessDeniedException;
import com.academy.mudogroupware.workspace.domain.exception.WorkspaceNotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
  @MockitoBean private WorkspaceDetailQueryUseCase workspaceDetailQueryUseCase;
  @MockitoBean private RenameWorkspaceUseCase renameWorkspaceUseCase;
  @MockitoBean private DeleteWorkspaceUseCase deleteWorkspaceUseCase;
  @MockitoBean private Clock clock;

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

  @Test
  void returns404WhenWorkspaceDoesNotExist() throws Exception {
    when(workspaceDetailQueryUseCase.getWorkspaceDetail(
            eq(1L), eq(10L), eq(100L), any(LocalDate.class), eq(false)))
        .thenThrow(new WorkspaceNotFoundException());

    mockMvc
        .perform(
            get("/api/workspaces/{workspaceId}", 100L)
                .param("date", "2026-08-05")
                .with(authentication(authenticatedUser())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.code").value("WORKSPACE_404_1"));
  }

  @Test
  void returns200WithDetailWhenAccessible() throws Exception {
    WorkspaceDetail detail = new WorkspaceDetail(100L, "1월 학사 운영", List.of(), List.of());
    when(workspaceDetailQueryUseCase.getWorkspaceDetail(
            eq(1L), eq(10L), eq(100L), eq(LocalDate.of(2026, 8, 5)), eq(false)))
        .thenReturn(detail);

    mockMvc
        .perform(
            get("/api/workspaces/{workspaceId}", 100L)
                .param("date", "2026-08-05")
                .with(authentication(authenticatedUser())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_2"))
        .andExpect(jsonPath("$.data.workspaceId").value(100))
        .andExpect(jsonPath("$.data.name").value("1월 학사 운영"));
  }

  @Test
  void usesClockDateWhenDateParameterIsOmitted() throws Exception {
    Clock fixedClock =
        Clock.fixed(LocalDate.of(2026, 8, 5).atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    when(clock.instant()).thenReturn(fixedClock.instant());
    when(clock.getZone()).thenReturn(fixedClock.getZone());
    WorkspaceDetail detail = new WorkspaceDetail(100L, "1월 학사 운영", List.of(), List.of());
    when(workspaceDetailQueryUseCase.getWorkspaceDetail(
            eq(1L), eq(10L), eq(100L), eq(LocalDate.of(2026, 8, 5)), eq(false)))
        .thenReturn(detail);

    mockMvc
        .perform(
            get("/api/workspaces/{workspaceId}", 100L).with(authentication(authenticatedUser())))
        .andExpect(status().isOk());

    verify(workspaceDetailQueryUseCase)
        .getWorkspaceDetail(1L, 10L, 100L, LocalDate.of(2026, 8, 5), false);
  }

  @Test
  void forwardsReadAllAuthorityWhenRequestingWorkspaceDetail() throws Exception {
    WorkspaceDetail detail = new WorkspaceDetail(100L, "1월 학사 운영", List.of(), List.of());
    when(workspaceDetailQueryUseCase.getWorkspaceDetail(
            eq(1L), eq(10L), eq(100L), eq(LocalDate.of(2026, 8, 5)), eq(true)))
        .thenReturn(detail);

    mockMvc
        .perform(
            get("/api/workspaces/{workspaceId}", 100L)
                .param("date", "2026-08-05")
                .with(authentication(authenticatedUser("WORKSPACE:READ_ALL"))))
        .andExpect(status().isOk());

    verify(workspaceDetailQueryUseCase)
        .getWorkspaceDetail(1L, 10L, 100L, LocalDate.of(2026, 8, 5), true);
  }

  @Test
  void returns403WhenRequesterCannotAccessWorkspace() throws Exception {
    when(workspaceDetailQueryUseCase.getWorkspaceDetail(
            eq(1L), eq(10L), eq(100L), any(LocalDate.class), eq(false)))
        .thenThrow(new WorkspaceAccessDeniedException());

    mockMvc
        .perform(
            get("/api/workspaces/{workspaceId}", 100L)
                .param("date", "2026-08-05")
                .with(authentication(authenticatedUser())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void renamesWorkspaceAndReturns200() throws Exception {
    when(renameWorkspaceUseCase.rename(
            new RenameWorkspaceCommand(10L, 100L, "운영팀")))
        .thenReturn("운영팀");

    mockMvc
        .perform(
            patch("/api/workspaces/{workspaceId}", 100L)
                .with(authentication(authenticatedUser()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"운영팀\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("WORKSPACE_200_3"))
        .andExpect(jsonPath("$.data.workspaceId").value(100))
        .andExpect(jsonPath("$.data.name").value("운영팀"));
  }

  @Test
  void returns403WhenRenamingWorkspaceRequesterCannotAccess() throws Exception {
    when(renameWorkspaceUseCase.rename(any(RenameWorkspaceCommand.class)))
        .thenThrow(new WorkspaceAccessDeniedException());

    mockMvc
        .perform(
            patch("/api/workspaces/{workspaceId}", 100L)
                .with(authentication(authenticatedUser()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"운영팀\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("WORKSPACE_403_1"));
  }

  @Test
  void returns400WhenRenameRequestHasBlankName() throws Exception {
    mockMvc
        .perform(
            patch("/api/workspaces/{workspaceId}", 100L)
                .with(authentication(authenticatedUser()))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  \"}"))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(renameWorkspaceUseCase);
  }

  @Test
  void deletesWorkspaceAndReturns204() throws Exception {
    mockMvc
        .perform(
            delete("/api/workspaces/{workspaceId}", 100L)
                .with(authentication(authenticatedUser()))
                .with(csrf()))
        .andExpect(status().isNoContent());

    verify(deleteWorkspaceUseCase).delete(new DeleteWorkspaceCommand(10L, 100L));
  }

  @Test
  void returns404WhenDeletingNonExistentWorkspace() throws Exception {
    org.mockito.Mockito.doThrow(new WorkspaceNotFoundException())
        .when(deleteWorkspaceUseCase)
        .delete(any(DeleteWorkspaceCommand.class));

    mockMvc
        .perform(
            delete("/api/workspaces/{workspaceId}", 100L)
                .with(authentication(authenticatedUser()))
                .with(csrf()))
        .andExpect(status().isNotFound());
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
