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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "구성원", description = "학원 구성원 검색/역할 변경 API")
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
    @Operation(
            summary = "구성원 역할 변경",
            description = "일반 직원 계정(accountType=MEMBER)의 역할을 같은 학원 소속 다른 역할로 바꿉니다. 역할 해제(역할 없음)는 지원하지 않습니다.")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<Void> changeRole(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "대상 구성원의 사용자 ID") @PathVariable Long userId,
            @Valid @RequestBody ChangeUserRoleRequest request) {
        changeUserRoleUseCase.changeRole(request.toCommand(userId, authUser.academyId()));
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "학원 구성원 검색",
            description = "같은 학원 소속 ACTIVE 구성원을 이름으로 검색합니다. 키워드가 없으면 전체 목록을 반환합니다. 로그인만 되면 권한 제약 없이 호출할 수 있습니다.")
    @GetMapping
    public ResponseEntity<GlobalApiResponse<List<UserSearchResponse>>> search(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "이름 부분 일치 검색어. 없으면 전체 목록 반환")
            @RequestParam(required = false) String keyword) {
        List<UserSearchResponse> data = searchUsersUseCase.search(authUser.academyId(), keyword).stream()
                .map(UserSearchResponse::from)
                .toList();
        return ResponseEntity.ok(GlobalApiResponse.ok(UserResponseCode.USER_SEARCHED, data));
    }
}
