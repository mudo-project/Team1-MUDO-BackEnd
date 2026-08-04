package com.academy.mudogroupware.attendance.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.academy.mudogroupware.attendance.application.usecase.RegisterWifiIpUseCase;
import com.academy.mudogroupware.attendance.presentation.api.response.CurrentClientIpResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;

import jakarta.servlet.http.HttpServletRequest;

class AttendanceWifiIpControllerTest {

    @Test
    void returnsCurrentClientIpForConfirmation() {
        RegisterWifiIpUseCase registerWifiIpUseCase = mock(RegisterWifiIpUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AttendanceWifiIpController controller =
                new AttendanceWifiIpController(registerWifiIpUseCase, clientIpResolver);

        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");

        GlobalApiResponse<CurrentClientIpResponse> response =
                controller.getCurrentClientIp(request);

        assertEquals(200, response.status());
        assertEquals("ACADEMY_200_1", response.code());
        assertEquals("203.0.113.10", response.data().ipAddress());
    }
}
