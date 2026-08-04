> 작성일: 2026-08-04
> 상태: 🚧 로그인·토큰 재발급 완료 · 계정 발급(회원가입)·조립식 권한 미착수

## 🎯 변경 목적

계정·권한(users) 도메인을 신설하고, 로그인과 액세스 토큰 재발급을 구현한다. 초기세팅 때 approval 도메인이 참조용으로 임시로 만들어둔 `users` 테이블을 팀이 확정한 ERD에 맞게 정합화하고, 그 위에서 인증 흐름을 짠다.

---

## ✅ 2026-08-03 · `users` 테이블 ERD 정합화 (`V4.1.1`)

### 배경

`users` 테이블은 approval 도메인 작업자가 초기세팅 커밋에서 다른 도메인이 `user_id`를 참조할 수 있도록 임시로 만들어둔 것으로, 확정 ERD(`user`, 단수형)와 컬럼 구성이 상당히 달랐다: PK가 `user_id`가 아닌 `id`, 재직 상태를 `hire_date`/`resign_date` 두 컬럼으로 표현, `must_change_pw`/`is_platform_admin`/`created_at`/`updated_at` 없음.

### 확정된 정책

- **추가**: `status`(`ACTIVE`/`RESIGNED`/`INACTIVE`), `joined_at`, `must_change_pw`, `is_platform_admin`, `created_at`, `updated_at`
- **삭제**(데이터 이관 후): `hire_date` → `joined_at`으로, `resign_date` 존재 여부 → `status = 'RESIGNED'`로 변환한 뒤 컬럼 제거
- **`email`은 ERD 원안에 없었으나 실무 필요성 판단 하에 유지 확정** — 공통 스키마에도 반영
- **테이블명(`users`)과 PK명(`id`)은 이번엔 바꾸지 않음** — `approval` 모듈의 `UserNameEntity`가 이미 `@Table(name="users")`로 참조 중이라, 다른 도메인이 의존 중인 걸 임의로 바꿀 수 없다(`AGENTS.md` 절대 규칙). rename이 필요해지면 `docs/MODULES.md`의 "타 모듈 변경 요청" 절차로 approval 담당자와 협의한다.
- **`role` 문자열 컬럼도 이번엔 그대로 둠** — 로그인 MVP부터 이 컬럼으로 동작시키고, 조립식 권한(`role`/`permission`/`role_permission`/`user_role` 4테이블)으로의 전환은 나머지 계정·권한 엔티티를 만들 때 JWT 클레임 구조까지 함께 재작업하기로 함.

### 완료 기준

- [x] `./gradlew test` 통과
- [x] 로컬 DB에 `V4.1.1` 적용 확인 (Flyway `out-of-order` 모드에서 버전 순서상 마지막에 실행됨을 확인)
- [x] `notice` 도메인이 같은 마이그레이션(`resign_date` 삭제)에 맞춰 자기 코드(`UserInfoEntity`)를 수정 — 팀에 반영 사실 전달됨

---

## ✅ 2026-08-03~04 · 로그인·리프레시 토큰 흐름 설계

### 배경

로그인 API 자체가 없는 상태(JWT 발급·파싱 인프라만 존재)에서, 팀의 이전 프로젝트(짐짝/Gym-Jjak_BE)의 `UserAuthController`/`TokenController`/`TokenCommandService` 패턴을 참고해 설계했다.

### 확정된 정책

- **refreshToken은 응답 바디가 아니라 HttpOnly 쿠키로 전달**한다(짐짝과 동일). `SameSite=Lax`, `Secure`, `Path=/`, 만료는 `jwt.refresh-token-expiration` 설정값과 동일.
- **재발급 시 refreshToken은 로테이션하지 않는다** — 액세스 토큰만 새로 발급하고 기존 refreshToken 쿠키는 그대로 유지.
- **로그인 실패는 "아이디 없음"과 "비밀번호 불일치"를 동일한 오류로 응답**한다 — 계정 존재 여부가 노출되지 않도록.
- **Port를 발급(`TokenIssuerUseCase`)과 검증(`RefreshTokenValidatorUseCase`)으로 분리**했다 — 짐짝은 `TokenPort` 하나에 검증·조회·발급을 다 몰아넣는 방식이었으나, 인터페이스 분리 원칙에 맞게 나눴다.
- **리프레시 토큰 검증(JWT 파싱 + DB 조회 + DB 비교)을 `TokenService.validateStored()` 하나로 캡슐화**했다 — 짐짝은 이 세 단계를 호출부에서 개별 호출했는데, 그러면 순서를 지키는 책임이 호출부 코드에 노출된다.
- **`role`은 문자열 그대로 JWT에 싣는다** — 짐짝은 `Role` enum이 있어 `.name()`으로 변환하지만, MUDO는 ERD상 "역할 완전 커스텀"(학원마다 자유롭게 역할 정의) 요구사항 때문에 애초에 고정된 enum 후보군이 성립하지 않는다.

