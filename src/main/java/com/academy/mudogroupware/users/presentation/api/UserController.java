package com.academy.mudogroupware.users.presentation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.users.application.result.CreateAccountResult;
import com.academy.mudogroupware.users.application.usecase.ChangeUserRoleUseCase;
import com.academy.mudogroupware.users.application.usecase.CreateAccountUseCase;
import com.academy.mudogroupware.users.application.usecase.ListMembersUseCase;
import com.academy.mudogroupware.users.application.usecase.PasswordSetupUseCase;
import com.academy.mudogroupware.users.application.usecase.SearchUsersUseCase;
import com.academy.mudogroupware.users.presentation.api.common.UserResponseCode;
import com.academy.mudogroupware.users.presentation.api.request.ChangeUserRoleRequest;
import com.academy.mudogroupware.users.presentation.api.request.CreateAccountRequest;
import com.academy.mudogroupware.users.presentation.api.request.PasswordSetupRequest;
import com.academy.mudogroupware.users.presentation.api.response.AccountCreateResponse;
import com.academy.mudogroupware.users.presentation.api.response.MemberListResponse;
import com.academy.mudogroupware.users.presentation.api.response.UserSearchResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(name = "구성원", description = "학원 구성원 검색/역할 변경 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final ChangeUserRoleUseCase changeUserRoleUseCase;
    private final SearchUsersUseCase searchUsersUseCase;
    private final CreateAccountUseCase createAccountUseCase;
    private final PasswordSetupUseCase passwordSetupUseCase;
    private final ListMembersUseCase listMembersUseCase;

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

    @PreAuthorize("hasAuthority('ACCOUNT:MANAGE')")
    @Operation(
            summary = "구성원 목록 조회(관리자)",
            description = "같은 학원 소속 구성원 전체(퇴사자 포함)를 이름/이메일/전화번호/역할명/입사일/계정상태/오늘 근태상태와 함께 조회합니다. "
                    + "keyword는 이름 또는 역할명에 부분 일치합니다. roleId를 지정하면 해당 역할 구성원만 반환합니다(조직도 화면에서 역할 탭별로 호출하는 용도). "
                    + "결과는 역할명, 이름 순으로 정렬됩니다.")
    @GetMapping("/members")
    public ResponseEntity<GlobalApiResponse<SliceResponse<MemberListResponse>>> listMembers(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "이름 또는 역할명 부분 일치 검색어. 없으면 전체 반환")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "특정 역할의 구성원만 조회. 없으면 전체 역할 포함")
            @RequestParam(required = false) Long roleId,
            @Parameter(description = "페이지 번호(0부터 시작)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "페이지 크기(1~100)")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        SliceResponse<MemberListResponse> data = SliceResponse.from(
                listMembersUseCase.list(authUser.academyId(), keyword, roleId, page, size),
                MemberListResponse::from);
        return ResponseEntity.ok(GlobalApiResponse.ok(UserResponseCode.MEMBERS_LISTED, data));
    }

    @Operation(
            summary = "최초 비밀번호 설정",
            description = "계정 발급 시 받은 링크의 username/임시비밀번호로 자기 비밀번호를 최초 설정합니다. "
                    + "이미 설정을 마친 계정이거나 임시비밀번호가 틀리면 동일한 오류로 응답합니다.")
    @PostMapping("/password-setup")
    public ResponseEntity<Void> setupPassword(@Valid @RequestBody PasswordSetupRequest request) {
        passwordSetupUseCase.setup(request.toCommand());
        return ResponseEntity.noContent().build();
    }
}
