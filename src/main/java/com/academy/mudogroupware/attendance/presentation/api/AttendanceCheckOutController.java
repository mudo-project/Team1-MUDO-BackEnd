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
    @PostMapping
    public ResponseEntity<GlobalApiResponse<CheckOutResponse>> checkOut(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CheckOutRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = clientIpResolver.resolve(servletRequest);
        CheckOutResult result = checkOutUseCase.checkOut(
                request.toCommand(
                        authUser.userId(), authUser.academyId(), clientIp));

        return ResponseEntity.ok(GlobalApiResponse.ok(
                AttendanceResponseCode.ATTENDANCE_CHECKED_OUT,
                CheckOutResponse.from(result)));
    }
}
