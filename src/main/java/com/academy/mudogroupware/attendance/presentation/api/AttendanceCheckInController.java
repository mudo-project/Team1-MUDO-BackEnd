package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.result.CheckInResult;
import com.academy.mudogroupware.attendance.application.usecase.CheckInUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.request.CheckInRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.CheckInResponse;
import com.academy.mudogroupware.global.infrastructure.web.ClientIpResolver;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태", description = "근태 관리 API")
@RestController
@RequestMapping("/api/attendance/check-ins")
@RequiredArgsConstructor
public class AttendanceCheckInController {

    private final CheckInUseCase checkInUseCase;
    private final ClientIpResolver clientIpResolver;

    @PreAuthorize("hasAuthority('ATTENDANCE:CHECK_IN')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<CheckInResponse>> checkIn(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CheckInRequest request,
            HttpServletRequest servletRequest) {
        String clientIp = clientIpResolver.resolve(servletRequest);
        CheckInResult result = checkInUseCase.checkIn(
                request.toCommand(authUser.userId(), authUser.academyId(), clientIp));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(
                        AttendanceResponseCode.ATTENDANCE_CHECKED_IN,
                        CheckInResponse.from(result)));
    }
}
