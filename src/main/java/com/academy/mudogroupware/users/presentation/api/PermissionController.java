package com.academy.mudogroupware.users.presentation.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.users.application.usecase.PermissionQueryUseCase;
import com.academy.mudogroupware.users.presentation.api.common.PermissionResponseCode;
import com.academy.mudogroupware.users.presentation.api.response.PermissionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "역할·권한", description = "학원별 역할 생성/조회/수정/삭제 및 권한 조립 API")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionQueryUseCase permissionQueryUseCase;

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @Operation(
            summary = "권한 카탈로그 조회",
            description = "시스템 전체 고정 권한 카탈로그를 조회합니다. 학원별로 다르지 않습니다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<PermissionResponse>>> getPermissions() {
        List<PermissionResponse> data = permissionQueryUseCase.getPermissions().stream()
                .map(PermissionResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(PermissionResponseCode.PERMISSION_LIST_RETRIEVED, data));
    }
}
