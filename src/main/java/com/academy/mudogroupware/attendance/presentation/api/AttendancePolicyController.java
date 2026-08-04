package com.academy.mudogroupware.attendance.presentation.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.attendance.application.result.SaveAttendancePolicyResult;
import com.academy.mudogroupware.attendance.application.usecase.SaveAttendancePolicyUseCase;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.request.SaveAttendancePolicyRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.AttendancePolicyResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "근태 정책", description = "학원 근무시간 정책 API")
@RestController
@RequestMapping("/api/attendance/policies")
@RequiredArgsConstructor
public class AttendancePolicyController {

    private final SaveAttendancePolicyUseCase saveAttendancePolicyUseCase;

    @Operation(
            summary = "근무시간 정책 저장",
            description = "원장 소유 학원의 기본 근무시간과 요일별 예외 설정을 저장합니다.")
    @PutMapping
    public GlobalApiResponse<AttendancePolicyResponse> save(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody SaveAttendancePolicyRequest request) {
        SaveAttendancePolicyResult result = saveAttendancePolicyUseCase.save(
                request.toCommand(authUser.userId()));
        return GlobalApiResponse.ok(
                AttendanceResponseCode.ATTENDANCE_POLICY_SAVED,
                AttendancePolicyResponse.from(result));
    }
}
