package com.academy.mudogroupware.calendar.presentation.api;

import com.academy.mudogroupware.calendar.application.usecase.CreateCalendarEventUseCase;
import com.academy.mudogroupware.calendar.presentation.api.common.CalendarResponseCode;
import com.academy.mudogroupware.calendar.presentation.api.request.CreateCalendarEventRequest;
import com.academy.mudogroupware.calendar.presentation.api.response.CreateCalendarEventResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TODO: 권한 모듈의 CALENDAR:CREATE(대표 및 대표가 허용한 권한) 검증이 준비되면 @PreAuthorize를 추가한다.
@Tag(name = "캘린더", description = "학원 공용 캘린더 일정 관리 API")
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CreateCalendarEventUseCase createCalendarEventUseCase;

    @Operation(summary = "일정 생성", description = "학원 공용 캘린더에 새 일정을 추가합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "일정 생성 성공"),
        @ApiResponse(responseCode = "400", description = "요청값이 유효하지 않음(제목 누락, 종료 시각이 시작 시각보다 이전 등)")
    })
    @PostMapping
    public ResponseEntity<GlobalApiResponse<CreateCalendarEventResponse>> createEvent(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateCalendarEventRequest request) {
        Long eventId = createCalendarEventUseCase.createEvent(request.toCommand(authUser));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(
                        CalendarResponseCode.EVENT_CREATED,
                        CreateCalendarEventResponse.from(eventId)));
    }
}
