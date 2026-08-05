package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.result.RegisterWifiIpResult;
import com.academy.mudogroupware.attendance.application.usecase.DeleteWifiIpUseCase;
import com.academy.mudogroupware.attendance.application.usecase.RegisterWifiIpUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.request.RegisterWifiIpRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.AcademyWifiIpResponse;
import com.academy.mudogroupware.attendance.presentation.api.response.CurrentClientIpResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendance/wifi-ips")
@RequiredArgsConstructor
public class AttendanceWifiIpController {

    private final RegisterWifiIpUseCase registerWifiIpUseCase;
    private final DeleteWifiIpUseCase deleteWifiIpUseCase;
    private final ClientIpResolver clientIpResolver;

    @PreAuthorize("hasAuthority('ATTENDANCE:WIFI_IP_MANAGE')")
    @GetMapping("/current")
    public GlobalApiResponse<CurrentClientIpResponse> getCurrentClientIp(
            HttpServletRequest servletRequest) {
        String clientIp = clientIpResolver.resolve(servletRequest);

        return GlobalApiResponse.ok(
                AttendanceResponseCode.CURRENT_CLIENT_IP_RETRIEVED,
                new CurrentClientIpResponse(clientIp));
    }

    @PreAuthorize("hasAuthority('ATTENDANCE:WIFI_IP_MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<AcademyWifiIpResponse>> register(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RegisterWifiIpRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = clientIpResolver.resolve(servletRequest);
        RegisterWifiIpResult result = registerWifiIpUseCase.register(
                request.toCommand(authUser.userId(), clientIp));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(
                        AttendanceResponseCode.WIFI_IP_REGISTERED,
                        AcademyWifiIpResponse.from(result)));
    }

    @PreAuthorize("hasAuthority('ATTENDANCE:WIFI_IP_MANAGE')")
    @DeleteMapping("/{wifiIpId}")
    public ResponseEntity<GlobalApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long wifiIpId) {
        deleteWifiIpUseCase.delete(authUser.userId(), wifiIpId);

        return ResponseEntity.ok(GlobalApiResponse.ok(
                AttendanceResponseCode.WIFI_IP_DELETED));
    }
}
