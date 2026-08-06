package com.academy.mudogroupware.calendar.presentation.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import com.academy.mudogroupware.calendar.application.usecase.CreateCalendarEventUseCase;
import com.academy.mudogroupware.calendar.application.usecase.GetCalendarEventsUseCase;
import com.academy.mudogroupware.calendar.domain.exception.InvalidCalendarQueryException;
import com.academy.mudogroupware.calendar.presentation.api.common.CalendarResponseCode;
import com.academy.mudogroupware.calendar.presentation.api.request.CreateCalendarEventRequest;
import com.academy.mudogroupware.calendar.presentation.api.response.CalendarEventResponse;
import com.academy.mudogroupware.calendar.presentation.api.response.CreateCalendarEventResponse;
import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TODO: 권한 모듈의 CALENDAR:CREATE(대표 및 대표가 허용한 권한) 검증이 준비되면 @PreAuthorize를 추가한다.
@Tag(name = "캘린더", description = "학원 공용 캘린더 일정 관리 API")
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class CalendarController {

    private final CreateCalendarEventUseCase createCalendarEventUseCase;
    private final GetCalendarEventsUseCase getCalendarEventsUseCase;

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

    @Operation(summary = "일정 목록/일별 조회",
            description = "한국 시간(Asia/Seoul) 기준으로 date(하루) 또는 yearMonth(한 달) 중 정확히 하나를 지정해 "
                    + "해당 기간에 시작하는 학원 공용 캘린더 일정을 모두 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "일정 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "date와 yearMonth 중 정확히 하나를 지정하지 않은 경우")
    })
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<CalendarEventResponse>>> getEvents(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "일별 조회 대상 날짜(한국 시간 기준)", example = "2026-08-04")
            @RequestParam(required = false) LocalDate date,
            @Parameter(description = "월간 조회 대상 연월(한국 시간 기준)", example = "2026-08")
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
        LocalDateTime from;
        LocalDateTime to;
        if (date != null && yearMonth == null) {
            from = date.atStartOfDay();
            to = date.atTime(LocalTime.MAX);
        } else if (yearMonth != null && date == null) {
            from = yearMonth.atDay(1).atStartOfDay();
            to = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);
        } else {
            throw new InvalidCalendarQueryException();
        }

        List<CalendarEventResponse> responses = getCalendarEventsUseCase
                .getEvents(authUser.academyId(), from, to).stream()
                .map(CalendarEventResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(CalendarResponseCode.EVENT_LIST_RETRIEVED, responses));
    }
}
