# 🔄 계정·권한(users) 처리 Flow

## 1. 로그인 흐름

```text
익명 요청 (SecurityConfig: POST /api/auth/login permitAll)
→ AuthController.login
→ LoginRequest(username, password) → LoginCommand
→ LoginUseCase.login (LoginService)
→ UserRepository.findByUsername
   → 없으면 UserException(LOGIN_FAILED)
→ PasswordEncoder.matches(입력 비밀번호, 저장된 해시)
   → 불일치하면 UserException(LOGIN_FAILED) — 위 "없음" 케이스와 동일 코드
→ User.ensureLoginAllowed()
   → status != ACTIVE 이면 UserException(LOGIN_RESTRICTED)
→ TokenIssuerUseCase.issue(id, username, roleId, academyId, accountType, adminScope)  ※ auth 모듈의 TokenService
   → JwtTokenProvider.createAccessToken / createRefreshToken
   → RefreshTokenRepository: 기존 행 있으면 교체(replace), 없으면 저장(save)
   → TokenPair(accessToken, refreshToken) 반환
→ LoginResponse(accessToken)
→ RefreshTokenCookieFactory.create(refreshToken) → Set-Cookie 헤더
→ GlobalApiResponse<LoginResponse>
```

- JWT엔 `roleId`/`academyId`만 싣습니다. `roleName`이나 permission 목록은 싣지 않고, 매 요청마다 새로 조회합니다(아래 3번 흐름).
- refreshToken은 응답 바디에 절대 포함되지 않고, HttpOnly 쿠키로만 전달됩니다.

## 2. 액세스 토큰 재발급 흐름

```text
익명 요청 (SecurityConfig: POST /api/token/reissue permitAll)
→ TokenController.reissue
→ @CookieValue(RefreshTokenCookieFactory.REFRESH_TOKEN_COOKIE) 로 refreshToken 추출
→ RefreshCommand(refreshToken)
→ RefreshUseCase.refresh (RefreshService)
→ refreshToken null/blank 이면 UserException(REFRESH_TOKEN_NOT_FOUND)
→ RefreshTokenValidatorUseCase.validateStored(refreshToken)  ※ auth 모듈의 TokenService
   → JwtTokenProvider.parseRefreshToken
      → 서명 위조/형식 오류 시 AuthException(INVALID_TOKEN)
      → 만료 시 AuthException(EXPIRED_TOKEN)
   → RefreshTokenRepository.findByUserId
      → 없으면 AuthException(REFRESH_TOKEN_NOT_FOUND)
   → 저장된 토큰 문자열과 요청 토큰 비교
      → 불일치 시 AuthException(REFRESH_TOKEN_MISMATCH)
   → RefreshTokenClaims(userId, username) 반환
→ UserRepository.findById(claims.userId())
   → 없으면 UserException(USER_NOT_FOUND)
→ User.ensureLoginAllowed()
   → status != ACTIVE 이면 UserException(LOGIN_RESTRICTED)
→ TokenIssuerUseCase.issueAccessToken(id, username, roleId, academyId, accountType, adminScope)  ※ 액세스 토큰만 새로 생성, refreshToken 저장소는 건드리지 않음
→ RefreshResponse(accessToken)
→ GlobalApiResponse<RefreshResponse>
```

- 로그인 흐름의 `TokenIssuerUseCase.issue()`(쌍 발급 + 저장)와 달리, 재발급은 `issueAccessToken()`(액세스 토큰만 생성, DB 쓰기 없음)을 호출합니다 — 그래서 `RefreshService`는 `@Transactional(readOnly = true)`로 선언되어 있습니다.
- JWT 자체 위조/만료(`AUTH_401_1`/`AUTH_401_2`)와, DB에 없거나(`AUTH_401_6`) DB 값과 다른 경우(`AUTH_401_7`)를 서로 다른 코드로 구분합니다 — 클라이언트가 "토큰을 완전히 새로 받아야 하는 경우"와 "다른 기기에서 로그인해서 밀려난 경우"를 구분할 수 있게 하기 위함입니다.
- 재발급 시점의 `roleId`는 액세스 토큰 발급 당시(로그인 또는 마지막 재발급) 값을 그대로 이어받습니다 — 재발급 자체가 역할을 다시 확인하는 절차는 아닙니다.

## 3. 요청 인증·인가 흐름 (인증이 필요한 모든 요청마다 반복)

