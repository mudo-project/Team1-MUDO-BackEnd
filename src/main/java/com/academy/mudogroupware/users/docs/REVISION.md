> 작성일: 2026-08-04
> 상태: 🚧 로그인·토큰 재발급·조립식 권한 인증 기반 완료, 로그아웃 API 완료, 역할 생성 + 권한 조립 API 완료 · 역할 수정·삭제·목록/상세 조회, 계정 발급(회원가입) API 미착수

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

## ✅ 2026-08-04 · 조립식 권한(Role/Permission) 인증·인가 기반 (PR #38)

### 배경

팀원들이 `notice`/`approval` 등 각자 도메인에 권한 체크를 붙이지 못하고 "`users.role` 값 체계 확정 전까지 미반영"으로 막혀 있었다. 계정 발급·역할 관리 API까지 한 번에 설계하려 했으나, 계정 생성은 "어떤 role을 줄지" 자체가 role/permission 구조에 의존해서 분리가 안 됐다. 팀원 요청이 급한 건 "권한 체크가 가능한 기반"이었으므로, 계정 발급·역할 관리 API는 후속으로 미루고 **인증·인가 기반만 먼저 구현**했다(PR #19 이후 두 번째로 겪은 동일한 패턴: 큰 설계를 한 번에 하지 말고 필요한 만큼만 먼저 쪼개서 내보낸다).

### 확정된 정책

- **`user_role` 중간테이블(M:N) 대신 `users.role_id` 단일 FK** — 설계 초반엔 "역할은 IAM식으로 여러 개 조립 가능"으로 갔으나, "겸직" 같은 다중 역할 요구사항이 실제로 확인된 적이 없어 `AGENTS.md`의 "안 물어본 유연성 만들지 않는다" 원칙에 따라 단순화했다. 나중에 필요해지면 그때 M:N으로 마이그레이션한다.
- **`permission`은 학원별 커스텀이 아니라 시스템 전체 고정 카탈로그** — `role`(학원마다 자유)과 다르게, `permission.code`는 실제 존재하는 기능(API 엔드포인트)에 대응해야 하므로 개발자가 기능을 만들 때마다 추가한다. 이번엔 계정·권한 자신의 기능(`ROLE:MANAGE`, `ACCOUNT:CREATE`)만 시드했다 — `notice`/`approval`은 아직 자기 권한 코드를 안 정했으므로 임의로 만들어주지 않았다.
- **와일드카드(`RESOURCE:*`) 확장은 이번엔 안 함** — 세부 액션 목록이 정해진 리소스가 하나도 없고, `@PreAuthorize`를 실제로 쓰는 곳도 아직 없어서 시기상조로 판단. 실제 필요한 도메인이 나오면 그때 설계.
- **JWT엔 `roleId`/`academyId`만, `roleName`과 permission 목록은 매 요청 조회** — `academyId`는 계정 소속이 구조적으로 불변(`user.academy_id` 단일소속 확정)이라 JWT에 바로 넣어도 안전하다. `roleId`는 재배정 가능해서 재로그인 전까지 예전 값을 유지하는 걸 감수했지만, `roleName`·permission까지 같이 굳히면 "역할 이름만 바꿔도 재로그인 전까진 화면에 예전 이름"이라는 불필요한 staleness가 하나 더 생긴다. 그래서 `roleId`로 매 요청 DB를 조회해 `roleName`+permission을 그때그때 새로 구성한다.
- **`global.domain.auth.RolePermissionLookupPort`로 계층 분리** — `JwtAuthenticationConverter`(global)가 role/permission 테이블을 직접 조회하면 `ARCHITECTURE.md`가 금지하는 "global이 도메인 데이터를 아는 것"이 된다. Port는 global에, 구현(`RolePermissionLookupAdapter`)은 users에 두어 `approval`/`notice`가 겪었던 임시 shim 패턴과 반대 방향(도메인이 global에 정식으로 제공)으로 처음부터 올바르게 만들었다.
- **`hasAuthority` 방식 채택, `hasRole` 아님** — `permission.code`가 `RESOURCE:ACTION` 형태라 Spring Security가 자동으로 붙이는 `ROLE_` 접두어(`hasRole`)와 안 맞는다.

### ⚠️ Breaking Change

- `users.role`(문자열) 컬럼을 제거하고 `role_id`로 대체했다(`V4.1.2`). `notice`의 `UserInfoEntity`가 이 컬럼을 직접 매핑하고 있어 병합 후 `Unknown column 'role'` 오류가 난다 — notice 담당자가 `resign_date` 삭제 때와 같은 방식으로 자기 shim을 고쳐야 한다(PR #38 리뷰 코멘트로 안내).

### 완료 기준

- [x] 로컬 DB에 role/permission 직접 시드 후 로그인 → JWT payload에 `roleId`/`academyId` 정상 포함 확인
- [x] `roleId` → permission 조회가 예외 없이 동작하고, 인증만 필요한 엔드포인트(`/api/approvals/me/pending-count`)가 `200 OK` 응답하는 것으로 authorities 구성 전 과정 검증
- [x] `./gradlew test` 통과 (JWT 관련 테스트 전부 `roleId`/`academyId` 기준으로 갱신)
- [ ] 역할 생성·수정·삭제, 권한 조립, 직원 계정 발급 API — 후속 PR

---

## ✅ 2026-08-04 · 로그아웃 API 추가, `users.academy_id` FK 연결 (이슈 #41, #42)

### 배경

역할/권한 인증 기반(PR #38) 완료 후, 후속 대형 작업(역할 관리·계정 발급 API) 전에 독립적으로 처리 가능한 작은 항목 두 개를 먼저 정리했다. 둘 다 큰 작업의 선행 조건은 아니지만, 방치할 이유도 없어 먼저 처리했다.

### 확정된 정책 — 로그아웃 API

- `TokenService.revoke(Long id)`는 이미 존재했으나 호출하는 컨트롤러가 없었다. `auth` 모듈에 `TokenRevokerUseCase` 계약을 새로 만들어 `TokenIssuerUseCase`/`RefreshTokenValidatorUseCase`와 동일한 Port 패턴으로 노출했다.
- `users` 쪽엔 `LogoutUseCase`/`LogoutService`를 신설해 `AuthController.logout`(`POST /api/auth/logout`, 인증 필요)에서 호출한다. `userId`는 `@AuthenticationPrincipal AuthUser`에서 꺼낸다 — 이미 인증 필터를 통과한 값이라 재검증하지 않는다.
- accessToken 블랙리스트는 만들지 않았다 — accessToken 수명이 짧고, 서버에 저장된 refreshToken만 지우면 재발급(`/api/token/reissue`)이 막히므로 실질적인 로그아웃 효과는 충분하다고 판단했다.
- 응답에서 refreshToken 쿠키를 `Max-Age=0`으로 즉시 만료시킨다(`RefreshTokenCookieFactory.clear()`).

### 확정된 정책 — `users.academy_id` FK

- `users.academy_id`는 `academy` 테이블(V2.1.2, 다른 팀원이 자기 도메인 작업 진행을 위해 먼저 만들어둔 테이블)보다 먼저 생긴 컬럼이라 FK가 없는 상태였다. `academy → users`(소유자 참조, `fk_academy_user`)는 이미 걸려있는데 반대 방향만 비어있어 정합성 공백이었다.
- `V4.1.3`으로 `fk_users_academy` FK를 추가했다. `ON DELETE`는 기본(RESTRICT)으로 뒀다 — 소속 직원이 남아있는 학원을 실수로 삭제하지 못하게 막는 게 맞다고 판단했다(반대 방향의 `ON DELETE SET NULL`과는 의도가 다르다: 원장 계정이 사라져도 학원 자체는 남아야 하지만, 학원이 사라지는데 소속 직원 레코드가 고아로 남는 건 막아야 한다).
- JPA `@ManyToOne` 관계는 추가하지 않았다 — `users` 도메인이 `academy` 도메인 엔티티를 직접 참조하게 되면 `ARCHITECTURE.md`의 도메인 간 직접 참조 금지 원칙을 깬다. DB 레벨 FK만 걸고 `UserEntity.academyId`는 계속 `Long`으로 유지한다.
- 적용 전 로컬 DB에서 `users.academy_id` 중 `academy.academy_id`에 없는 값이 있는지 확인 후 진행했다(orphan 없음 확인).

### 완료 기준

- [x] `./gradlew compileJava` 통과
- [x] 로컬 DB에 FK 수동 적용 후 정상 동작 확인, Flyway 히스토리와 어긋나지 않도록 원복
- [ ] 실제 앱 재시작으로 Flyway가 `V4.1.3`을 정식 적용하는지 확인 — PR 리뷰/머지 전 확인 필요

---

## ✅ 2026-08-05 · 역할 생성 API 구현 (`POST /api/roles`, 이슈 #59)

### 배경

`docs/superpowers/specs/2026-08-04-role-management-api-design.md`에서 설계한 역할 관리 API 7개 중 첫 번째인 역할 생성부터 구현했다. 설계 시점엔 보류했던 두 가지를 이번에 확정했다.

### 확정된 정책

- **에러 예외 클래스는 새 `docs/ERROR_HANDLING.md` 컨벤션을 따랐다.** 설계 시점엔 "구현 시점에 결정"으로 미뤄뒀는데, 구현하는 사이 `global`의 `NotFoundException`/`ConflictException`/`BadRequestException`에 `protected ErrorCode` 생성자가 실제로 추가됐고 `workspace` 도메인(`WorkspaceNameConflictException`)이 이미 이 방식을 쓰고 있어서, 기존 `UserException(UserErrorCode)` 단일 생성자 대신 `RoleNameDuplicateException extends ConflictException`을 새로 만들었다. 기존 `LOGIN_FAILED` 등에서 쓰던 `UserException`은 이번 작업에서 건드리지 않았다(범위 밖, 두 패턴이 과도기적으로 공존).
- **이름 중복은 애플리케이션 사전 체크 + DB 유니크 제약 둘 다로 방어한다.** `CreateRoleService`가 `existsByAcademyIdAndName`으로 먼저 확인하지만, 두 요청이 동시에 들어오면 이 체크만으로는 못 막는다(check-then-act race). `role(academy_id, name)` UNIQUE 제약(`uk_role_academy_name`)이 이미 있었으므로, `RoleRepositoryImpl.save()`가 `saveAndFlush()` + `DataIntegrityViolationException` 캐치로 이 제약 위반을 `RoleNameDuplicateException`으로 변환하도록 만들었다 — `workspace` 도메인의 `WorkspacePersistenceAdapter`가 이미 똑같은 문제(워크스페이스 이름 중복)를 이 방식으로 풀어놨어서 그대로 포팅했다. (이 방어책은 최종 전체 리뷰에서 처음엔 빠져있던 걸 발견해서 추가했다 — 개별 태스크 리뷰에선 안 보이다가 전체를 놓고 봤을 때 `workspace`와 비교해서 드러난 케이스.)
- **`permissionCodes`는 `Role` 도메인 모델에 아직 안 넣었다.** 역할 생성 API는 이름·설명만 받고, 권한 조립은 별도 API(`PUT /api/roles/{roleId}/permissions`, 후속 작업)에서 다룬다 — 설계 스펙에서 이미 결정된 사항 그대로.

### 완료 기준

- [x] `RoleController`/`CreateRoleUseCase`/`CreateRoleService`/`RoleRepository`/`RoleRepositoryImpl` 등 구현
- [x] `UserErrorCode` 1건(`ROLE_NAME_DUPLICATE`, `USER_409_1`) + `RoleNameDuplicateException` 추가
- [x] `CreateRoleServiceTest`(이름 중복/정상 생성), `RoleRepositoryImplTest`(DB 유니크 제약 위반 변환) 테스트 작성
- [x] 로컬 curl로 생성 성공(201)/이름 중복(409)/권한 없음(403) 확인
- [x] `./gradlew build` 통과
- [x] `users/docs/{README,API,CHANGELOG,REVISION}.md` 갱신

## ✅ 2026-08-07 · SUPER ADMIN 인증 연결

### 배경

플랫폼 관리자(슈퍼 어드민)가 존재하면 "모든 기능을 쓸 수 있는 계정"을 위해 매 요청마다 역할 조회를 한다는 불필요한 오버헤드가 생겼다. 기존 임시 `is_platform_admin` 불린 컬럼이 있었지만, 학원 관리자(academy-level admin, `admin_scope=ACADEMY`)라는 두 번째 관리자 레벨이 설계에 포함되면서, 단순 불린으로는 "어떤 종류의 관리자인가"를 표현할 수 없게 되었다. 따라서 `account_type`(ADMIN/MEMBER) + `admin_scope`(PLATFORM/ACADEMY, nullable) 이원 구조로 업그레이드하고, JWT 발급/파싱·인증 파이프라인에 이를 전파한다.

### 확정된 정책

- **`is_platform_admin`(boolean)을 `account_type`/`admin_scope` 이원 구조로 대체한 이유**: 팀은 "향후 academy-level admin을 도입할 때 다시 손댈 바뀐다"는 예측 하에 단순 불린으로만 구현했었다. 구조를 미리 `ADMIN/MEMBER` + `PLATFORM/ACADEMY` 조합으로 설계해두면, 나중에 academy-level admin을 활성화할 때 컬럼이나 마이그레이션을 다시 만들 필요 없이 권한 로직만 추가하면 된다 — "안 물어본 유연성을 미리 만들지 않는다"는 `AGENTS.md` 원칙의 반대 방향이지만, 이번에는 "할 줄 알았던 작업이 뒤에 가서 다시 호출될 게 명확하다"는 점에서 미리 준비하기로 팀이 판단했다.

- **`PlatformAdminPermissionPort`를 별도로 만들지 않고 기존 `RolePermissionLookupPort`를 확장하지 않은 이유**: `RolePermissionLookupPort`는 이미 `notice` 모듈이 의존하고 있어서, 이 인터페이스 시그니처를 바꾸면 `notice` 코드가 컴파일 오류가 난다. 따라서 새로운 포트 `PlatformAdminPermissionPort`를 만들어 `JwtAuthenticationConverter`에서 `accountType==ADMIN && adminScope==PLATFORM`일 때만 이 포트를 호출하고, 나머지(`MEMBER` 또는 `ADMIN+ACADEMY`)는 기존 `RolePermissionLookupPort`를 호출하는 방식으로 두 코드 경로를 분리했다 — 이미 있는 인터페이스를 건드리지 않으므로 `notice` 모듈 영향 없다.

- **기존 call site들(`AuthUser`, `User.restore(...)`)의 시그니처를 유지한 방법**: `AuthUser`와 `User.restore()` 생성자에 `accountType`/`adminScope` 두 필드를 추가했으나, 다른 도메인(`workspace`, `attendance`, `calendar`, `memo`, `lecture`, `approval`)의 test/코드가 이미 이 생성자를 호출하고 있어서 컴파일 오류가 날 가능성이 있었다. Java record/class의 delegating constructor(또는 overload) 패턴을 사용해, 기존 생성자는 `accountType=MEMBER`, `adminScope=null`로 기본값을 채우고, 새로운 full 생성자는 모든 필드를 받도록 하여 **이미 있는 호출 사이트는 수정 없이 그대로 동작**하고 새로운 코드(users 도메인의 JWT 발급/파싱)만 새 생성자를 쓰도록 했다.

- **관리자 조직(academy) 및 슈퍼 어드민 계정을 schema migration이 아닌 실제 academy 행 + 수동 SQL INSERT로 만든 이유**: "MUDO 관리자 조직"을 별도 엔티티로 만드는 건 다른 도메인들의 실제 academy 데이터를 건드린다는 뜻이다. migration이면 모든 개발자/배포 환경의 DB에 이 "fake" 행이 생기게 된다 — 로컬에선 테스트 및 수동 검증에 쓰겠지만, staging/production에도 의도치 않게 생긴다. 대신 "실제로 필요한 환경의 DBA가 필요할 때 수동으로 INSERT한다"는 정책으로, schema는 그대로 두고 로컬 개발 편의상 수동 스크립트로 제공했다. 또한 `admin_scope=ACADEMY`는 아직 권한 로직이 미연동 상태라서, 지금 당장 필요하지 않은 컬럼값에 대한 데이터를 환경 전체에 배포할 필요가 없다는 판단도 있었다.

### 완료 기준

- [x] `users.account_type`/`admin_scope` 컬럼 추가(`V5.1.1`)
- [x] JWT 발급(`TokenService.issue/issueAccessToken`) 및 파싱(`JwtTokenProvider.parseAccessToken`) 메서드에 `accountType`/`adminScope` 매개변수 전파
- [x] `JwtAuthenticationConverter.toAuthentication()`에서 `accountType==ADMIN && adminScope==PLATFORM`인 경우, `RolePermissionLookupPort` 대신 `PlatformAdminPermissionPort.allPermissionCodes()` 호출해 `roleName="SUPER_ADMIN"` 고정으로 전체 권한 부여
- [x] `AuthUser`, `User.restore()` 생성자에 새 필드 추가(delegating constructor 패턴으로 기존 호출 사이트 호환성 유지)
- [x] 로컬 DB에 슈퍼 어드민 계정 수동 시드(username=superadmin, account_type=ADMIN, admin_scope=PLATFORM, role_id=NULL)
- [x] 로컬 e2e 검증: 슈퍼 어드민으로 로그인 → JWT가 accountType/adminScope 포함 → 모든 권한이 authorities로 부여됨 확인
- [x] `./gradlew test` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `AccountType` enum (ADMIN/MEMBER) 추가, `AdminScope` enum (PLATFORM/ACADEMY) 추가. `AuthUser` record에 `accountType`/`adminScope` 필드 추가 |
| Persistence | `users.account_type`/`admin_scope` 컬럼 추가(`V5.1.1`), `UserEntity`에 두 필드 추가 |
| Infrastructure | `PlatformAdminPermissionAdapter`/`PlatformAdminPermissionPort` 신규 구현 |
| Security(global) | `JwtClaims`에 `accountType`/`adminScope` 필드 추가, `JwtTokenProvider.createAccessToken/parseAccessToken`에 두 매개변수 추가. `JwtAuthenticationConverter`에 platform admin 체크 로직 추가(`RolePermissionLookupPort` vs `PlatformAdminPermissionPort` 분기) |
| Application(auth) | `TokenService.issue/issueAccessToken` 메서드 시그니처에 두 매개변수 추가 |
| Application(users) | `LoginService`/`RefreshService`에서 `TokenIssuerUseCase` 호출 시 `accountType`/`adminScope` 전달 로직 추가 |
| Domain(users) | `User.restore()` 생성자 확장(delegating constructor 패턴) |

---

## ✅ 2026-08-05 · 권한 카탈로그 조회 + 역할 권한 조립 API 구현 (`GET /api/permissions`, `PUT /api/roles/{roleId}/permissions`, 이슈 #84)

### 배경

역할 생성 API(PR #76)만으로는 권한이 하나도 없는 역할만 만들 수 있어 사실상 못 쓰는 상태였다. `docs/superpowers/specs/2026-08-05-role-permission-assignment-api-design.md`에서 설계한 대로, 권한 카탈로그 조회와 역할 권한 조립을 함께 구현해 "역할다운 역할"을 완성했다.

### 확정된 정책

- **권한은 `id`가 아니라 `code`(`RESOURCE:ACTION` 형태)로 식별한다.** 요청/응답 모두 `permissionCodes`(문자열 Set)를 주고받는다 — `id`는 DB 내부 값이라 환경마다 달라질 수 있지만, `code`는 실제 `@PreAuthorize("hasAuthority(...)")`에 박히는 값과 그대로 대응해서 프론트/백엔드가 같은 문자열을 기준으로 얘기할 수 있다.
- **`RoleRepository.updatePermissions()`를 `save()`와 분리했다.** `save()`는 "새 역할 생성"이라 매번 새 Entity를 빌더로 만드는 연산인데, 권한 조립은 "이미 있는 역할의 관계(`role_permission`)만 바꾸는" 다른 연산이다. `findWithPermissionsById()`로 관리 대상(managed) 엔티티를 로드하고 그 `permissions` Set을 직접 `clear()`+`addAll()`로 바꾸면, 트랜잭션 커밋 시점에 Hibernate dirty-checking이 자동으로 `role_permission` insert/delete를 반영한다 — **명시적 `save()` 호출이 없으므로, 이 메서드를 호출하는 서비스 계층에 반드시 `@Transactional`이 걸려있어야 한다**(`AssignRolePermissionsService`에 적용).
- **역할이 존재하지 않는 경우와 다른 학원 소유인 경우를 동일하게 `RoleNotFoundException`(404)으로 처리한다.** 다른 학원의 역할 존재 여부가 노출되지 않도록 의도적으로 구분하지 않았다.
- **`AssignRolePermissionsRequest.permissionCodes`는 `@NotNull`만 검증하고 `@NotEmpty`는 걸지 않는다.** 역할의 권한을 전부 비우는 요청(빈 배열)을 막을 이유가 없어 허용하기로 했다 — 전체 교체(replace) 방식이라 빈 배열을 보내면 기존 권한이 모두 제거된다.
- **`permission.code` 컬럼 collation을 `utf8mb4_unicode_ci`(대소문자 구분 안 함) → `utf8mb4_bin`(구분함)으로 변경했다(`V4.1.4`).** 기존 collation이면 `notice:read`(소문자)가 실제 저장된 `NOTICE:READ`와 매칭돼 DB 검증(`findAllByCodeIn`)은 통과하지만, 실제 권한 체크(`hasAuthority()`)는 대소문자를 구분하므로 "검증은 통과했는데 실제로는 안 먹히는" 조용한 버그가 생길 수 있었다. 기존 데이터가 전부 대문자라 마이그레이션 자체는 안전하다.

### 완료 기준

- [x] `Permission`/`PermissionRepository`/`PermissionRepositoryImpl`/`PermissionJpaRepository` 구현
- [x] `Role`에 `permissionCodes` 추가, `RoleRepositoryImpl.toDomain()`/`findById`/`updatePermissions` 구현
- [x] `UserErrorCode` 2건(`INVALID_PERMISSION_CODE`, `ROLE_NOT_FOUND`) + `InvalidPermissionCodeException`/`RoleNotFoundException` 추가
- [x] `V4.1.4__change_permission_code_collation.sql` 마이그레이션, 로컬 DB 적용 확인
- [x] `PermissionController`(`GET /api/permissions`), `RoleController`에 `PUT .../permissions` 핸들러 추가
- [x] `AssignRolePermissionsServiceTest`(4가지 분기), `RoleRepositoryImplDataJpaTest`(실제 DB) 작성 및 통과
- [x] 로컬 curl/DB로 end-to-end 검증(카탈로그 조회, 권한 조립 성공/존재하지 않는 코드 400/남의 학원·없는 역할 404)
- [x] `./gradlew build` 통과

---

## 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Presentation | `AuthController`(로그인), `TokenController`(재발급), `RefreshTokenCookieFactory` 신규 |
| Application | `LoginUseCase`/`LoginService`, `RefreshUseCase`/`RefreshService` 신규. `auth` 모듈에 `TokenIssuerUseCase`(발급) 확장, `RefreshTokenValidatorUseCase`(검증) 신규 |
| Domain | `User`(불변 객체, `ensureLoginAllowed()`), `UserStatus`, `UserErrorCode`/`UserException` 신규 |
| Persistence | `UserEntity`(role→role_id), `UserJpaRepository`, `UserRepositoryImpl` 신규. `RoleEntity`/`PermissionEntity`/`RoleJpaRepository` 신규 |
| Infrastructure | `RolePermissionLookupAdapter`(`global.RolePermissionLookupPort` 구현) 신규 |
| Migration | `V4.1.1`(users 테이블 ERD 정합화), `V4.1.2`(role/permission/role_permission 신설, users.role→role_id), `V4.1.3`(users.academy_id → academy FK 연결) |
| Presentation(추가) | `AuthController.logout` 신규 |
| Application(추가) | `LogoutUseCase`/`LogoutService` 신규. `auth` 모듈에 `TokenRevokerUseCase` 신규 |
| 공통(`global`) | `AuthErrorCode`에 리프레시 토큰 실패 코드 2종 추가·`ROLE_CLAIM_MISSING` 제거, `SecurityConfig` CORS 설정 정리, `JwtClaims`/`JwtTokenProvider`/`AuthUser`/`JwtAuthenticationConverter`를 `roleId`/`academyId`/`RolePermissionLookupPort` 기반으로 재작업 |
| Presentation(추가) | `RoleController`(`POST /api/roles`) 신규 |
| Application(추가) | `CreateRoleCommand`/`CreateRoleUseCase`/`CreateRoleService` 신규 |
| Domain(추가) | `Role`(불변 객체), `RoleRepository`, `RoleNameDuplicateException` 신규. `UserErrorCode`에 `ROLE_NAME_DUPLICATE`(`USER_409_1`) 추가 |
| Persistence(추가) | `RoleEntity`에 `@Builder` 생성자 추가, `RoleJpaRepository`에 `existsByAcademyIdAndName` 추가, `RoleRepositoryImpl` 신규(DB 유니크 제약 위반 → `RoleNameDuplicateException` 변환 포함) |
| Presentation(추가) | `PermissionController`(`GET /api/permissions`) 신규, `RoleController`에 `PUT /{roleId}/permissions` 핸들러 추가 |
| Application(추가) | `AssignRolePermissionsCommand`/`AssignRolePermissionsUseCase`/`AssignRolePermissionsService`, `PermissionQueryUseCase`/`PermissionQueryService` 신규 |
| Domain(추가) | `Permission`(불변 레코드), `PermissionRepository` 신규. `Role`에 `permissionCodes` 필드 추가(`restore()` 시그니처 변경, `withPermissionCodes()` 추가). `UserErrorCode`에 `INVALID_PERMISSION_CODE`(`USER_400_1`)/`ROLE_NOT_FOUND`(`USER_404_2`) 추가, `InvalidPermissionCodeException`/`RoleNotFoundException` 신규 |
| Persistence(추가) | `PermissionEntity`에 `@Builder` 생성자 추가, `PermissionJpaRepository`/`PermissionRepositoryImpl` 신규. `RoleRepositoryImpl`에 `findById`/`updatePermissions` 구현, `toDomain()` 갱신 |
| Migration(추가) | `V4.1.4`(`permission.code` collation을 `utf8mb4_bin`으로 변경) |

## 🧪 완료 기준 (전체)

- [x] 로그인 성공/실패 각 코드별 응답 확인
- [x] 액세스 토큰 재발급 성공/실패(4가지 실패 유형) 각 코드별 응답 확인
- [x] `./gradlew test` 통과
- [x] CodeRabbit 리뷰 4건 반영
- [x] role/permission 테이블 신설, `users.role_id` 전환, 매 요청 권한 조회(`hasAuthority`) 기반 구축
- [ ] 계정 발급(회원가입, 원장이 하위 직원 계정 생성) API — 미착수
- [x] 로그아웃 API — `POST /api/auth/logout` 추가, `TokenRevokerUseCase`로 refreshToken 삭제
- [x] 역할 생성 API — `POST /api/roles` 추가, DB 유니크 제약 백스톱 포함
- [x] 권한 카탈로그 조회 API — `GET /api/permissions` 추가
- [x] 역할 권한 조립 API — `PUT /api/roles/{roleId}/permissions` 추가(전체 교체 방식), 존재하지 않는 코드 400/역할 없음·다른 학원 404
- [ ] 역할 수정·삭제, 목록/상세 조회 API — 미착수
- [x] `academy` 테이블 생성됨(다른 팀원, `V2.1.2`~`V2.1.4`) — `role.academy_id`, `users.academy_id` 모두 FK 연결 완료(`V4.1.3`)

## 📌 후속 문서

완료된 변경의 사용자 관점 요약은 [CHANGELOG.md](CHANGELOG.md)에서 확인할 수 있다.
