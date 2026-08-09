package com.academy.mudogroupware.timetable.presentation.api;

import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.timetable.application.command.DeleteTimetableSetCommand;
import com.academy.mudogroupware.timetable.application.command.ExportTimetableCommand;
import com.academy.mudogroupware.timetable.application.usecase.CreateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.DeleteTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.ExportTimetableUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetUseCase;
import com.academy.mudogroupware.timetable.application.usecase.GetTimetableSetsUseCase;
import com.academy.mudogroupware.timetable.application.usecase.UpdateTimetableSetUseCase;
import com.academy.mudogroupware.timetable.domain.exception.InvalidExportColorException;
import com.academy.mudogroupware.timetable.domain.model.ClassType;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportColorCriterion;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportDensity;
import com.academy.mudogroupware.timetable.domain.model.TimetableExportFormat;
import com.academy.mudogroupware.timetable.presentation.api.common.TimetableResponseCode;
import com.academy.mudogroupware.timetable.presentation.api.request.CreateTimetableSetRequest;
import com.academy.mudogroupware.timetable.presentation.api.request.UpdateTimetableSetRequest;
import com.academy.mudogroupware.timetable.presentation.api.response.CreateTimetableSetResponse;
import com.academy.mudogroupware.timetable.presentation.api.response.TimetableSetDetailResponse;
import com.academy.mudogroupware.timetable.presentation.api.response.TimetableSetSummaryResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final DeleteTimetableSetUseCase deleteTimetableSetUseCase;
    private final ExportTimetableUseCase exportTimetableUseCase;
    private final ObjectMapper objectMapper;

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

    @Operation(summary = "시간표 세트 삭제", description = "시간표 세트와 그 안의 모든 수업 슬롯을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "삭제 성공"),
        @ApiResponse(responseCode = "403", description = "TIMETABLE:MANAGE 권한이 없는 경우"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않거나 다른 학원 소속인 경우")
    })
    @PreAuthorize("hasAuthority('TIMETABLE:MANAGE')")
    @DeleteMapping("/{timetableSetId}")
    public ResponseEntity<Void> deleteTimetableSet(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long timetableSetId) {
        deleteTimetableSetUseCase.deleteTimetableSet(new DeleteTimetableSetCommand(authUser.academyId(), timetableSetId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "시간표 세트 내보내기",
            description = "시간표 세트의 수업 목록을 엑셀/PDF/PNG 파일로 내보냅니다. 엑셀·PNG는 요일/강의실 층/수업종류 필터를 반영하며, "
                    + "PDF는 인쇄용으로 필터와 무관하게 항상 세트 전체를 내보냅니다. 배경색은 강의실/강사/학년 중 선택한 기준별로 지정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "내보내기 성공"),
        @ApiResponse(responseCode = "400", description = "색상 값이 6자리 16진수 형식이 아니거나 colorMap이 올바른 JSON이 아닌 경우"),
        @ApiResponse(responseCode = "404", description = "시간표 세트가 존재하지 않거나 다른 학원 소속인 경우")
    })
    @GetMapping("/{timetableSetId}/export")
    public ResponseEntity<byte[]> exportTimetable(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long timetableSetId,
            @RequestParam TimetableExportFormat format,
            @RequestParam TimetableExportColorCriterion colorCriterion,
            @RequestParam String colorMap,
            @RequestParam(defaultValue = "NORMAL") TimetableExportDensity density,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(required = false) String floor,
            @RequestParam(required = false) ClassType classType) {
        Map<String, String> colors = parseColorMap(colorMap);
        byte[] file = exportTimetableUseCase.export(new ExportTimetableCommand(
                authUser.academyId(), timetableSetId, format, colorCriterion, colors, density,
                dayOfWeek, floor, classType));

        String filename = "timetable_" + timetableSetId + "." + extension(format);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mediaType(format)))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(file);
    }

    private Map<String, String> parseColorMap(String colorMap) {
        Map<String, String> parsed;
        try {
            parsed = objectMapper.readValue(colorMap, new TypeReference<Map<String, String>>() {
            });
        } catch (JsonProcessingException e) {
            throw new InvalidExportColorException(e);
        }
        if (parsed == null || parsed.containsValue(null)) {
            throw new InvalidExportColorException();
        }
        return parsed;
    }

    private String extension(TimetableExportFormat format) {
        return switch (format) {
            case EXCEL -> "xlsx";
            case PDF -> "pdf";
            case PNG -> "png";
        };
    }

    private String mediaType(TimetableExportFormat format) {
        return switch (format) {
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case PDF -> "application/pdf";
            case PNG -> "image/png";
        };
    }
}
