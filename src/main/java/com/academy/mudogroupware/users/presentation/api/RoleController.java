package com.academy.mudogroupware.users.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.users.application.command.DeleteRoleCommand;
import com.academy.mudogroupware.users.application.usecase.AssignRolePermissionsUseCase;
import com.academy.mudogroupware.users.application.usecase.CreateRoleUseCase;
import com.academy.mudogroupware.users.application.usecase.DeleteRoleUseCase;
import com.academy.mudogroupware.users.application.usecase.GetRoleUseCase;
import com.academy.mudogroupware.users.application.usecase.ListRolesUseCase;
import com.academy.mudogroupware.users.application.usecase.UpdateRoleUseCase;
import com.academy.mudogroupware.users.presentation.api.common.RoleResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.AssignRolePermissionsRequest;
import com.academy.mudogroupware.users.presentation.api.request.CreateRoleRequest;
import com.academy.mudogroupware.users.presentation.api.request.UpdateRoleRequest;
import com.academy.mudogroupware.users.presentation.api.response.RoleCreateResponse;
import com.academy.mudogroupware.users.presentation.api.response.RoleDetailResponse;
import com.academy.mudogroupware.users.presentation.api.response.RoleListResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "역할·권한", description = "학원별 역할 생성/조회/수정/삭제 및 권한 조립 API")
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final CreateRoleUseCase createRoleUseCase;
    private final AssignRolePermissionsUseCase assignRolePermissionsUseCase;
    private final ListRolesUseCase listRolesUseCase;
    private final GetRoleUseCase getRoleUseCase;
    private final UpdateRoleUseCase updateRoleUseCase;
    private final DeleteRoleUseCase deleteRoleUseCase;

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @Operation(
            summary = "역할 생성",
            description = "학원 안에서 사용할 역할을 이름/설명/색상으로 만듭니다. 같은 학원 내 이름 중복은 거부됩니다.")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<RoleCreateResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        Long roleId = createRoleUseCase.createRole(request.toCommand());
        RoleCreateResponse data = RoleCreateResponse.from(roleId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(RoleResponseCode.ROLE_CREATED, data));
    }

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @Operation(
            summary = "역할 목록 조회",
            description = "소속 학원의 역할 목록을 조회합니다. 권한 목록은 포함하지 않습니다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<RoleListResponse>>> list() {
        List<RoleListResponse> data = listRolesUseCase.listRoles().stream()
                .map(RoleListResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(RoleResponseCode.ROLE_LIST_FOUND, data));
    }

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @Operation(
            summary = "역할 상세 조회",
            description = "역할 하나의 이름/설명/색상/인원수/권한 목록을 조회합니다. 다른 학원 소속이면 미존재와 동일하게 404로 응답합니다.")
    @GetMapping("/{roleId}")
    public ResponseEntity<GlobalApiResponse<RoleDetailResponse>> get(
            @Parameter(description = "역할 ID") @PathVariable Long roleId) {
        RoleDetailResponse data = RoleDetailResponse.from(getRoleUseCase.getRole(roleId));
        return ResponseEntity.ok(GlobalApiResponse.ok(RoleResponseCode.ROLE_DETAIL_FOUND, data));
    }

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @Operation(
            summary = "역할 수정",
            description = "역할의 이름/설명/색상을 수정합니다. 권한 목록은 이 API로 바꿀 수 없습니다(권한 조립 API 사용).")
    @PutMapping("/{roleId}")
    public ResponseEntity<Void> update(
            @Parameter(description = "역할 ID") @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleRequest request) {
        updateRoleUseCase.updateRole(request.toCommand(roleId));
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @Operation(
            summary = "역할 삭제",
            description = "역할을 삭제합니다. ACTIVE 상태인 구성원이 이 역할을 쓰고 있으면 거부됩니다.")
    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "역할 ID") @PathVariable Long roleId) {
        deleteRoleUseCase.deleteRole(new DeleteRoleCommand(roleId));
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('ROLE:MANAGE')")
    @Operation(
            summary = "역할 권한 조립",
            description = "역할에 부여할 권한 코드 목록으로 전체 교체합니다(합집합이 아님). 빈 배열을 보내면 모든 권한이 제거됩니다.")
    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<Void> assignPermissions(
            @Parameter(description = "역할 ID") @PathVariable Long roleId,
            @Valid @RequestBody AssignRolePermissionsRequest request) {
        assignRolePermissionsUseCase.assignPermissions(request.toCommand(roleId));
        return ResponseEntity.noContent().build();
    }
}
