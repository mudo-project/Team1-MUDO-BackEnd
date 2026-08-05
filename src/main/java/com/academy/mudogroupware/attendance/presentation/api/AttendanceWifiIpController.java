package com.academy.mudogroupware.attendance.presentation.api;

import java.util.List;

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
import com.academy.mudogroupware.attendance.application.usecase.GetWifiIpsUseCase;
import com.academy.mudogroupware.attendance.application.usecase.RegisterWifiIpUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.request.RegisterWifiIpRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.AcademyWifiIpResponse;
import com.academy.mudogroupware.attendance.presentation.api.response.CurrentClientIpResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    private final GetWifiIpsUseCase getWifiIpsUseCase;
    private final ClientIpResolver clientIpResolver;

    @PreAuthorize("hasAuthority('ATTENDANCE:WIFI_IP_MANAGE')")
    @Operation(
            summary = "등록된 출퇴근 허용 IP 목록 조회",
            description = "원장이 소유한 학원에 등록한 출퇴근 허용 IP 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "등록된 와이파이 IP 목록 조회 성공"),
            @ApiResponse(responseCode = "403", description = "와이파이 IP 조회 권한 없음")
    })
    @GetMapping
    public GlobalApiResponse<List<AcademyWifiIpResponse>> getAll(
            @AuthenticationPrincipal AuthUser authUser) {
        List<AcademyWifiIpResponse> response = getWifiIpsUseCase.getAll(authUser.userId())
                .stream()
                .map(AcademyWifiIpResponse::from)
                .toList();

        return GlobalApiResponse.ok(
                AttendanceResponseCode.WIFI_IP_LIST_RETRIEVED,
                response);
    }

    @Operation(
            summary = "현재 접속 IP 조회",
            description = "요청을 보낸 클라이언트의 공인 IP 주소를 조회합니다.")
    @GetMapping("/current")
    public GlobalApiResponse<CurrentClientIpResponse> getCurrentClientIp(
            HttpServletRequest servletRequest) {
        String clientIp = clientIpResolver.resolve(servletRequest);

        return GlobalApiResponse.ok(
                AttendanceResponseCode.CURRENT_CLIENT_IP_RETRIEVED,
                new CurrentClientIpResponse(clientIp));
    }

    @PreAuthorize("hasAuthority('ATTENDANCE:WIFI_IP_MANAGE')")
    @Operation(
            summary = "출퇴근 허용 IP 등록",
            description = "현재 접속 IP와 사용자가 확인한 IP가 일치하면 소유 학원의 출퇴근 허용 IP로 등록합니다.")
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
    @Operation(
            summary = "출퇴근 허용 IP 삭제",
            description = "소유 학원에 등록된 출퇴근 허용 IP를 삭제합니다.")
    @DeleteMapping("/{wifiIpId}")
    public ResponseEntity<GlobalApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long wifiIpId) {
        deleteWifiIpUseCase.delete(authUser.userId(), wifiIpId);

        return ResponseEntity.ok(GlobalApiResponse.ok(
                AttendanceResponseCode.WIFI_IP_DELETED));
    }
}
