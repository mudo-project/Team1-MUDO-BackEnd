package com.academy.mudogroupware.attendance.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.academy.mudogroupware.attendance.application.command.CheckOutCommand;
import com.academy.mudogroupware.attendance.application.result.CheckOutResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckOutUseCase;
import com.academy.mudogroupware.attendance.domain.model.AttendanceStatus;
import com.academy.mudogroupware.attendance.domain.model.ClockOutType;
import com.academy.mudogroupware.attendance.presentation.api.request.CheckOutRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.CheckOutResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import jakarta.servlet.http.HttpServletRequest;

class AttendanceCheckOutControllerTest {

    @Test
    void checksOutNormallyWithoutNoteAndReturnsGlobalResponse() {
        CheckOutUseCase checkOutUseCase = mock(CheckOutUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        AttendanceCheckOutController controller =
                new AttendanceCheckOutController(checkOutUseCase, clientIpResolver);
        AuthUser authUser = new AuthUser(10L, "employee", 1L, 2L, "EMPLOYEE");
        LocalDateTime clockInAt = LocalDateTime.of(2026, 8, 5, 22, 0);
        LocalDateTime clockOutAt = LocalDateTime.of(2026, 8, 6, 2, 0);
        CheckOutCommand command = new CheckOutCommand(
                10L, 1L, "203.0.113.10", ClockOutType.NORMAL, null);
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
}
