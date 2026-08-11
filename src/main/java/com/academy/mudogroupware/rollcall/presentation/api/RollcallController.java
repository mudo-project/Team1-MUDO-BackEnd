package com.academy.mudogroupware.rollcall.presentation.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.rollcall.application.query.RosterView;
import com.academy.mudogroupware.rollcall.application.usecase.ExportAttendanceSheetUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.GetLectureRosterUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.GetMessageSendCandidatesUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.SaveAttendanceEntriesUseCase;
import com.academy.mudogroupware.rollcall.application.usecase.SendAttendanceMessagesUseCase;
import com.academy.mudogroupware.rollcall.presentation.api.common.RollcallResponseCode;
import com.academy.mudogroupware.rollcall.presentation.api.request.SaveAttendanceEntriesRequest;
import com.academy.mudogroupware.rollcall.presentation.api.request.SendAttendanceMessagesRequest;
import com.academy.mudogroupware.rollcall.presentation.api.response.MessageSendCandidateResponse;
import com.academy.mudogroupware.rollcall.presentation.api.response.MessageSendResultResponse;
import com.academy.mudogroupware.rollcall.presentation.api.response.RosterResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "강의 출결부", description = "강의 출결 조회/저장, 엑셀 다운로드, 문자 발송 대상 조회/발송 API")
@RestController
@RequestMapping("/api/rollcall/lectures/{lectureId}/attendance")
@RequiredArgsConstructor
public class RollcallController {

    private final GetLectureRosterUseCase getLectureRosterUseCase;
    private final SaveAttendanceEntriesUseCase saveAttendanceEntriesUseCase;
    private final ExportAttendanceSheetUseCase exportAttendanceSheetUseCase;
    private final GetMessageSendCandidatesUseCase getMessageSendCandidatesUseCase;
    private final SendAttendanceMessagesUseCase sendAttendanceMessagesUseCase;

    @Operation(summary = "강의 출결부 조회",
            description = "지정한 날짜의 강의 수강생 로스터와 출결 상태, 하단 요약(총원/출석/결석/지각/인강/기타)을 조회한다.")
    @PreAuthorize("hasAuthority('ROLLCALL:MANAGE')")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<RosterResponse>> getRoster(
            @PathVariable Long lectureId,
            @RequestParam LocalDate date) {
        RosterView roster = getLectureRosterUseCase.getRoster(lectureId, date);
        return ResponseEntity.ok(GlobalApiResponse.ok(RollcallResponseCode.ROSTER_RETRIEVED,
                RosterResponse.from(roster)));
    }

    @Operation(summary = "출결 저장", description = "학생별 출결 상태를 일괄 저장한다(upsert). 일부 학생만 체크해도 저장할 수 있다.")
    @PreAuthorize("hasAuthority('ROLLCALL:MANAGE')")
    @PutMapping
    public ResponseEntity<Void> saveEntries(
            @PathVariable Long lectureId,
            @RequestParam LocalDate date,
            @Valid @RequestBody SaveAttendanceEntriesRequest request) {
        saveAttendanceEntriesUseCase.saveEntries(request.toCommand(lectureId, date));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "출결부 엑셀 다운로드",
            description = "번호/학생명/학년/출결상태/비고와 하단 요약이 포함된 .xlsx 파일을 다운로드한다. "
                    + "출결 상태가 비어 있어도 다운로드할 수 있다.")
    @PreAuthorize("hasAuthority('ROLLCALL:MANAGE')")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportSheet(
            @PathVariable Long lectureId,
            @RequestParam LocalDate date) {
        byte[] sheet = exportAttendanceSheetUseCase.exportSheet(lectureId, date);
        String filename = "attendance_" + lectureId + "_" + date + ".xlsx";
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename, StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(sheet);
    }

    @Operation(summary = "문자 발송 대상 조회",
            description = "학생별 출결 상태에 매칭되는 문자 템플릿을 조회한다. 매칭되는 템플릿이 없으면 발송 불가로 표시된다. "
                    + "실제 문자 발송은 이번 범위가 아니다.")
    @PreAuthorize("hasAuthority('ROLLCALL:MANAGE')")
    @GetMapping("/message-candidates")
    public ResponseEntity<GlobalApiResponse<List<MessageSendCandidateResponse>>> getMessageCandidates(
            @PathVariable Long lectureId,
            @RequestParam LocalDate date) {
        List<MessageSendCandidateResponse> candidates = getMessageSendCandidatesUseCase
                .getCandidates(lectureId, date).stream()
                .map(MessageSendCandidateResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(RollcallResponseCode.MESSAGE_CANDIDATES_RETRIEVED,
                candidates));
    }

    @Operation(summary = "출결 안내 문자 발송",
            description = "선택한 학생들에게 출결 상태에 매칭되는 문자 템플릿으로 SMS를 발송한다. "
                    + "학생별 발송 성공/실패 결과를 반환한다(일부만 실패해도 전체가 실패로 처리되지 않는다).")
    @PreAuthorize("hasAuthority('ROLLCALL:MANAGE')")
    @PostMapping("/message-candidates/send")
    public ResponseEntity<GlobalApiResponse<List<MessageSendResultResponse>>> sendMessages(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long lectureId,
            @RequestParam LocalDate date,
            @Valid @RequestBody SendAttendanceMessagesRequest request) {
        List<MessageSendResultResponse> results = sendAttendanceMessagesUseCase
                .send(request.toCommand(lectureId, authUser.academyId(), date)).stream()
                .map(MessageSendResultResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(RollcallResponseCode.MESSAGES_SENT, results));
    }
}
