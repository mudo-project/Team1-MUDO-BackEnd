package com.academy.mudogroupware.attendance.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.academy.mudogroupware.attendance.application.command.CheckInCommand;
import com.academy.mudogroupware.attendance.application.result.CheckInResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckInUseCase;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.presentation.api.request.CheckInRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.CheckInResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import jakarta.servlet.http.HttpServletRequest;

class AttendanceCheckInControllerTest {

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
}
