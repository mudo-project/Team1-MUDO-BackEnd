package com.academy.mudogroupware.student.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.api.common.SliceResponse;
import com.academy.mudogroupware.student.application.command.DeleteStudentCommand;
import com.academy.mudogroupware.student.application.command.EndEnrollmentCommand;
import com.academy.mudogroupware.student.application.usecase.CreateStudentUseCase;
import com.academy.mudogroupware.student.application.usecase.DeleteStudentUseCase;
import com.academy.mudogroupware.student.application.usecase.EndEnrollmentUseCase;
import com.academy.mudogroupware.student.application.usecase.EnrollStudentUseCase;
import com.academy.mudogroupware.student.application.usecase.StudentQueryUseCase;
import com.academy.mudogroupware.student.application.usecase.UpdateStudentUseCase;
import com.academy.mudogroupware.student.presentation.api.common.StudentResponseCode;
import com.academy.mudogroupware.student.presentation.api.request.CreateStudentRequest;
import com.academy.mudogroupware.student.presentation.api.request.EnrollStudentRequest;
import com.academy.mudogroupware.student.presentation.api.request.UpdateStudentRequest;
import com.academy.mudogroupware.student.presentation.api.response.EnrollmentCreateResponse;
import com.academy.mudogroupware.student.presentation.api.response.StudentCreateResponse;
import com.academy.mudogroupware.student.presentation.api.response.StudentDetailResponse;
import com.academy.mudogroupware.student.presentation.api.response.StudentSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "학생", description = "학생 등록/목록/상세/수강 등록 API")
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
@Validated
public class StudentController {

    private final CreateStudentUseCase createStudentUseCase;
    private final UpdateStudentUseCase updateStudentUseCase;
    private final DeleteStudentUseCase deleteStudentUseCase;
    private final StudentQueryUseCase studentQueryUseCase;
    private final EnrollStudentUseCase enrollStudentUseCase;
    private final EndEnrollmentUseCase endEnrollmentUseCase;

    @Operation(summary = "학생 등록", description = "학생 기본 정보를 등록한다. 학생은 로그인 계정이 아니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "학생 등록 성공")
    })
    @PreAuthorize("hasAuthority('STUDENT:MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<StudentCreateResponse>> createStudent(
            @Valid @RequestBody CreateStudentRequest request) {
        Long studentId = createStudentUseCase.createStudent(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(StudentResponseCode.STUDENT_CREATED,
                        StudentCreateResponse.from(studentId)));
    }

    @Operation(summary = "학생 목록 조회", description = "학생을 가나다순으로 조회하고 이름 검색을 지원한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "학생 목록 조회 성공")
    })
    @PreAuthorize("hasAuthority('STUDENT:MANAGE')")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<SliceResponse<StudentSummaryResponse>>> getStudents(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        SliceResponse<StudentSummaryResponse> response = SliceResponse.from(
                studentQueryUseCase.getStudents(keyword, page, size),
                StudentSummaryResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(StudentResponseCode.STUDENT_LIST_RETRIEVED, response));
    }

    @Operation(summary = "학생 상세 조회", description = "학생 기본 정보와 현재 수강 중인 강의 목록을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "학생 상세 조회 성공")
    })
    @PreAuthorize("hasAuthority('STUDENT:MANAGE')")
    @GetMapping("/{studentId}")
    public ResponseEntity<GlobalApiResponse<StudentDetailResponse>> getStudentDetail(
            @PathVariable Long studentId) {
        StudentDetailResponse response = StudentDetailResponse.from(
                studentQueryUseCase.getStudentDetail(studentId));
        return ResponseEntity.ok(GlobalApiResponse.ok(StudentResponseCode.STUDENT_DETAIL_RETRIEVED, response));
    }

    @Operation(summary = "학생 정보 수정", description = "학생 기본 정보를 수정한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "학생 정보 수정 성공")
    })
    @PreAuthorize("hasAuthority('STUDENT:MANAGE')")
    @PatchMapping("/{studentId}")
    public ResponseEntity<Void> updateStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody UpdateStudentRequest request) {
        updateStudentUseCase.updateStudent(request.toCommand(studentId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "학생 삭제", description = "학생을 소프트 삭제한다. 삭제된 학생은 목록/상세 조회에 노출되지 않는다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "학생 삭제 성공")
    })
    @PreAuthorize("hasAuthority('STUDENT:MANAGE')")
    @DeleteMapping("/{studentId}")
    public ResponseEntity<Void> deleteStudent(
            @PathVariable Long studentId) {
        deleteStudentUseCase.deleteStudent(new DeleteStudentCommand(studentId));
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "학생 수강 등록", description = "\"결제하기\" 버튼 흐름에서 실제 결제 없이 학생을 강의에 등록한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "학생 수강 등록 성공")
    })
    @PreAuthorize("hasAuthority('STUDENT:MANAGE')")
    @PostMapping("/{studentId}/enrollments")
    public ResponseEntity<GlobalApiResponse<EnrollmentCreateResponse>> enroll(
            @PathVariable Long studentId,
            @Valid @RequestBody EnrollStudentRequest request) {
        Long enrollmentId = enrollStudentUseCase.enroll(request.toCommand(studentId));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(StudentResponseCode.ENROLLMENT_CREATED,
                        EnrollmentCreateResponse.from(enrollmentId)));
    }

    @Operation(summary = "학생 수강 종료", description = "현재 수강 중인 강의를 종료 상태로 변경한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "학생 수강 종료 성공")
    })
    @PreAuthorize("hasAuthority('STUDENT:MANAGE')")
    @PatchMapping("/{studentId}/enrollments/{enrollmentId}/end")
    public ResponseEntity<Void> endEnrollment(
            @PathVariable Long studentId,
            @PathVariable Long enrollmentId) {
        endEnrollmentUseCase.end(new EndEnrollmentCommand(studentId, enrollmentId));
        return ResponseEntity.noContent().build();
    }
}