### 완료 기준

- [x] 로그인 성공/실패, 재발급 성공/실패(쿠키 없음/위조 토큰/DB 미존재/DB 불일치) 전 케이스 로컬 curl 검증 완료
- [x] `./gradlew test` 통과

---

## ✅ 2026-08-04 · 토큰 컨트롤러 분리 (`/api/auth/refresh` → `/api/token/reissue`)

### 배경

Redis 도입이 확정되면서(WebSocket Pub/Sub 용도로 시작해 액세스 토큰 블랙리스트·멀티 세션·로그인 rate limiting까지 확장 예정), 토큰/세션 관련 엔드포인트가 앞으로 늘어날 것으로 판단해 로그인(`AuthController`)과 재발급(`TokenController`)을 미리 분리했다.

### 확정된 정책

- `POST /api/token/reissue`로 이동, 짐짝의 `/api/token/*` 네이밍을 따름
- 응답 코드도 `UserResponseCode`에서 분리해 별도 `TokenResponseCode` 신설
- `SecurityConfig`의 CORS 설정 중 쓰이지 않던 `X-Refresh-Token`/`New-Access-Token` 헤더(쿠키 방식 확정 전 임시로 남아있던 설정)를 함께 정리

### 완료 기준

- [x] `./gradlew test` 통과, 신규 경로로 curl 재검증

---

## ✅ 2026-08-04 · 도메인별 ErrorCode 세분화

### 배경

`AGENTS.md`의 문서 라우팅 표가 예외 처리 규칙을 `docs/ERROR_HANDLING.md`로 안내하지만, 이 파일은 실제로 존재하지 않는다. 실제 규칙은 `docs/API_CONTRACT.md`의 "ErrorCode 규칙" 섹션에 있으며, 도메인별 `{Domain}ErrorCode`를 만들어 클라이언트 분기가 가능하게 하라고 명시돼 있다. 최초 구현 시 이를 확인하지 않고 공용 `UnauthorizedException`/`ForbiddenException`(코드가 전부 `COMMON_401_1`/`COMMON_403_1`로 동일)을 사용해 규칙을 어긴 상태였다.

### 확정된 정책

- `users.domain.exception.UserErrorCode`/`UserException` 신설 (`global.domain.auth.AuthErrorCode`/`AuthException`과 동일 패턴)
- `LOGIN_FAILED`(`USER_401_1`), `LOGIN_RESTRICTED`(`USER_403_1`), `REFRESH_TOKEN_NOT_FOUND`(`USER_401_2`), `USER_NOT_FOUND`(`USER_404_1`)
- 리프레시 토큰 자체의 위조/DB 미존재/DB 불일치는 `auth.domain.auth.AuthErrorCode`에 `REFRESH_TOKEN_NOT_FOUND`(`AUTH_401_6`), `REFRESH_TOKEN_MISMATCH`(`AUTH_401_7`) 추가 — 짐짝의 3단계 구분(`INVALID_REFRESH_TOKEN`/`REFRESH_TOKEN_NOT_FOUND`/`REFRESH_TOKEN_MISMATCH`)과 동일한 세분화 수준을 맞춤
- `approval` 도메인도 README에 "팀 논의 후 도입 예정"이라고만 적혀 있었는데, 사실 이미 정해진 규칙이라 새로 논의할 필요 없이 이 패턴을 그대로 적용하면 된다고 정정함

### 완료 기준

- [x] 로그인 실패/계정 제한/리프레시 토큰 없음/유저 없음/위조/DB 미존재/DB 불일치 7개 케이스 각각 다른 코드로 응답하는지 curl 검증 완료

