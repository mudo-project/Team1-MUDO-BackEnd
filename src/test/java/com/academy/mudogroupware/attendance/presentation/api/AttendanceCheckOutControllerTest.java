package com.academy.mudogroupware.attendance.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;
import com.academy.mudogroupware.attendance.application.result.CheckOutResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckOutUseCase;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.model.ClockOutType;
import com.academy.mudogroupware.attendance.presentation.api.request.CheckOutRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.CheckOutResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.infrastructure.security.config.SecurityConfig;
import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationFilter;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAccessDeniedHandler;
import com.academy.mudogroupware.global.presentation.security.handler.CustomAuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;

@WebMvcTest(AttendanceCheckOutController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        CustomAccessDeniedHandler.class
})
class AttendanceCheckOutControllerTest {

    private static final AuthUser AUTH_USER =
            new AuthUser(10L, "employee", 2L, "EMPLOYEE");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckOutUseCase webCheckOutUseCase;
    @MockitoBean
    private ClientIpResolver webClientIpResolver;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void checksOutNormallyWithoutNoteAndReturnsGlobalResponse() {
        CheckOutUseCase checkOutUseCase = mock(CheckOutUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        AttendanceCheckOutController controller =
                new AttendanceCheckOutController(checkOutUseCase, clientIpResolver);
        AuthUser authUser = new AuthUser(10L, "employee", 2L, "EMPLOYEE");
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 5, 22, 0);
        LocalDateTime clockOutAt = LocalDateTime.of(2026, 8, 6, 2, 0);
        CheckOutCommand command = new CheckOutCommand(
                10L, "203.0.113.10", ClockOutType.NORMAL, null);
        when(clientIpResolver.resolve(servletRequest)).thenReturn("203.0.113.10");
        when(checkOutUseCase.checkOut(command)).thenReturn(new CheckOutResult(
                5L, LocalDate.of(2026, 8, 5), clockInAt,
                clockOutAt, ClockOutType.NORMAL, null, AttendanceStatus.NORMAL));

        ResponseEntity<GlobalApiResponse<CheckOutResponse>> response =
                controller.checkOut(
                        authUser, new CheckOutRequest(ClockOutType.NORMAL, null),
                        servletRequest);

        verify(checkOutUseCase).checkOut(command);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("ATTENDANCE_200_2", response.getBody().code());
        assertEquals(clockOutAt, response.getBody().data().clockOutAt());
        assertEquals(ClockOutType.NORMAL, response.getBody().data().clockOutType());
        assertNull(response.getBody().data().clockOutNote());
    }

    @Test
    void rejectsUnauthenticatedCheckOut() throws Exception {
        mockMvc.perform(post("/api/attendance/check-outs")
                        .contentType("application/json")
                        .content("{\"clockOutType\":\"NORMAL\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("COMMON_401_1"));

        verifyNoInteractions(webCheckOutUseCase, webClientIpResolver);
    }

    @Test
    void allowsAuthenticatedCheckOutWithoutDedicatedAuthority() throws Exception {
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 12, 8, 55);
        LocalDateTime clockOutAt = LocalDateTime.of(2026, 8, 12, 18, 0);
        CheckOutCommand command = new CheckOutCommand(
                10L, "203.0.113.10", ClockOutType.NORMAL, null);
        when(webClientIpResolver.resolve(any(HttpServletRequest.class)))
                .thenReturn("203.0.113.10");
        when(webCheckOutUseCase.checkOut(command)).thenReturn(new CheckOutResult(
                5L, LocalDate.of(2026, 8, 12), clockInAt,
                clockOutAt, ClockOutType.NORMAL, null, AttendanceStatus.NORMAL));

        mockMvc.perform(post("/api/attendance/check-outs")
                        .with(authentication(authenticatedUser()))
                        .contentType("application/json")
                        .content("{\"clockOutType\":\"NORMAL\"}"))
                .andExpect(status().isOk());

        verify(webCheckOutUseCase).checkOut(command);
    }

    @Test
    void rejectsCheckOutWithoutClockOutType() throws Exception {
        mockMvc.perform(post("/api/attendance/check-outs")
                        .with(authentication(authenticatedUser()))
                        .contentType("application/json")
                        .content("{\"clockOutNote\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));

        verifyNoInteractions(webCheckOutUseCase, webClientIpResolver);
    }

    @Test
    void rejectsCheckOutNoteWith256Characters() throws Exception {
        String body = "{\"clockOutType\":\"NORMAL\",\"clockOutNote\":\""
                + "a".repeat(256) + "\"}";

        mockMvc.perform(post("/api/attendance/check-outs")
                        .with(authentication(authenticatedUser()))
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("COMMON_400_1"));

        verifyNoInteractions(webCheckOutUseCase, webClientIpResolver);
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        return new UsernamePasswordAuthenticationToken(AUTH_USER, null, java.util.List.of());
    }
}
