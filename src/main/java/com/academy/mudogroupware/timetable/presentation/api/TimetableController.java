package com.academy.mudogroupware.timetable.presentation.api;

import java.util.List;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetsUseCase;
import com.academy.mudogroupware.timetable.application.usecase.UpdateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.presentation.api.common.TimetableResponseCode;
import com.academy.mudogroupware.timetable.presentation.api.request.CreateTimetableSetRequest;
import com.academy.mudogroupware.timetable.presentation.api.request.UpdateTimetableSetRequest;
import com.academy.mudogroupware.timetable.presentation.api.response.CreateTimetableSetResponse;
import com.academy.mudogroupware.timetable.presentation.api.response.TimetableSetDetailResponse;
import com.academy.mudogroupware.timetable.presentation.api.response.TimetableSetSummaryResponse;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "시간표", description = "학원 시간표 세트/수업 슬롯 관리 API")
@RestController
@RequestMapping("/api/timetables")
@RequiredArgsConstructor
public class TimetableController {

    private final CreateTimetableSetUseCase createTimetableSetUseCase;
    private final GetTimetableSetsUseCase getTimetableSetsUseCase;
    private final GetTimetableSetUseCase getTimetableSetUseCase;
    private final UpdateTimetableSetUseCase updateTimetableSetUseCase;

    @Operation(summary = "시간표 세트 생성", description = "기간·운영시간·요일·슬롯단위·강의실 구성을 지정해 새 시간표 세트를 만듭니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "시간표 세트 생성 성공"),
        @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않음(이름 누락, 종료일이 시작일보다 이전, 강의실 코드 중복 등)"),
        @ApiResponse(responseCode = "403", description = "TIMETABLE:MANAGE 권한이 없는 경우")
    })
    @PreAuthorize("hasAuthority('TIMETABLE:MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<CreateTimetableSetResponse>> createTimetableSet(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateTimetableSetRequest request) {
        Long timetableSetId = createTimetableSetUseCase.createTimetableSet(request.toCommand(authUser.academyId()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(
                        TimetableResponseCode.SET_CREATED, CreateTimetableSetResponse.from(timetableSetId)));
    }

    @Operation(summary = "시간표 세트 목록 조회", description = "학원의 모든 시간표 세트를 시작일 최신순으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<TimetableSetSummaryResponse>>> getTimetableSets(
            @AuthenticationPrincipal AuthUser authUser) {
        List<TimetableSetSummaryResponse> responses = getTimetableSetsUseCase
                .getTimetableSets(authUser.academyId()).stream()
                .map(TimetableSetSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(TimetableResponseCode.SET_LIST_RETRIEVED, responses));
    }

    @Operation(summary = "시간표 세트 상세 조회", description = "시간표 세트 번호로 상세 정보(강의실 구성 포함)를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않거나 다른 학원 소속인 경우")
    })
    @GetMapping("/{timetableSetId}")
    public ResponseEntity<GlobalApiResponse<TimetableSetDetailResponse>> getTimetableSet(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long timetableSetId) {
        TimetableSetDetailResponse response = TimetableSetDetailResponse.from(
                getTimetableSetUseCase.getTimetableSet(authUser.academyId(), timetableSetId));
        return ResponseEntity.ok(GlobalApiResponse.ok(TimetableResponseCode.SET_DETAIL_RETRIEVED, response));
    }

    @Operation(summary = "시간표 세트 수정", description = "시간표 세트의 기간/운영시간/요일/슬롯단위/강의실 구성을 전체 교체합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "수정 성공"),
        @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "TIMETABLE:MANAGE 권한이 없는 경우"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않거나 다른 학원 소속인 경우")
    })
    @PreAuthorize("hasAuthority('TIMETABLE:MANAGE')")
    @PatchMapping("/{timetableSetId}")
    public ResponseEntity<Void> updateTimetableSet(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long timetableSetId,
            @Valid @RequestBody UpdateTimetableSetRequest request) {
        updateTimetableSetUseCase.updateTimetableSet(request.toCommand(authUser.academyId(), timetableSetId));
        return ResponseEntity.noContent().build();
    }
}
