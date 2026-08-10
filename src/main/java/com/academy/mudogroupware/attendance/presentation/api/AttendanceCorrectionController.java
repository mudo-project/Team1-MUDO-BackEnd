package com.academy.mudogroupware.attendance.presentation.api;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.academy.mudogroupware.attendance.application.usecase.*;
import com.academy.mudogroupware.attendance.presentation.api.common.AttendanceResponseCode;
import com.academy.mudogroupware.attendance.presentation.api.request.CreateAttendanceCorrectionRequest;
import com.academy.mudogroupware.attendance.presentation.api.response.*;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@Tag(name = "근태", description = "근태 관리 API")
@RestController @RequestMapping("/api/attendance/me") @RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AttendanceCorrectionController {
    private final CreateAttendanceCorrectionUseCase createUseCase;
    private final GetMyAttendanceCorrectionUseCase queryUseCase;
    private final GetMyAttendanceDayUseCase dayUseCase;
    @Operation(summary = "특정 날짜 내 근태 상세 조회")
    @GetMapping("/days/{date}")
    public GlobalApiResponse<MyAttendanceDayResponse> getDay(@AuthenticationPrincipal AuthUser user, @PathVariable LocalDate date) {
        return GlobalApiResponse.ok(AttendanceResponseCode.MY_ATTENDANCE_DAY_RETRIEVED,
                MyAttendanceDayResponse.from(dayUseCase.get(user.userId(), date)));
    }
    @Operation(summary = "근태 수정 요청 등록")
    @PostMapping("/correction-requests")
    public GlobalApiResponse<AttendanceCorrectionResponse> create(@AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CreateAttendanceCorrectionRequest request) {
        return GlobalApiResponse.created(AttendanceResponseCode.CORRECTION_REQUEST_CREATED,
                AttendanceCorrectionResponse.from(createUseCase.create(request.toCommand(user.userId()))));
    }
    @Operation(summary = "내 근태 수정 요청 목록 조회")
    @GetMapping("/correction-requests")
    public GlobalApiResponse<List<AttendanceCorrectionResponse>> getAll(@AuthenticationPrincipal AuthUser user) {
        return GlobalApiResponse.ok(AttendanceResponseCode.MY_CORRECTION_REQUESTS_RETRIEVED,
                queryUseCase.getAll(user.userId()).stream().map(AttendanceCorrectionResponse::from).toList());
    }
    @Operation(summary = "내 근태 수정 요청 상세 조회")
    @GetMapping("/correction-requests/{requestId}")
    public GlobalApiResponse<AttendanceCorrectionResponse> get(@AuthenticationPrincipal AuthUser user, @PathVariable Long requestId) {
        return GlobalApiResponse.ok(AttendanceResponseCode.MY_CORRECTION_REQUEST_RETRIEVED,
                AttendanceCorrectionResponse.from(queryUseCase.get(requestId, user.userId())));
    }
}