---

## ✅ 2026-08-04 · CodeRabbit 리뷰 반영 (PR #27)

### 배경

머지된 PR #19에 CodeRabbit이 7개 코멘트를 남겼다. 그중 4개(가벼운 수정, 명백히 맞는 지적)를 반영하고, 나머지 3개는 범위 밖이거나 의도된 설계로 판단해 이번에는 반영하지 않았다.

### 확정된 정책 (반영한 것)

- `LoginCommand.toString()`에서 비밀번호 마스킹 — record 기본 `toString()`이 로그/예외 메시지에 평문 비밀번호를 노출하는 문제
- 리프레시 토큰 쿠키 생성 로직을 `RefreshTokenCookieFactory`로 분리 — `AuthController`에만 있으면 향후 재발급 API에서 로테이션을 지원할 때 정책이 중복될 수 있음
- `LoginRequest`에 `@Size` 검증 추가 — `username`은 DB 컬럼(`VARCHAR(50)`) 길이와 일치, `password`는 해싱 자원 낭비 방지
- `users/docs/README.md`에 리프레시 토큰 재발급 흐름 반영 (당시 로그인만 반영돼 있었음)

### 보류·반박한 것 (반영하지 않은 이유)

- **로그인 브루트포스 방어(rate limiting)** — WAF + 앱 레벨 rate limit는 Redis 도입 이후의 인프라 전체 설계에 속하는 별도 작업으로 이미 방향을 잡아뒀다. 이 PR 범위 밖.
- **`TokenService`가 애플리케이션 계층에서 `JwtTokenProvider`/`RefreshTokenJpaEntity`(JPA Entity)를 직접 의존** — `ARCHITECTURE.md` 위반이 맞으나, 이번 세션에서 새로 만든 코드가 아니라 원래 있던 `TokenService` 구조다. Port로 분리하는 큰 리팩터라 별도 이슈로 다루기로 함.
- **`hire_date`/`resign_date` DROP이 비가역적** — 맞는 지적이지만 실수가 아니라, "정확한 퇴사일자는 필요 없고 `status`로 재직상태만 관리"하기로 확정한 의도된 설계다.

### 완료 기준

- [x] `./gradlew test` 통과
- [x] `develop` 최신 상태(다른 팀원 병합분 포함) 위에서 재빌드 성공 확인

---

## 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Presentation | `AuthController`(로그인), `TokenController`(재발급), `RefreshTokenCookieFactory` 신규 |
| Application | `LoginUseCase`/`LoginService`, `RefreshUseCase`/`RefreshService` 신규. `auth` 모듈에 `TokenIssuerUseCase`(발급) 확장, `RefreshTokenValidatorUseCase`(검증) 신규 |
| Domain | `User`(불변 객체, `ensureLoginAllowed()`), `UserStatus`, `UserErrorCode`/`UserException` 신규 |
| Persistence | `UserEntity`, `UserJpaRepository`, `UserRepositoryImpl` 신규 (`users` 테이블 재사용) |
| Migration | `V4.1.1`(users 테이블 ERD 정합화) |
| 공통(`global`) | `AuthErrorCode`에 리프레시 토큰 실패 코드 2종 추가, `SecurityConfig` CORS 설정 정리 |

## 🧪 완료 기준 (전체)

- [x] 로그인 성공/실패 각 코드별 응답 확인
- [x] 액세스 토큰 재발급 성공/실패(4가지 실패 유형) 각 코드별 응답 확인
- [x] `./gradlew test` 통과
- [x] CodeRabbit 리뷰 4건 반영
- [ ] 계정 발급(회원가입, 원장이 하위 직원 계정 생성) API — 미착수
- [ ] 로그아웃 API — `TokenService.revoke()`는 있으나 호출하는 컨트롤러 없음, 미착수
- [ ] 조립식 권한(`role`/`permission`/`role_permission`/`user_role`) 전환 — 미착수
- [ ] `academy` 테이블 생성 후 `users.academy_id`에 FK 제약 추가 필요 (다른 팀원 작업 진행 중)

## 📌 후속 문서

완료된 변경의 사용자 관점 요약은 [CHANGELOG.md](CHANGELOG.md)에서 확인할 수 있다.