```text
모든 요청 (SecurityConfig: 명시적으로 permitAll 안 된 경로는 authenticated() 필요)
→ JwtAuthenticationFilter (OncePerRequestFilter)
→ Authorization 헤더 또는 accessToken 쿠키에서 토큰 추출
→ JwtTokenProvider.parseAccessToken → JwtClaims(userId, username, roleId, academyId, accountType, adminScope)
   → 위조/만료 시 request attribute에 에러코드만 저장(필터는 그냥 통과, 이후 인가 단계에서 401/403으로 응답)
→ JwtAuthenticationConverter.toAuthentication(claims)
   
   【platform admin 분기】
   → accountType == ADMIN && adminScope == PLATFORM 인지 확인
      → YES: PlatformAdminPermissionPort.allPermissionCodes()  ※ users 도메인의 PlatformAdminPermissionAdapter가 구현
         → 모든 권한 코드 반환
         → RolePermissionInfo(roleName="SUPER_ADMIN", permissionCodes=전체)
      
      → NO (MEMBER 또는 ADMIN+ACADEMY): RolePermissionLookupPort.lookup(roleId)  ※ users 도메인의 RolePermissionLookupAdapter가 구현
         → roleId가 null이면 즉시 빈 RolePermissionInfo 반환(DB 조회 안 함)
         → role → role_permission → permission 조인 조회 (@Transactional(readOnly=true))
         → RolePermissionInfo(roleName, permissionCodes)
   
   → AuthUser(userId, username, academyId, roleId, roleName, accountType, adminScope)
   → authorities = permissionCodes를 SimpleGrantedAuthority로 변환한 목록
→ SecurityContextHolder에 Authentication 저장
→ 컨트롤러의 @PreAuthorize("hasAuthority('RESOURCE:ACTION')")가 authorities를 검사
```

- **platform admin(`accountType==ADMIN && adminScope==PLATFORM`)인 경우**, 역할 조회 단계를 완전히 건너뛰고 `PlatformAdminPermissionPort`에서 전체 권한 목록을 받아 `roleName="SUPER_ADMIN"`으로 고정합니다.
- **그 외의 계정(`MEMBER` 또는 아직 미사용 상태인 `ADMIN+ACADEMY`)**은 기존 `RolePermissionLookupPort.lookup(roleId)` 경로를 통해 역할의 권한을 조회합니다.
- `roleId`가 없는 평신원 계정(비platform admin, 역할 미할당)은 `RolePermissionLookupAdapter`에서 DB 조회 없이 빈 권한으로 처리됩니다.
- 역할을 가진 계정의 roleId→permission 조회는 **매 요청마다** 실행됩니다. JWT에 permission을 통째로 넣지 않은 이유는, 원장이 역할의 권한 구성을 바꾸면 재로그인 없이 다음 요청부터 바로 반영되게 하기 위함입니다.

## 4. 로그아웃 흐름

```text
인증된 요청 (SecurityConfig: POST /api/auth/logout 은 permitAll 목록에 없음 → authenticated() 필요)
→ AuthController.logout
→ @AuthenticationPrincipal AuthUser 에서 userId 추출 (JWT 재검증 없이 이미 인증 단계에서 채워진 값 재사용)
→ LogoutUseCase.logout (LogoutService)
→ TokenRevokerUseCase.revoke(userId)  ※ auth 모듈의 TokenService
   → RefreshTokenRepository.deleteByUserId
→ RefreshTokenCookieFactory.clear() → Max-Age=0 Set-Cookie 헤더
→ GlobalApiResponse<Void>
```

- 로그아웃은 서버에 저장된 refreshToken만 삭제합니다. 이미 발급된 accessToken 자체를 무효화하는 블랙리스트는 없습니다 — accessToken 수명이 짧아 별도 저장소 없이도 허용 가능한 범위로 판단했습니다.
- `revoke`는 `TokenIssuerUseCase`/`RefreshTokenValidatorUseCase`와 동일하게 `auth` 모듈이 공개한 `TokenRevokerUseCase` 계약을 통해 호출합니다.

---

## 📝 문서 정보

- 업데이트일: `2026-08-07`
- 변경 사항(요약):
  - 로그인 흐름을 처음 작성했습니다.
  - 액세스 토큰 재발급 흐름을 추가했습니다 (리프레시 토큰 검증 3단계 분기 포함).
  - JWT를 `roleId`/`academyId` 기반으로 재작업하고, 매 요청 권한 조회 흐름(3번)을 추가했습니다.
  - 로그아웃 흐름(4번)을 추가했습니다.
  - JWT에 `accountType`/`adminScope` 매개변수 전파, `JwtAuthenticationConverter`에 platform admin 분기 로직 추가 (accountType==ADMIN && adminScope==PLATFORM인 경우 RolePermissionLookupPort 대신 PlatformAdminPermissionPort 호출).
