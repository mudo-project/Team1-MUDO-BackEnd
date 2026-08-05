# users 모듈

## 책임과 범위

계정·권한(로그인, 회원, 역할)을 담당한다. 로그인·액세스 토큰 재발급과, 조립식 권한(역할·권한)의 DB/인증 기반이 구현돼 있다. 역할 생성 API가 구현돼 있다. 역할 수정·삭제, 권한 조립, 목록·상세 조회, 직원 계정 발급 API는 아직 없다(후속 작업).

- **User(사용자)**: 학원 소속 직원(원장/행정/강사/조교) 계정. `academy_id`로 소속 학원을, `role_id`로 역할을 가진다.
- **Role(역할)**: 학원마다 자유롭게 만드는 역할(예: "원장", "행정"). 학원 간 이름이 겹쳐도 무방하다(`academy_id`+`name` 유니크).
- **Permission(권한)**: `RESOURCE:ACTION` 형태의 코드(예: `ROLE:MANAGE`)로 이뤄진 시스템 전체 고정 카탈로그. 학원이 새로 만드는 게 아니라, 각 도메인이 자기 기능을 구현하며 필요한 코드를 추가한다.

## 담당자

be4 (계정·권한)

## 소유하는 주요 데이터와 상태

- `User` — DB 테이블 `users` (원래 approval 도메인이 초기세팅 때 임시로 만든 테이블을 `V4.1.1__align_users_table_with_erd.sql`로 ERD 확정안에 맞게 정합화함)
- `User.status`: `ACTIVE` / `RESIGNED` / `INACTIVE`. `RESIGNED`/`INACTIVE` 상태는 로그인 불가.
- `Role`/`Permission` — DB 테이블 `role`/`permission`/`role_permission`(M:N). `V4.1.2__create_role_permission_tables.sql`로 신설하며 동시에 `users.role`(문자열) 컬럼을 제거하고 `users.role_id`(FK)로 대체했다. **한 유저는 역할을 하나만 가진다**(M:N `user_role` 중간테이블 대신 `users.role_id` 단일 FK) — "겸직" 같은 다중 역할 요구사항이 실제로 확인되기 전까지 단순하게 유지하기로 함.
- 테이블명(`users`, 복수형)과 PK 컬럼명(`id`)은 ERD 컨벤션(단수형 `user`, `user_id`)과 다르다. `approval` 도메인의 `UserNameEntity`가 이미 `users` 테이블을 참조하고 있어 임의로 rename하지 않았다. rename이 필요해지면 `docs/MODULES.md`의 "타 모듈 변경 요청" 절차로 approval 담당자와 협의한다.
- `users.academy_id`는 `academy` 테이블(V2.1.2)보다 먼저 생긴 컬럼이라 FK가 없었다. `V4.1.3__add_academy_fk_to_users.sql`로 `academy.academy_id`를 참조하는 FK(`fk_users_academy`, 기본 RESTRICT)를 추가해 정합성을 DB 레벨에서 보장한다. JPA `@ManyToOne` 매핑은 추가하지 않았다(도메인 간 엔티티 직접 참조 방지, `academyId`는 계속 `Long` 필드로 유지).

## 외부에 공개하는 Application API

인증 (`/api/auth`, `/api/token`):
- `LoginUseCase` — 로그인. 아이디·비밀번호 검증 후 `auth` 모듈의 `TokenIssuerUseCase`를 통해 토큰을 발급한다. accessToken은 응답 바디, refreshToken은 `RefreshTokenCookieFactory`가 만드는 HttpOnly 쿠키로 내려간다.
- `RefreshUseCase` — `POST /api/token/reissue`. 요청의 `refreshToken` HttpOnly 쿠키를 `auth` 모듈의 `RefreshTokenValidatorUseCase`로 검증하고, 검증된 사용자 정보로 `TokenIssuerUseCase.issueAccessToken()`을 호출해 accessToken만 재발급한다. **refreshToken은 로테이션하지 않는다** — 재발급 응답에도 새 refreshToken 쿠키를 내려주지 않고, 기존 쿠키가 만료 전까지 그대로 유지된다.
- `LogoutUseCase` — `POST /api/auth/logout`(인증 필요). `auth` 모듈의 `TokenRevokerUseCase`로 서버에 저장된 refreshToken을 삭제하고, 응답에서 refreshToken 쿠키를 Max-Age=0으로 만료시킨다.

