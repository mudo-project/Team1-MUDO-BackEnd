package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.result.CheckOutResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckOutUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.request.CheckOutRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.CheckOutResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendance/check-outs")
@RequiredArgsConstructor
public class AttendanceCheckOutController {

    private final CheckOutUseCase checkOutUseCase;
    private final ClientIpResolver clientIpResolver;

    @PreAuthorize("hasAuthority('ATTENDANCE:CHECK_OUT')")
    @Operation(
            summary = "퇴근 체크아웃",
            description = "소속 학원에 등록된 허용 IP에서 퇴근을 기록합니다. 초과근무 퇴근은 사유가 필요합니다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<CheckOutResponse>> checkOut(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CheckOutRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = clientIpResolver.resolve(servletRequest);
        CheckOutResult result = checkOutUseCase.checkOut(
                request.toCommand(
                        authUser.userId(), clientIp));

        return ResponseEntity.ok(GlobalApiResponse.ok(
                AttendanceResponseCode.ATTENDANCE_CHECKED_OUT,
                CheckOutResponse.from(result)));
    }
}
