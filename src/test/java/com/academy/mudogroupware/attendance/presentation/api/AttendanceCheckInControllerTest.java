package com.academy.mudogroupware.attendance.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.attendance.application.command.CheckInCommand;
import com.academy.mudogroupware.attendance.application.result.CheckInResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckInUseCase;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.presentation.api.request.CheckInRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.CheckInResponse;
import com.academy.mudogroupware.global.infrastructure.security.config.SecurityConfig;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationFilter;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAccessDeniedHandler;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(AttendanceCheckInController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class AttendanceCheckInControllerTest {

    private static final AuthUser AUTH_USER =
            new AuthUser(10L, "employee", 2L, "EMPLOYEE");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckInUseCase webCheckInUseCase;
    @MockitoBean
    private ClientIpResolver webClientIpResolver;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void checksInWithAuthenticatedAcademyAndDetectedIp() {
        CheckInUseCase checkInUseCase = mock(CheckInUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        AttendanceCheckInController controller =
                new AttendanceCheckInController(checkInUseCase, clientIpResolver);
        AuthUser authUser = new AuthUser(10L, "employee", 2L, "EMPLOYEE");
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 5, 9, 11);
        CheckInCommand command = new CheckInCommand(
                10L, "203.0.113.10", "교통 정체");
        when(clientIpResolver.resolve(servletRequest)).thenReturn("203.0.113.10");
        when(checkInUseCase.checkIn(command)).thenReturn(new CheckInResult(
                5L, LocalDate.of(2026, 8, 5), clockInAt,
                "교통 정체", AttendanceStatus.LATE));

        ResponseEntity<GlobalApiResponse<CheckInResponse>> response = controller.checkIn(
                authUser, new CheckInRequest("교통 정체"), servletRequest);

        verify(checkInUseCase).checkIn(command);
        assertEquals(201, response.getStatusCode().value());
        assertEquals("ATTENDANCE_201_1", response.getBody().code());
        assertEquals(AttendanceStatus.LATE, response.getBody().data().status());
    }

    @Test
    void allowsAuthenticatedCheckInWithoutDedicatedAuthority() throws Exception {
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 12, 8, 55);
        CheckInCommand command = new CheckInCommand(10L, "203.0.113.10", null);
        when(webClientIpResolver.resolve(any(HttpServletRequest.class)))
                .thenReturn("203.0.113.10");
        when(webCheckInUseCase.checkIn(command)).thenReturn(new CheckInResult(
                5L, LocalDate.of(2026, 8, 12), clockInAt,
                null, AttendanceStatus.NORMAL));

        mockMvc.perform(post("/api/attendance/check-ins")
                        .with(authentication(authenticatedUser()))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated());

        verify(webCheckInUseCase).checkIn(command);
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        return new UsernamePasswordAuthenticationToken(AUTH_USER, null, java.util.List.of());
    }
}