역할 관리 (`/api/roles`, `ROLE:MANAGE` 권한 필요):
- `CreateRoleUseCase` — `POST /api/roles`. 요청자의 `academyId`(JWT)로 역할을 생성한다. 같은 학원 내 이름 중복은 애플리케이션 사전 체크(`USER_409_1`)와 DB `UNIQUE` 제약(`uk_role_academy_name`) 둘 다로 방어한다(동시 요청 대비).
- `PermissionQueryUseCase` — `GET /api/permissions`. 시스템 전체 고정 권한 카탈로그를 그대로 반환한다.
- `AssignRolePermissionsUseCase` — `PUT /api/roles/{roleId}/permissions`. 역할의 권한을 요청받은 `permissionCodes`로 전체 교체한다. 역할이 없거나 다른 학원 소속이면 `USER_404_2`, 존재하지 않는 권한 코드가 섞여 있으면 `USER_400_1`.

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **토큰 발급**: `auth.application.usecase.TokenIssuerUseCase`(auth 모듈이 공개한 계약, 구현체는 `TokenService`)를 호출한다.
- **리프레시 토큰 검증**: `auth.application.usecase.RefreshTokenValidatorUseCase`(구현체는 `TokenService`)를 호출한다. JWT 자체 위조/DB 미존재/DB 불일치를 각각 다른 `AuthErrorCode`로 구분해서 던진다.
- **리프레시 토큰 폐기**: `auth.application.usecase.TokenRevokerUseCase`(구현체는 `TokenService`)를 호출한다. 로그아웃 시 저장된 refreshToken을 삭제한다.
- **비밀번호 검증**: Spring Security `PasswordEncoder`(BCrypt, `global.infrastructure.security.config.SecurityConfig`에 Bean으로 등록됨)를 사용한다.

## 다른 모듈에 제공하는 것

- **역할·권한 조회**: `global.domain.auth.RolePermissionLookupPort`(global이 정의한 계약)를 `users.infrastructure.security.RolePermissionLookupAdapter`가 구현한다. `global`의 `JwtAuthenticationConverter`가 매 요청마다 이 Port로 `roleId` → `{roleName, permissionCodes}`를 조회해 `AuthUser`/Spring Security `authorities`를 구성한다. 다른 도메인은 이 Port를 직접 호출할 일이 없다(인증 흐름 내부에서만 쓰임) — 대신 `@PreAuthorize("hasAuthority('RESOURCE:ACTION')")`로 결과만 사용하면 된다.

## 발행·소비하는 Event

- 현재 없음.

## 변경 시 주의 사항

- 로그인 실패 시 "아이디 또는 비밀번호가 올바르지 않습니다"로 통일해서 응답한다 — 아이디 존재 여부가 노출되지 않도록 하기 위함.
- 계정 발급(회원가입) API는 아직 없다. ERD상 자체 회원가입이 아니라 "원장이 하위 계정 발급" 흐름으로 확정돼 있어, 이후 구현 시 권한 검증(원장만 발급 가능, `ACCOUNT:CREATE` permission)이 필요하다.
- 역할 생성 + 권한 조립 API는 구현됐다. 역할 수정·삭제, 목록·상세 조회는 아직 없다.
- **⚠️ Breaking Change (2026-08-04)**: `users.role`(문자열) 컬럼이 제거되고 `role_id`로 대체됐다. `notice` 도메인의 `UserInfoEntity`가 이 컬럼을 직접 매핑하고 있어 별도 수정이 필요하다(notice 담당자 몫, 이전 `resign_date` 삭제 때와 동일한 패턴).
- JWT의 `roleId`는 재로그인 전까지 예전 값을 유지한다(원장이 역할을 재배정해도 즉시 반영 안 됨). 반면 `roleName`과 permission 목록은 매 요청마다 새로 조회하므로 즉시 반영된다. `academyId`는 계정 소속이 구조적으로 불변이라 이 문제 자체가 없다.

## 세부 문서

- [API.md](API.md) — 엔드포인트별 요청/응답 예시, 검증 규칙, 오류 코드
- [API_FLOW.md](API_FLOW.md) — 계층별 호출 흐름
- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점 변경 요약
