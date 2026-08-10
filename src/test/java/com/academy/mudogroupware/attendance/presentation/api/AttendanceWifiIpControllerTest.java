package com.academy.mudogroupware.attendance.presentation.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.academy.mudogroupware.attendance.application.usecase.DeleteWifiIpUseCase;
import com.academy.mudogroupware.attendance.application.usecase.GetWifiIpsUseCase;
import com.academy.mudogroupware.attendance.application.usecase.RegisterWifiIpUseCase;
import com.academy.mudogroupware.attendance.domain.model.AcademyWifiIp;
import com.academy.mudogroupware.attendance.presentation.api.response.AcademyWifiIpResponse;
import com.academy.mudogroupware.attendance.presentation.api.response.CurrentClientIpResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import jakarta.servlet.http.HttpServletRequest;

class AttendanceWifiIpControllerTest {

    @Test
    void returnsCurrentClientIpForConfirmation() {
        RegisterWifiIpUseCase registerWifiIpUseCase = mock(RegisterWifiIpUseCase.class);
        DeleteWifiIpUseCase deleteWifiIpUseCase = mock(DeleteWifiIpUseCase.class);
        GetWifiIpsUseCase getWifiIpsUseCase = mock(GetWifiIpsUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        AttendanceWifiIpController controller =
                new AttendanceWifiIpController(
                        registerWifiIpUseCase, deleteWifiIpUseCase,
                        getWifiIpsUseCase, clientIpResolver);

        when(clientIpResolver.resolve(request)).thenReturn("203.0.113.10");

        GlobalApiResponse<CurrentClientIpResponse> response =
                controller.getCurrentClientIp(request);

        assertEquals(200, response.status());
        assertEquals("ACADEMY_200_1", response.code());
        assertEquals("203.0.113.10", response.data().ipAddress());
    }

    @Test
    void returnsRegisteredWifiIpsWithGlobalResponse() {
        RegisterWifiIpUseCase registerWifiIpUseCase = mock(RegisterWifiIpUseCase.class);
        DeleteWifiIpUseCase deleteWifiIpUseCase = mock(DeleteWifiIpUseCase.class);
        GetWifiIpsUseCase getWifiIpsUseCase = mock(GetWifiIpsUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        AttendanceWifiIpController controller = new AttendanceWifiIpController(
                registerWifiIpUseCase, deleteWifiIpUseCase,
                getWifiIpsUseCase, clientIpResolver);
        AuthUser authUser = new AuthUser(10L, "owner", 1L, 1L, "OWNER");
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 30);
        when(getWifiIpsUseCase.getAll(10L)).thenReturn(List.of(
                AcademyWifiIp.restore(
                        5L, "203.0.113.10", "본관 와이파이", createdAt, createdAt)));

        GlobalApiResponse<List<AcademyWifiIpResponse>> response = controller.getAll(authUser);

        assertEquals(200, response.status());
        assertEquals("ACADEMY_200_3", response.code());
        assertEquals(1, response.data().size());
        assertEquals(5L, response.data().get(0).wifiIpId());
        assertEquals("203.0.113.10", response.data().get(0).ipAddress());
        verify(getWifiIpsUseCase).getAll(10L);
    }

    @Test
    void returnsEmptyListWhenNoWifiIpIsRegistered() {
        RegisterWifiIpUseCase registerWifiIpUseCase = mock(RegisterWifiIpUseCase.class);
        DeleteWifiIpUseCase deleteWifiIpUseCase = mock(DeleteWifiIpUseCase.class);
        GetWifiIpsUseCase getWifiIpsUseCase = mock(GetWifiIpsUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        AttendanceWifiIpController controller = new AttendanceWifiIpController(
                registerWifiIpUseCase, deleteWifiIpUseCase,
                getWifiIpsUseCase, clientIpResolver);
        AuthUser authUser = new AuthUser(10L, "owner", 1L, 1L, "OWNER");
        when(getWifiIpsUseCase.getAll(10L)).thenReturn(List.of());

        GlobalApiResponse<List<AcademyWifiIpResponse>> response = controller.getAll(authUser);

        assertEquals(List.of(), response.data());
    }

    @Test
    void deletesWifiIpAndReturnsGlobalResponse() {
        RegisterWifiIpUseCase registerWifiIpUseCase = mock(RegisterWifiIpUseCase.class);
        DeleteWifiIpUseCase deleteWifiIpUseCase = mock(DeleteWifiIpUseCase.class);
        GetWifiIpsUseCase getWifiIpsUseCase = mock(GetWifiIpsUseCase.class);
        ClientIpResolver clientIpResolver = mock(ClientIpResolver.class);
        AttendanceWifiIpController controller = new AttendanceWifiIpController(
                registerWifiIpUseCase, deleteWifiIpUseCase,
                getWifiIpsUseCase, clientIpResolver);
        AuthUser authUser = new AuthUser(10L, "owner", 1L, 1L, "OWNER");

        ResponseEntity<GlobalApiResponse<Void>> response = controller.delete(authUser, 5L);

        verify(deleteWifiIpUseCase).delete(10L, 5L);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("ACADEMY_200_2", response.getBody().code());
        assertEquals(null, response.getBody().data());
    }
}
