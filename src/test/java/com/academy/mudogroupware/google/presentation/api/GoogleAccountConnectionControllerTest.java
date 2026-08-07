package com.academy.mudogroupware.google.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.google.application.command.CheckGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.command.CompleteGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.command.DisconnectGoogleAccountCommand;
import com.academy.mudogroupware.google.application.command.StartGoogleConnectionCommand;
import com.academy.mudogroupware.google.application.query.GoogleAccountConnectionView;
import com.academy.mudogroupware.google.application.usecase.CheckGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.application.usecase.CompleteGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.application.usecase.DisconnectGoogleAccountUseCase;
import com.academy.mudogroupware.google.application.usecase.GetGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.application.usecase.StartGoogleAccountConnectionUseCase;
import com.academy.mudogroupware.google.domain.exception.GoogleAccountNotConnectedException;
import com.academy.mudogroupware.google.domain.model.GoogleConnectionStatus;

@WebMvcTest(GoogleAccountConnectionController.class)
class GoogleAccountConnectionControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(7L, "user", 1L, 3L, "OWNER");

    @Autowired private MockMvc mockMvc;

    @MockitoBean private StartGoogleAccountConnectionUseCase startGoogleAccountConnectionUseCase;
    @MockitoBean private CompleteGoogleAccountConnectionUseCase completeGoogleAccountConnectionUseCase;
    @MockitoBean private GetGoogleAccountConnectionUseCase getGoogleAccountConnectionUseCase;
    @MockitoBean private CheckGoogleAccountConnectionUseCase checkGoogleAccountConnectionUseCase;
    @MockitoBean private DisconnectGoogleAccountUseCase disconnectGoogleAccountUseCase;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void startConnectionReturns200WithAuthorizationUrl() throws Exception {
        when(startGoogleAccountConnectionUseCase.start(any(StartGoogleConnectionCommand.class)))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=abc");

        mockMvc.perform(post("/api/google/connections/authorize-url")
                        .with(authentication(authenticatedUser()))
                        .with(csrf())
                        .param("switchAccount", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GOOGLE_200_1"))
                .andExpect(jsonPath("$.data.authorizationUrl").value("https://accounts.google.com/o/oauth2/v2/auth?state=abc"));

        verify(startGoogleAccountConnectionUseCase).start(new StartGoogleConnectionCommand(1L, 7L, true));
    }

    @Test
    void startConnectionReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/google/connections/authorize-url").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(startGoogleAccountConnectionUseCase);
    }

    // /callback은 SecurityConfig에서 permitAll로 열어 둔 엔드포인트지만(구글 리다이렉트는 Authorization
    // 헤더가 없음), @WebMvcTest 슬라이스는 실제 SecurityConfig를 로드하지 않아 인증 없이는 기본 차단된다.
    // 여기서는 콜백 처리 로직(리다이렉트 분기)만 검증한다.

    @Test
    void callbackRedirectsWithSuccessWhenCompleteSucceeds() throws Exception {
        mockMvc.perform(get("/api/google/connections/callback")
                        .with(authentication(authenticatedUser()))
                        .param("code", "auth-code")
                        .param("state", "signed-state"))
                .andExpect(status().isFound());

        verify(completeGoogleAccountConnectionUseCase)
                .complete(new CompleteGoogleConnectionCommand("auth-code", "signed-state"));
    }

    @Test
    void callbackRedirectsWithFailedWhenGoogleReturnsError() throws Exception {
        mockMvc.perform(get("/api/google/connections/callback")
                        .with(authentication(authenticatedUser()))
                        .param("error", "access_denied"))
                .andExpect(status().isFound());

        verifyNoInteractions(completeGoogleAccountConnectionUseCase);
    }

    @Test
    void callbackRedirectsWithFailedWhenCompleteThrows() throws Exception {
        doThrow(new GoogleAccountNotConnectedException())
                .when(completeGoogleAccountConnectionUseCase)
                .complete(any(CompleteGoogleConnectionCommand.class));

        mockMvc.perform(get("/api/google/connections/callback")
                        .with(authentication(authenticatedUser()))
                        .param("code", "auth-code")
                        .param("state", "signed-state"))
                .andExpect(status().isFound());
    }

    @Test
    void getConnectionReturns200WithNullDataWhenNotConnected() throws Exception {
        when(getGoogleAccountConnectionUseCase.getConnection(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/google/connections").with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("GOOGLE_200_2"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getConnectionReturns200WithConnectionDataWhenConnected() throws Exception {
        LocalDateTime connectedAt = LocalDateTime.of(2026, 7, 1, 14, 22);
        GoogleAccountConnectionView view = new GoogleAccountConnectionView(
                "academy@mudo.co.kr", 7L, "drive.file", connectedAt, connectedAt.plusDays(60), connectedAt,
                GoogleConnectionStatus.CONNECTED);
        when(getGoogleAccountConnectionUseCase.getConnection(1L)).thenReturn(Optional.of(view));

        mockMvc.perform(get("/api/google/connections").with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.googleEmail").value("academy@mudo.co.kr"))
                .andExpect(jsonPath("$.data.status").value("CONNECTED"));
    }

    @Test
    void checkConnectionReturns204() throws Exception {
        mockMvc.perform(post("/api/google/connections/check")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(checkGoogleAccountConnectionUseCase).check(new CheckGoogleConnectionCommand(1L));
    }

    @Test
    void checkConnectionReturns404WhenNotConnected() throws Exception {
        doThrow(new GoogleAccountNotConnectedException())
                .when(checkGoogleAccountConnectionUseCase).check(any(CheckGoogleConnectionCommand.class));

        mockMvc.perform(post("/api/google/connections/check")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GOOGLE_404_1"));
    }

    @Test
    void disconnectReturns204() throws Exception {
        mockMvc.perform(delete("/api/google/connections")
                        .with(authentication(authenticatedUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(disconnectGoogleAccountUseCase).disconnect(new DisconnectGoogleAccountCommand(1L));
    }

    @Test
    void disconnectReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/google/connections").with(csrf()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(disconnectGoogleAccountUseCase);
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER,
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }
}
