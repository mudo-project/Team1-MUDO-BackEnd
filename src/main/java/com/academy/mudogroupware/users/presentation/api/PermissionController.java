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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionQueryUseCase permissionQueryUseCase;

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<PermissionResponse>>> getPermissions() {
        List<PermissionResponse> data = permissionQueryUseCase.getPermissions().stream()
                .map(PermissionResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(PermissionResponseCode.PERMISSION_LIST_RETRIEVED, data));
    }
}
