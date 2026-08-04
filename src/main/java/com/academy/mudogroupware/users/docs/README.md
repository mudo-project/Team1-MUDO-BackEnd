# users 모듈

## 책임과 범위

계정·권한(로그인, 회원, 역할)을 담당한다. 현재는 로그인과 액세스 토큰 재발급이 구현돼 있다.

- **User(사용자)**: 학원 소속 직원(원장/행정/강사/조교) 계정. `academy_id`로 소속 학원을 가진다.

## 담당자

be4 (계정·권한)

## 소유하는 주요 데이터와 상태

- `User` — DB 테이블 `users` (원래 approval 도메인이 초기세팅 때 임시로 만든 테이블을 `V4.1.1__align_users_table_with_erd.sql`로 ERD 확정안에 맞게 정합화함)
- `User.status`: `ACTIVE` / `RESIGNED` / `INACTIVE`. `RESIGNED`/`INACTIVE` 상태는 로그인 불가.
- `role` 컬럼은 현재 문자열 하나로만 존재한다. `role`/`permission`/`role_permission`/`user_role` 조립식 권한 테이블로의 전환은 아직 하지 않았다 — 나머지 계정·권한 엔티티(역할·권한 관리)를 만들 때 JWT 클레임 구조까지 함께 재작업할 예정이다.
- 테이블명(`users`, 복수형)과 PK 컬럼명(`id`)은 ERD 컨벤션(단수형 `user`, `user_id`)과 다르다. `approval` 도메인의 `UserNameEntity`가 이미 `users` 테이블을 참조하고 있어 임의로 rename하지 않았다. rename이 필요해지면 `docs/MODULES.md`의 "타 모듈 변경 요청" 절차로 approval 담당자와 협의한다.

## 외부에 공개하는 Application API

인증 (`/api/auth`, `/api/token`):
- `LoginUseCase` — 로그인. 아이디·비밀번호 검증 후 `auth` 모듈의 `TokenIssuerUseCase`를 통해 토큰을 발급한다. accessToken은 응답 바디, refreshToken은 `RefreshTokenCookieFactory`가 만드는 HttpOnly 쿠키로 내려간다.
- `RefreshUseCase` — `POST /api/token/reissue`. 요청의 `refreshToken` HttpOnly 쿠키를 `auth` 모듈의 `RefreshTokenValidatorUseCase`로 검증하고, 검증된 사용자 정보로 `TokenIssuerUseCase.issueAccessToken()`을 호출해 accessToken만 재발급한다. **refreshToken은 로테이션하지 않는다** — 재발급 응답에도 새 refreshToken 쿠키를 내려주지 않고, 기존 쿠키가 만료 전까지 그대로 유지된다.

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **토큰 발급**: `auth.application.usecase.TokenIssuerUseCase`(auth 모듈이 공개한 계약, 구현체는 `TokenService`)를 호출한다.
- **리프레시 토큰 검증**: `auth.application.usecase.RefreshTokenValidatorUseCase`(구현체는 `TokenService`)를 호출한다. JWT 자체 위조/DB 미존재/DB 불일치를 각각 다른 `AuthErrorCode`로 구분해서 던진다.
- **비밀번호 검증**: Spring Security `PasswordEncoder`(BCrypt, `global.infrastructure.security.config.SecurityConfig`에 Bean으로 등록됨)를 사용한다.

## 발행·소비하는 Event

- 현재 없음.

## 변경 시 주의 사항

- 로그인 실패 시 "아이디 또는 비밀번호가 올바르지 않습니다"로 통일해서 응답한다 — 아이디 존재 여부가 노출되지 않도록 하기 위함.
- 계정 발급(회원가입) API는 아직 없다. ERD상 자체 회원가입이 아니라 "원장이 하위 계정 발급" 흐름으로 확정돼 있어, 이후 구현 시 권한 검증(원장만 발급 가능)이 필요하다.
- `role`/`permission` 조립식 권한 전환 시 `JwtClaims`, `JwtTokenProvider`, `AuthUser`(모두 `global` 하위, 현재 role을 단일 문자열로 다룸)도 함께 변경해야 한다.

## 세부 문서

- [API.md](API.md) — 엔드포인트별 요청/응답 예시, 검증 규칙, 오류 코드
- [API_FLOW.md](API_FLOW.md) — 계층별 호출 흐름
- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점 변경 요약
