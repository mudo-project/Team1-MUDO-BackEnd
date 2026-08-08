package com.academy.mudogroupware.timetable.presentation.api;

import java.util.List;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSlotsUseCase;
import com.academy.mudogroupware.timetable.presentation.api.common.TimetableResponseCode;
import com.academy.mudogroupware.timetable.presentation.api.request.CreateTimetableSlotRequest;
import com.academy.mudogroupware.timetable.presentation.api.response.CreateTimetableSlotResponse;
import com.academy.mudogroupware.timetable.presentation.api.response.TimetableSlotResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시간표", description = "학원 시간표 세트/수업 슬롯 관리 API")
@RestController
@RequestMapping("/api/timetables/{timetableSetId}/slots")
@RequiredArgsConstructor
public class TimetableSlotController {

    private final CreateTimetableSlotUseCase createTimetableSlotUseCase;
    private final GetTimetableSlotsUseCase getTimetableSlotsUseCase;
    private final GetTimetableSlotUseCase getTimetableSlotUseCase;

    @Operation(summary = "수업 슬롯 등록", description = "시간표 세트 안에 수업(요일/시간/강의실/강사/과목)을 등록합니다. 같은 강의실에 겹치는 시간대가 있으면 거절합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "등록 성공"),
        @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "TIMETABLE:MANAGE 권한이 없는 경우"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않거나 다른 학원 소속인 경우"),
        @ApiResponse(responseCode = "409", description = "같은 강의실에 겹치는 시간대의 수업이 이미 있는 경우")
    })
    @PreAuthorize("hasAuthority('TIMETABLE:MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<CreateTimetableSlotResponse>> createSlot(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long timetableSetId,
            @Valid @RequestBody CreateTimetableSlotRequest request) {
        Long slotId = createTimetableSlotUseCase.createSlot(request.toCommand(authUser.academyId(), timetableSetId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(TimetableResponseCode.SLOT_CREATED, CreateTimetableSlotResponse.from(slotId)));
    }

    @Operation(summary = "수업 슬롯 목록 조회", description = "시간표 세트 안의 모든 수업 슬롯을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않거나 다른 학원 소속인 경우")
    })
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<TimetableSlotResponse>>> getSlots(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long timetableSetId) {
        List<TimetableSlotResponse> responses = getTimetableSlotsUseCase
                .getSlots(authUser.academyId(), timetableSetId).stream()
                .map(TimetableSlotResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(TimetableResponseCode.SLOT_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "수업 슬롯 상세 조회", description = "수업 슬롯 번호로 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않거나 다른 학원 소속이거나, 수업 슬롯이 존재하지 않거나 다른 세트 소속인 경우")
    })
    @GetMapping("/{timetableSlotId}")
    public ResponseEntity<GlobalApiResponse<TimetableSlotResponse>> getSlot(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long timetableSetId,
            @PathVariable Long timetableSlotId) {
        TimetableSlotResponse response = TimetableSlotResponse.from(
                getTimetableSlotUseCase.getSlot(authUser.academyId(), timetableSetId, timetableSlotId));
        return ResponseEntity.ok(GlobalApiResponse.ok(TimetableResponseCode.SLOT_DETAIL_RETRIEVED, response));
    }
}
