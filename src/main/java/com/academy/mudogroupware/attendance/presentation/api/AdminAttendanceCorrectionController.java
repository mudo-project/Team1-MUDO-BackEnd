package com.academy.mudogroupware.attendance.presentation.api;

import com.academy.mudogroupware.attendance.application.usecase.ManageAttendanceCorrectionUseCase;
import com.academy.mudogroupware.attendance.domain.model.AttendanceCorrectionStatus;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.request.RejectAttendanceCorrectionRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.AdminAttendanceCorrectionResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "근태", description = "관리자 근태 수정 요청 API")
@RestController
@RequestMapping("/api/attendance/correction-requests")
@RequiredArgsConstructor
@Validated
public class AdminAttendanceCorrectionController {
    private final ManageAttendanceCorrectionUseCase useCase;

    @Operation(summary = "근태 수정 요청 목록 조회")
    @GetMapping
    @PreAuthorize("hasAuthority('ATTENDANCE:CORRECTION_READ')")
    public GlobalApiResponse<SliceResponse<AdminAttendanceCorrectionResponse>> getAll(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) AttendanceCorrectionStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "30") @Min(1) @Max(100) int size) {
        return GlobalApiResponse.ok(AttendanceResponseCode.ATTENDANCE_CORRECTION_LIST_RETRIEVED,
                SliceResponse.from(useCase.getAll(user.academyId(), status, page, size),
                        AdminAttendanceCorrectionResponse::from));
    }

    @Operation(summary = "근태 수정 요청 상세 조회")
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAuthority('ATTENDANCE:CORRECTION_READ')")
    public GlobalApiResponse<AdminAttendanceCorrectionResponse> get(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long requestId) {
        return GlobalApiResponse.ok(AttendanceResponseCode.ATTENDANCE_CORRECTION_RETRIEVED,
                AdminAttendanceCorrectionResponse.from(useCase.get(user.academyId(), requestId)));
    }

    @Operation(summary = "근태 수정 요청 승인")
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasAuthority('ATTENDANCE:CORRECTION_PROCESS')")
    public GlobalApiResponse<Void> approve(
            @AuthenticationPrincipal AuthUser user, @PathVariable Long requestId) {
        useCase.approve(user.academyId(), requestId, user.userId());
        return GlobalApiResponse.ok(AttendanceResponseCode.ATTENDANCE_CORRECTION_APPROVED);
    }

    @Operation(summary = "근태 수정 요청 반려")
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasAuthority('ATTENDANCE:CORRECTION_PROCESS')")
    public GlobalApiResponse<Void> reject(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long requestId,
            @Valid @RequestBody RejectAttendanceCorrectionRequest request) {
        useCase.reject(user.academyId(), requestId, user.userId(), request.reason());
        return GlobalApiResponse.ok(AttendanceResponseCode.ATTENDANCE_CORRECTION_REJECTED);
    }
}
