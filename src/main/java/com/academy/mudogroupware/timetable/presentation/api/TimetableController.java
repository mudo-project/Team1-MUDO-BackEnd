package com.academy.mudogroupware.timetable.presentation.api;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.presentation.api.common.TimetableResponseCode;
import com.academy.mudogroupware.timetable.presentation.api.request.CreateTimetableSetRequest;
import com.academy.mudogroupware.timetable.presentation.api.response.CreateTimetableSetResponse;

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
}
