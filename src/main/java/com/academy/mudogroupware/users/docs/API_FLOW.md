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
→ TokenIssuerUseCase.issue(id, username, role)  ※ auth 모듈의 TokenService
   → JwtTokenProvider.createAccessToken / createRefreshToken
   → RefreshTokenRepository: 기존 행 있으면 교체(replace), 없으면 저장(save)
   → TokenPair(accessToken, refreshToken) 반환
→ LoginResponse(accessToken)
→ RefreshTokenCookieFactory.create(refreshToken) → Set-Cookie 헤더
→ GlobalApiResponse<LoginResponse>
```

- `role`은 현재 `users.role` 문자열 컬럼 값을 그대로 JWT 클레임에 싣습니다(조립식 권한 전환 전 상태).
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
→ TokenIssuerUseCase.issueAccessToken(id, username, role)  ※ 액세스 토큰만 새로 생성, refreshToken 저장소는 건드리지 않음
→ RefreshResponse(accessToken)
→ GlobalApiResponse<RefreshResponse>
```

- 로그인 흐름의 `TokenIssuerUseCase.issue()`(쌍 발급 + 저장)와 달리, 재발급은 `issueAccessToken()`(액세스 토큰만 생성, DB 쓰기 없음)을 호출합니다 — 그래서 `RefreshService`는 `@Transactional(readOnly = true)`로 선언되어 있습니다.
- JWT 자체 위조/만료(`AUTH_401_1`/`AUTH_401_2`)와, DB에 없거나(`AUTH_401_6`) DB 값과 다른 경우(`AUTH_401_7`)를 서로 다른 코드로 구분합니다 — 클라이언트가 "토큰을 완전히 새로 받아야 하는 경우"와 "다른 기기에서 로그인해서 밀려난 경우"를 구분할 수 있게 하기 위함입니다.

---

## 📝 문서 정보

- 업데이트일: `2026-08-04`
- 변경 사항(요약):
  - 로그인 흐름을 처음 작성했습니다.
  - 액세스 토큰 재발급 흐름을 추가했습니다 (리프레시 토큰 검증 3단계 분기 포함).
