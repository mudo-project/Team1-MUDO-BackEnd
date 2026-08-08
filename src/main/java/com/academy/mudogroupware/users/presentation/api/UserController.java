package com.academy.mudogroupware.users.presentation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.usecase.ChangeUserRoleUseCase;
import com.academy.mudogroupware.users.presentation.api.request.ChangeUserRoleRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ChangeUserRoleUseCase changeUserRoleUseCase;

    @PreAuthorize("hasAuthority('ACCOUNT:MANAGE')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> changeRole(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserRoleRequest request) {
        changeUserRoleUseCase.changeRole(request.toCommand(userId, authUser.academyId()));
        return ResponseEntity.noContent().build();
    }
}
