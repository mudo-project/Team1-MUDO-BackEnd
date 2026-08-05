package com.academy.mudogroupware.users.presentation.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.usecase.AssignRolePermissionsUseCase;
import com.academy.mudogroupware.users.application.usecase.CreateRoleUseCase;
import com.academy.mudogroupware.users.presentation.api.common.RoleResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.AssignRolePermissionsRequest;
import com.academy.mudogroupware.users.presentation.api.request.CreateRoleRequest;
import com.academy.mudogroupware.users.presentation.api.response.RoleCreateResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final CreateRoleUseCase createRoleUseCase;
    private final AssignRolePermissionsUseCase assignRolePermissionsUseCase;

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<RoleCreateResponse>> createRole(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateRoleRequest request) {
        Long roleId = createRoleUseCase.createRole(request.toCommand(authUser.academyId()));
        RoleCreateResponse data = RoleCreateResponse.from(roleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(RoleResponseCode.ROLE_CREATED, data));
    }

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<Void> assignPermissions(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long roleId,
            @Valid @RequestBody AssignRolePermissionsRequest request) {
        assignRolePermissionsUseCase.assignPermissions(request.toCommand(roleId, authUser.academyId()));
        return ResponseEntity.noContent().build();
    }
}
