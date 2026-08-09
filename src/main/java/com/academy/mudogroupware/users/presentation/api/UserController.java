package com.academy.mudogroupware.users.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academy.mudogroupware.global.presentation.api.common.GlobalApiResponse;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.result.CreateAccountResult;
import com.academy.mudogroupware.users.application.usecase.ChangeUserRoleUseCase;
import com.academy.mudogroupware.users.application.usecase.CreateAccountUseCase;
import com.academy.mudogroupware.users.application.usecase.SearchUsersUseCase;
import com.academy.mudogroupware.users.presentation.api.common.UserResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.ChangeUserRoleRequest;
import com.academy.mudogroupware.users.presentation.api.request.CreateAccountRequest;
import com.academy.mudogroupware.users.presentation.api.response.AccountCreateResponse;
import com.academy.mudogroupware.users.presentation.api.response.UserSearchResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final ChangeUserRoleUseCase changeUserRoleUseCase;
    private final SearchUsersUseCase searchUsersUseCase;
    private final CreateAccountUseCase createAccountUseCase;

    @PreAuthorize("hasAuthority('ACCOUNT:MANAGE')")
    @PostMapping
    public ResponseEntity<GlobalApiResponse<AccountCreateResponse>> createAccount(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody CreateAccountRequest request) {
        CreateAccountResult result = createAccountUseCase.createAccount(request.toCommand(authUser.academyId()));
        AccountCreateResponse data = AccountCreateResponse.from(result);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(GlobalApiResponse.created(UserResponseCode.ACCOUNT_CREATED, data));
    }

    @PreAuthorize("hasAuthority('ACCOUNT:MANAGE')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> changeRole(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long userId,
            @Valid @RequestBody ChangeUserRoleRequest request) {
        changeUserRoleUseCase.changeRole(request.toCommand(userId, authUser.academyId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<UserSearchResponse>>> search(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestParam(required = false) String keyword) {
        List<UserSearchResponse> data = searchUsersUseCase.search(authUser.academyId(), keyword).stream()
                .map(UserSearchResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(UserResponseCode.USER_SEARCHED, data));
    }
}
