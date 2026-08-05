package com.academy.mudogroupware.lecture.presentation.api;

import java.time.DayOfWeek;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.lecture.application.query.LectureDetailView;
import com.academy.mudogroupware.lecture.application.usecase.CreateLectureUseCase;
import com.academy.mudogroupware.lecture.application.usecase.LectureQueryUseCase;
import com.academy.mudogroupware.lecture.domain.model.Grade;
import com.academy.mudogroupware.lecture.domain.repository.LectureFilter;
import com.academy.mudogroupware.lecture.presentation.api.common.LectureResponseCode;
import com.academy.mudogroupware.lecture.presentation.api.request.CreateLectureRequest;
import com.academy.mudogroupware.lecture.presentation.api.response.LectureCreateResponse;
import com.academy.mudogroupware.lecture.presentation.api.response.LectureDetailResponse;
import com.academy.mudogroupware.lecture.presentation.api.response.LectureSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "강의 관리", description = "강의 등록/조회 API. 수강 등록은 student 모듈(/api/students/{studentId}/enrollments)이 담당한다.")
@RestController
@RequestMapping("/api/lectures")
@RequiredArgsConstructor
public class LectureController {

    private final CreateLectureUseCase createLectureUseCase;
    private final LectureQueryUseCase lectureQueryUseCase;

    @Operation(summary = "강의 등록",
            description = "학년/시즌/과목/교실/담당 선생님/요일·시간대를 지정해 강의를 등록한다. "
                    + "시즌·과목·교실은 이름이 없으면 자동 생성된다. 같은 교실·요일·시간대가 이미 있으면 409.")
    @PreAuthorize("hasAuthority('LECTURE:MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<LectureCreateResponse>> createLecture(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateLectureRequest request) {
        Long lectureId = createLectureUseCase.createLecture(request.toCommand(authUser.academyId(),
                authUser.userId()));
        LectureCreateResponse data = LectureCreateResponse.from(lectureId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(LectureResponseCode.LECTURE_CREATED, data));
    }

    @Operation(summary = "강의 목록 조회",
            description = "시즌/학년/과목/선생님/교실/요일 필터와 페이지 단위로 요청자 소속 학원의 강의 목록을 조회한다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<SliceResponse<LectureSummaryResponse>>> getLectures(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) Grade grade,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) DayOfWeek dayOfWeek,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        LectureFilter filter = new LectureFilter(termId, grade, subjectId, teacherId, classroomId, dayOfWeek);
        SliceResponse<LectureSummaryResponse> data = SliceResponse.from(
                lectureQueryUseCase.getLectures(authUser.academyId(), filter, page, size),
                LectureSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(LectureResponseCode.LECTURE_LIST_RETRIEVED, data));
    }

    @Operation(summary = "강의 상세 조회", description = "강의 기본 정보와 수강생 목록을 조회한다. 다른 학원의 강의를 조회하면 403.")
    @GetMapping("/{lectureId}")
    public ResponseEntity<GlobalApiResponse<LectureDetailResponse>> getLectureDetail(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long lectureId) {
        LectureDetailView view = lectureQueryUseCase.getLectureDetail(lectureId, authUser.academyId());
        return ResponseEntity.ok(GlobalApiResponse.ok(LectureResponseCode.LECTURE_DETAIL_RETRIEVED,
                LectureDetailResponse.from(view)));
    }
}
