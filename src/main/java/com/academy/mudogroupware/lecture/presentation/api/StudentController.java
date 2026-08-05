package com.academy.mudogroupware.lecture.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.lecture.application.usecase.RegisterStudentUseCase;
import com.academy.mudogroupware.lecture.presentation.api.common.LectureResponseCode;
import com.academy.mudogroupware.lecture.presentation.api.request.RegisterStudentRequest;
import com.academy.mudogroupware.lecture.presentation.api.response.StudentCreateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "학생 관리", description = "학생 등록 API")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final RegisterStudentUseCase registerStudentUseCase;

    @Operation(summary = "학생 등록", description = "요청자 소속 학원에 학생을 등록한다.")
    @PreAuthorize("hasAuthority('LECTURE:MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<StudentCreateResponse>> registerStudent(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody RegisterStudentRequest request) {
        Long studentId = registerStudentUseCase.registerStudent(request.toCommand(authUser.academyId()));
        StudentCreateResponse data = StudentCreateResponse.from(studentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(LectureResponseCode.STUDENT_REGISTERED, data));
    }
}
