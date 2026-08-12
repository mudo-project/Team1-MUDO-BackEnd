> 작성일: 2026-08-04
> 상태: 🚧 로그인·토큰 재발급·조립식 권한 인증 기반 완료, 로그아웃 API 완료, SUPER ADMIN 인증 연결 완료, 학원 신청/승인 워크플로우(PR 1·2·3/3, "계정 발급 체계" 2단계) 완료, 역할 관리 API 7개(생성/목록/상세/수정/삭제/권한 조립/권한 카탈로그 조회) 완료, 학원 구성원 검색 API 완료, 역할 색상/인원수 완료, CodeRabbit 피드백 반영 + 로깅 컨벤션 전체 도메인(Service 17개) 적용 완료, 직원 계정 발급 API 완료("계정 발급 체계" 3단계 완료), 학원 신청 접수 API 완료(최소 스코프, "계정 발급 체계" 2단계 최종 완결), 학원 신청 접수 시점 requestedLoginId 중복확인 완료, 관리자용 구성원 목록 조회 API 완료(오늘 근태 상태·페이지네이션·역할 필터 포함), 최초 로그인/비밀번호 설정 흐름 재설계 완료(계정 발급 시 연락처·이메일 입력 차단, 임시 비밀번호 평문 응답으로 회귀, 최초 비밀번호 설정 인증 필요화 + 연락처·이메일 필수 등록, 로그인 응답·JWT에 `mustChangePw` 클레임 추가), 계정 발급 권한(ACCOUNT:MANAGE)에 역할 목록/상세 조회 허용 완료, 사용자 역할 변경 API를 구성원 정보 수정 API로 병합 완료 · 후속 작업: 이메일 발송, 사업자등록증 검증(OCR·국세청 API)

## 🎯 변경 목적

계정·권한(users) 도메인을 신설하고, 로그인과 액세스 토큰 재발급을 구현한다. 초기세팅 때 approval 도메인이 참조용으로 임시로 만들어둔 `users` 테이블을 팀이 확정한 ERD에 맞게 정합화하고, 그 위에서 인증 흐름을 짠다.

---

## ✅ 2026-08-12 · 계정 발급 권한(ACCOUNT:MANAGE)에 역할 조회 허용

### 배경

"계정 생성 권한이 있다면 역할 CRUD도 당연히 가능해야 하는 게 자연스럽지 않나?"라는 질문에서 시작된 논의. 확인해보니 `ACCOUNT:MANAGE`(`UserController`)와 `ROLE:MANAGE`(`RoleController`)는 실제로 완전히 분리된 권한이었다. 팀이 이전에 정한 "관련 행위는 하나의 코드로 묶는다" 원칙(예: `ACCOUNT:CREATE`→`ACCOUNT:MANAGE`로 통합)을 계정↔역할처럼 서로 다른 리소스 사이에도 그대로 적용해 하나로 묶자는 제안이 있었으나, 그 원칙은 같은 리소스 안에서의 통합이었을 뿐 리소스가 다른 경우까지 일반화한 적은 없어 그대로 적용하는 것에는 반대했다. 다만 실무적으로 계정 발급(`POST /api/users`) 화면에서 `roleId`를 선택해야 하므로, 역할 목록/상세 조회(읽기 전용)만큼은 `ACCOUNT:MANAGE`로도 열어주는 게 합리적이라는 절충안으로 수렴했다.

### 확정된 정책

- **역할 목록 조회(`GET /api/roles`)와 역할 상세 조회(`GET /api/roles/{roleId}`)만 `ACCOUNT:MANAGE`에도 허용한다.** `@PreAuthorize("hasAuthority('ROLE:MANAGE') or hasAuthority('ACCOUNT:MANAGE')")`로 변경했다.
- **역할 생성·수정·삭제·권한 조립(쓰기 작업 4개)은 그대로 `ROLE:MANAGE` 전용으로 남긴다.** 권한 조립은 보안 민감도가 높아 계정 발급 권한만으로 손댈 수 있게 하지 않는다.
- **권한 코드 자체는 통합하지 않는다.** `ACCOUNT:MANAGE`/`ROLE:MANAGE`는 카탈로그에 여전히 별도 코드로 남아있고, 이번 변경은 컨트롤러의 `@PreAuthorize` 식에 `or` 조건을 추가한 것뿐이다 — 역할에 권한을 배정할 때 두 코드를 항상 같이 묶어 배정해야 하는 정책 변화는 아니다.

### 완료 기준

- [x] `RoleController.list()`/`RoleController.get()`의 `@PreAuthorize`에 `ACCOUNT:MANAGE` 조건 추가
- [x] `./gradlew compileJava` 통과
- [x] 로컬 curl e2e: `ACCOUNT:MANAGE`만 가진 테스트 역할(`ROLE:MANAGE` 없음)을 만들어 그 역할의 계정으로 로그인 → 목록/상세 조회 200 확인 → 역할 생성/수정/권한 조립/삭제 4개 모두 403 확인
- [x] 문서 갱신(API.md/README.md/CHANGELOG.md/REVISION.md/Notion)

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Presentation(users) | `RoleController.list()`/`RoleController.get()`의 `@PreAuthorize`를 `ROLE:MANAGE` 단독에서 `ROLE:MANAGE or ACCOUNT:MANAGE`로 변경(Javadoc/`@Operation` 설명 갱신) |
| Migration | 없음 |

---

## ✅ 2026-08-12 · 구성원 역할 변경을 구성원 정보 수정 API로 병합

### 배경

프론트에서 관리자용 "구성원 정보 수정" 화면과 "역할 변경"을 하나의 저장 액션으로 처리하고 싶다는 요청이 들어왔다. 기존에는 `PATCH /api/users/{userId}`(이름/연락처/이메일/입사일)와 `PATCH /api/users/{userId}/role`(역할)이 완전히 분리된 API였다(2026-08-08 "사용자 역할 변경 추가"에서 별도 API로 만들었고, README에는 "역할 변경에는 role 존재 검증이 별도로 필요해서 책임을 분리했다"고 근거를 남겨뒀었다).

### 확정된 정책

- **`UpdateMemberProfileRequest`에 선택 필드 `roleId`를 추가한다.** 값을 보내지 않으면 기존 역할을 유지하고(다른 필드들과 동일한 부분 수정 패턴), 값을 보내면 역할도 함께 바뀐다.
- **역할 존재 검증을 프로필 필드 갱신보다 먼저 수행한다.** `UpdateUserProfileService.updateMemberProfile()`이 `RoleRepository`를 새로 의존성으로 받아, `roleId != null`이면 `roleRepository.findById()`로 먼저 확인하고(없으면 `RoleNotFoundException`, `404 USER_404_2`) `userRepository.changeRole()`을 호출한 뒤에야 `userRepository.updateProfile()`을 호출한다. 이 순서 덕분에 잘못된 `roleId`를 보내면 이름·연락처 등 다른 필드도 전혀 반영되지 않는다(로컬 e2e로 확인: 잘못된 roleId로 이름도 같이 보냈을 때 이름이 안 바뀐 채 404만 응답).
- **역할 변경 관련 로직·검증(대상 MEMBER 여부, 역할 존재 확인, `changeRole` 리포지토리 호출)은 기존 `ChangeUserRoleService`의 패턴을 그대로 옮겨왔다.** `UserRepository.changeRole`/`UserRepositoryImpl.changeRole`/`UserEntity.changeRole`(FK 위반 시 `RoleNotFoundException` 변환 포함)은 이미 검증된 코드라 그대로 재사용하고, 그 위의 오케스트레이션 계층(`ChangeUserRoleCommand`/`ChangeUserRoleUseCase`/`ChangeUserRoleService`/`ChangeUserRoleRequest`)만 삭제했다.
- **`PATCH /api/users/{userId}/role` 엔드포인트와 관련 클래스를 전부 삭제한다.** `UserController`에서 핸들러·필드·import를 제거했고, 그 경로로 호출하면 이제 매핑된 컨트롤러가 없어 컨트롤러 자체를 안 탄다. (기존에 발견한 별개 이슈: 이 앱은 매핑되지 않은 경로 전체에 대해 404 대신 500을 반환한다 — `NoResourceFoundException`을 잡는 catch-all `GlobalExceptionHandler`가 원인으로 보이며, 이번 병합과 무관한 기존 버그라 별도로 남겨뒀다.)
- **`changeRole`이 다른 곳에서 안 쓰이는지 확인 후 삭제했다.** 코드베이스 전체에서 `ChangeUserRoleUseCase`/`ChangeUserRoleCommand`/`ChangeUserRoleService`/`ChangeUserRoleRequest`를 참조하는 곳은 자기 자신의 호출 체인(`UserController` → `ChangeUserRoleService` → `UserRepository.changeRole`)뿐이었다.

### 완료 기준

- [x] `UpdateMemberProfileUseCase`/`UpdateMemberProfileRequest`에 선택 필드 `roleId` 추가
- [x] `UpdateUserProfileService`에 `RoleRepository` 의존성 추가, 역할 검증→변경→프로필 갱신 순서로 구현(TDD: 역할 미지정 시 변경 없음/역할 지정 시 변경/잘못된 역할 시 `RoleNotFoundException`+아무 것도 반영 안 됨 3케이스 추가)
- [x] `UserController`에서 `changeRole` 핸들러·`ChangeUserRoleUseCase` 필드·관련 import 제거, `updateMemberProfile` 핸들러가 `roleId`를 전달하도록 수정
- [x] `ChangeUserRoleCommand`/`ChangeUserRoleUseCase`/`ChangeUserRoleService`/`ChangeUserRoleRequest`/`ChangeUserRoleServiceTest` 삭제(다른 참조 없음 확인)
- [x] `./gradlew test` 전체 통과
- [x] 로컬 curl e2e: 이름/연락처만 수정(역할 유지) → 역할만 수정(다른 필드 유지) → 이름+역할 동시 수정 → 잘못된 roleId로 이름+역할 동시 수정 시 404 확인 및 이름이 반영되지 않았음을 재조회로 확인 → 옛 `/role` 엔드포인트가 더 이상 라우팅되지 않음을 확인
- [x] 문서 갱신(API.md/README.md/CHANGELOG.md/REVISION.md/Notion)

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Application(users) | `UpdateMemberProfileUseCase`(`roleId` 파라미터 추가), `UpdateUserProfileService`(`RoleRepository` 의존성 추가, 역할 변경 로직 흡수) |
| Presentation(users) | `UpdateMemberProfileRequest`(`roleId` 필드 추가), `UserController`(`changeRole` 핸들러 제거, `updateMemberProfile` 핸들러 갱신) |
| 삭제 | `ChangeUserRoleCommand`/`ChangeUserRoleUseCase`/`ChangeUserRoleService`/`ChangeUserRoleRequest`/`ChangeUserRoleServiceTest`, `PATCH /api/users/{userId}/role` 엔드포인트 |
| Migration | 없음(기존 컬럼·제약 재사용) |

---

## ✅ 2026-08-12 · 최초 로그인/비밀번호 설정 흐름 재설계

### 배경

계정 발급 시 `phone`/`email`을 선택 입력으로 받을 수 있게 한 뒤(`V4.1.7`, 위 "계정생성 phone/email 선택입력화" 참고), "그럼 최초 로그인 시 비밀번호를 바꾸면서 연락처·이메일도 같이 받을 수 있나?"라는 질문에서 시작해 다음 문제들을 한 번에 다시 들여다봤다.

1. 임시 비밀번호로 로그인한 뒤 앱을 그냥 꺼버리거나 재설치하면, `mustChangePw=true`라는 신호를 프론트가 다시 확인할 방법이 없었다 — 로그인 API 응답에 이 값이 아예 없었기 때문이다.
2. `POST /api/users/password-setup`은 비밀번호 설정 링크(`PasswordSetupLinkBuilder`가 만든 URL, `?username=...&tempPassword=...`)에 아이디와 임시 비밀번호를 쿼리스트링으로 실어 보내고 있었다. 이는 (a) 브라우저 히스토리에 평문으로 남고, (b) 서버·프록시·CDN 접근 로그가 기본적으로 요청 URL 전체(쿼리스트링 포함)를 기록하며, (c) 같은 페이지에서 외부 리소스를 로드하면 `Referer` 헤더로 제3자에게 전달될 수 있는 실제 보안 안티패턴(OWASP에 문서화됨)이다. 이 방식은 PR #314(2026-08-10)에서 "평문 비밀번호가 API 응답과 로그에 남는 게 마음에 걸린다"는 이유로 기존 `temporaryPassword` 평문 응답 방식을 대체하며 도입됐는데, 재검토 결과 원래 우려(앱 레벨 응답 로깅)는 그대로 남아있으면서 새로운 노출 경로(브라우저 히스토리/접근 로그/Referer)만 추가된, 더 나빠진 변경이었다.
3. `POST /api/users/password-setup`이 익명 공개 엔드포인트라 `username`/`tempPassword`를 요청 바디로 받아야 했다.

### 확정된 정책

- **계정 발급 시 연락처·이메일 입력을 완전히 막는다.** `CreateAccountRequest`/`CreateAccountCommand`/`User.create()`에서 `phone`/`email` 파라미터를 아예 제거했다(선택 입력에서 "받지 않음"으로 강화). 전역 Jackson 설정(`FAIL_ON_UNKNOWN_PROPERTIES`)이 켜져 있어, 요청 바디에 `phone`/`email`을 넣어도 알 수 없는 필드로 처리되어 `400`으로 거절된다(로컬 e2e로 확인). 원장이 직원 전체의 연락처를 대신 입력·관리하지 않고, 본인이 최초 비밀번호 설정에서 등록하도록 강제하는 쪽으로 정책을 굳혔다.
- **비밀번호 설정 링크 방식을 폐기하고 평문 임시 비밀번호 응답으로 되돌린다.** `PasswordSetupLinkBuilder`(+ 테스트, `app.frontend-url` 설정)를 완전히 삭제했다. `AccountIssuer.issue()`가 `phone`/`email` 파라미터도 함께 제거하고 `IssuedAccount(user, temporaryPassword)`를 반환하도록 되돌렸다(`CreateAccountResult`/`AccountCreateResponse`의 `passwordSetupLink` 필드도 `temporaryPassword`로 되돌림). JSON 응답 필드는 요청 URL이 아니므로 브라우저 히스토리·접근 로그·Referer 노출 경로가 없다 — 원래 PR #314가 우려했던 "응답에 평문이 남는다"는 지점은 이 설계에서도 동일하게 존재하지만, 그 우려는 URL이든 JSON 바디든 앱 레벨 응답 로깅 정책으로 별도 해결해야 하는 문제이고 지금 스코프에는 없다고 판단했다.
- **로그인 응답과 액세스 토큰(JWT) 양쪽에 `mustChangePw`를 싣는다.** `JwtClaims`/`JwtTokenProvider`/`TokenIssuerUseCase`/`TokenService`/`AuthUser`/`JwtAuthenticationConverter`/`LoginService`/`RefreshService` 전체에 `mustChangePw`를 관통시켰다(레거시 토큰엔 클레임이 없으므로 `JwtTokenProvider.parseAccessToken`은 누락 시 `false`로 안전하게 기본값 처리). `AuthUser`는 기존 4-arg 편의 생성자(21개 크로스 도메인 테스트가 의존)를 건드리지 않고 `mustChangePw=false` 기본값을 주는 방식으로 확장해, 무관한 테스트 파일들을 손대지 않았다. 로그인 응답 바디는 로그인 시점 1회성 신호이고, 이후 새로고침 등으로 다시 확인해야 하면 프론트가 로컬에 저장한 JWT를 디코드해서 같은 클레임을 읽으면 된다. `mustChangePw`는 여전히 백엔드가 다른 API 호출을 막는 로그인 게이트로 쓰지 않는다(`LoginService`/`User.ensureLoginAllowed()`는 `status`만 확인) — 순수하게 프론트 화면 전환용 신호다.
- **`POST /api/users/password-setup`을 인증 필요 엔드포인트로 바꾸고, 새 비밀번호와 함께 이메일·전화번호를 필수로 받는다.** 요청 바디는 `{newPassword, email, phone}`만 받고 `username`/임시 비밀번호는 받지 않는다 — 대상 계정은 `@AuthenticationPrincipal AuthUser`(JWT)에서 식별한다. `SecurityConfig`의 `permitAll` 매처를 제거했다. 서비스 내부에서는 JWT의 `mustChangePw` 클레임을 그대로 믿지 않고 `userRepository.findById()`로 최신 DB 상태를 다시 조회해 `mustChangePw==true`인지 재확인한다(클레임은 로그인 시점 값이라 그 사이 이미 설정을 마쳤을 수 있음). 비밀번호·연락처·이메일 갱신은 `UserJpaRepository.completePasswordSetupIfMustChange`(기존 CR-Fix에서 도입한 `WHERE mustChangePw=true` 조건부 원자적 UPDATE)를 확장해 한 문장으로 처리한다 — 부분 반영(비밀번호만 바뀌고 연락처는 안 바뀌는 등)이 구조적으로 불가능하다.
- **엔드포인트 자체가 인증을 요구하게 됐지만, "이미 설정 완료" 재요청에 대한 마스킹 정책은 그대로 유지한다.** 처음 이 흐름을 재검토할 때 "임시 비밀번호로 로그인하면 '이미 재설정된 비밀번호입니다'라고 구체적으로 알려줄 수 있나?"라는 요청이 있었으나, 익명 호출자가 이 구분을 이용해 계정 상태(존재 여부·설정 완료 여부)를 추론할 수 있다는 점을 근거로 반대했고 사용자가 이를 받아들여 제외했다. 인증이 추가된 지금은 호출자가 이미 그 계정의 소유자로 확인된 상태라 엄밀히는 같은 위험이 없지만, 재요청 시 "이미 처리됨"과 그 외 실패를 구분하지 않는 기존 응답(`400 USER_400_2`)을 그대로 유지하기로 했다 — 새로운 위험을 만들지 않는 선에서 기존 동작을 최소한으로만 바꾼다는 원칙을 따랐다.
- **이메일 중복은 여전히 `409 USER_409_7`로 별도 처리한다.** `UserRepositoryImpl.completePasswordSetup`이 `DataIntegrityViolationException`을 잡아 `uk_users_email` 위반이면 `EmailDuplicateException`으로 변환한다(내 정보 수정/구성원 정보 수정과 동일한 패턴). 이건 익명 열거 문제가 아니라 인증된 본인이 스스로 잘못된 값을 넣은 경우라 구체적인 오류를 그대로 보여준다.

### 검토했다가 제외한 대안

- **랜덤 토큰 발급 방식(별도 토큰 테이블)**: `refresh_tokens` 테이블과 같은 "도메인 모델 없는 infra-only 테이블, 1인 1토큰 upsert" 패턴을 그대로 재사용할 수 있어 구현 난이도는 낮다고 판단했으나, 결국 별도 저장소·만료 로직·발급 API가 추가로 필요해 지금 스코프에 비해 과했다. JWT의 `mustChangePw` 클레임 + 인증된 password-setup 엔드포인트만으로 원래 문제(재확인 불가능, URL 노출)를 모두 해결할 수 있어 채택하지 않았다.
- **`temporaryPassword`를 계정 발급 응답으로, 최초 로그인 여부는 액세스 토큰에 담는 방식**: 최종 채택한 설계와 거의 같은 방향이었으나, `password-setup` 엔드포인트를 아예 없애고 `PATCH /api/users/me/password`(내 비밀번호 변경)를 재사용하자는 제안이 있었다. 사용자가 "최초 설정과 평소 비밀번호 변경은 책임이 다르다"는 이유로 반대해, `password-setup`은 별도 엔드포인트로 유지하고 대신 페이로드만 이메일·전화번호까지 받도록 확장했다.

### 완료 기준

- [x] JWT 체인 전체에 `mustChangePw` 클레임 추가(`JwtClaims`/`JwtTokenProvider`/`TokenIssuerUseCase`/`TokenService`/`AuthUser`/`JwtAuthenticationConverter`/`LoginService`/`RefreshService`, `LoginResult`/`LoginResponse`에 반영), 관련 테스트 전부 TDD로 갱신(신규 `LoginServiceTest` 포함)
- [x] `CreateAccountRequest`/`CreateAccountCommand`/`User.create()`에서 `phone`/`email` 제거, 요청에 포함 시 `400`으로 거절되는지 로컬 e2e로 확인
- [x] `AccountIssuer`/`IssuedAccount`/`CreateAccountResult`/`AccountCreateResponse`를 `temporaryPassword` 반환으로 되돌림, `PasswordSetupLinkBuilder`+테스트+`app.frontend-url` 설정(로컬/운영/테스트 3곳) 삭제
- [x] `PasswordSetupRequest`/`PasswordSetupCommand`/`PasswordSetupService`를 인증 기반(`userId`로 조회) + `{newPassword, email, phone}` 필수 입력으로 재작성(TDD)
- [x] `UserJpaRepository.completePasswordSetupIfMustChange` 확장(비밀번호+연락처+이메일 동시 갱신), `UserRepositoryImpl.completePasswordSetup`에 이메일 중복 → `EmailDuplicateException` 변환 추가(TDD)
- [x] `UserController`의 `password-setup` 핸들러에 `@AuthenticationPrincipal AuthUser` 추가, `SecurityConfig`에서 `permitAll` 매처 제거
- [x] `./gradlew test` 전체 통과(환경 이슈로 `Java heap space` 1회 발생 후 재시도 성공 — IntelliJ+Gradle 데몬+구동 중인 앱 서버가 동시에 메모리를 점유한 환경 문제, 코드 결함 아님)
- [x] 로컬 curl e2e: 계정 발급(연락처·이메일 미입력 확인, 포함 시 400 확인) → 로그인(`mustChangePw=true` 응답·JWT 클레임 확인) → 인증 없이 password-setup 호출 시 401 확인 → 이메일 누락 시 400 확인 → 정상 설정(이메일 중복 시 409 확인 후 유니크 값으로 재시도, 204) → 재로그인(`mustChangePw=false` 확인, DB에 연락처·이메일 반영 확인) → 옛 임시 비밀번호 로그인 실패(401) → 동일 계정 재설정 시도 시 400 확인
- [x] Swagger(OpenAPI) 문서로 `AccountCreateResponse`/`PasswordSetupRequest`/`CreateAccountRequest`/`LoginResponse` 스키마 재검증
- [x] 문서 갱신(API.md/API_FLOW.md/README.md/CHANGELOG.md/REVISION.md)

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `User.create()`에서 `phone`/`email` 파라미터 제거(내부적으로 `null` 고정, `restore()`는 영향 없음) |
| Application(users) | `CreateAccountCommand`(`phone`/`email` 제거), `AccountIssuer`/`IssuedAccount`(`temporaryPassword` 필드로 복귀), `CreateAccountResult`(동일), `PasswordSetupCommand`(`{userId, newPassword, email, phone}`로 전면 교체), `PasswordSetupService`(인증 기반 재작성), `LoginResult`(신규), `LoginService`/`RefreshService`(`mustChangePw` 전파) |
| Persistence(users) | `UserJpaRepository.completePasswordSetupIfMustChange`(비밀번호+연락처+이메일 동시 갱신으로 확장), `UserRepositoryImpl.completePasswordSetup`(이메일 중복 예외 변환 추가) |
| Presentation(users) | `CreateAccountRequest`(`phone`/`email` 제거), `AccountCreateResponse`(`temporaryPassword`로 복귀), `PasswordSetupRequest`(`{newPassword, email, phone}`로 전면 교체), `UserController.setupPassword`(`@AuthenticationPrincipal AuthUser` 추가), `LoginResponse`(`mustChangePw` 추가) |
| 공통(`global`) | `JwtClaims`/`JwtTokenProvider`/`AuthUser`/`JwtAuthenticationConverter`에 `mustChangePw` 추가, `SecurityConfig`에서 `POST /api/users/password-setup`의 `permitAll` 매처 제거 |
| 공통(`auth`) | `TokenIssuerUseCase`/`TokenService`의 `issue()`/`issueAccessToken()`에 `mustChangePw` 파라미터 추가 |
| 삭제 | `PasswordSetupLinkBuilder`+테스트, `app.frontend-url`(로컬/운영/테스트 설정 3곳), 이제 무의미해진 `CreateAccountRequestTest`(email 검증 테스트) |
| Migration | 없음(기존 컬럼·제약 재사용, 신규 컬럼 추가 없음) |

### 후속 작업

- 임시 비밀번호·아이디를 학원 관리자가 직원에게 전달하는 과정의 자동화(이메일/카카오톡 발송 연동)는 여전히 범위 밖이다.
- 응답 바디에 평문 임시 비밀번호가 담기는 것 자체를 앱 레벨 로깅에서 마스킹할지는 별도 검토가 필요하다(access log에 응답 바디를 남기는 설정이 있는지 확인 필요) — 이번 작업은 URL 노출 경로만 제거했다.

---

## ✅ 2026-08-12 · 프로필 수정 동시성 방어 + 이메일 형식 검증 (CodeRabbit 피드백 반영)

### 배경

PR #374(구성원 정보 수정) 리뷰에서 CodeRabbit이 남긴 미해결 지적 3건을 반영했다: (1) `UpdateUserProfileService`가 조회한 기존 값으로 모든 프로필 필드를 다시 저장하는 방식이라 동시 수정 시 필드 유실 위험이 있음, (2) 프로필/계정발급 요청의 `email` 필드가 `@Size`만 있고 `@Email`이 없어 형식이 안 맞는 값도 저장됨, (3) 테스트가 `UserException` 타입만 검증하고 구체적 에러코드는 검증하지 않음.

### 확정된 정책

- **동시성 방어**: `UserEntity`에 `@Version`(JPA 낙관적 락)을 추가했다(`V4.1.9` 마이그레이션, `users.version BIGINT NOT NULL DEFAULT 0`). `UserRepositoryImpl.updateProfile`이 `flush()` 중 `OptimisticLockingFailureException`을 받으면 `ProfileUpdateConflictException`(`409 USER_409_8`)으로 변환한다. `shared_file_root`(`SharedFileRootEntity`)에 이미 있던 `@Version` 패턴을 그대로 따랐다.
- **이메일 형식 검증**: `UpdateMemberProfileRequest`/`UpdateMyProfileRequest`/`CreateAccountRequest`의 `email` 필드에 `@Email`을 추가했다. `null`은 부분 수정 의미로 계속 허용되고(Bean Validation은 null을 유효한 값으로 취급), 형식이 안 맞는 비어있지 않은 값만 `400 COMMON_400_1`로 거절된다.
- **테스트 보강**: `UpdateUserProfileServiceTest`의 두 실패 케이스가 `.extracting(e -> ((UserException) e).getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND)`로 구체적 에러코드까지 검증하도록 바꿨고, `findById`가 `Optional.empty()`를 반환하는(대상이 진짜로 존재하지 않는) 케이스를 `updateMyProfile`/`updateMemberProfile` 양쪽에 추가했다.
- **범위 밖**: PR #367에서 CodeRabbit이 지적했던 나머지 1건은 이미 해당 PR 안에서 해결된 상태였다(코멘트에 "Addressed in commit decb792" 표시 확인). PR #371/#372/#373/#375에는 CodeRabbit 리뷰가 rate limit으로 아예 실행되지 않았고, 이미 머지된 PR이라 `@coderabbitai review`로도 재실행이 안 된다("This command is applicable only when automatic reviews are paused") — 재검토가 필요하면 해당 코드를 건드리는 새 PR을 열어야 리뷰가 트리거된다.

### 완료 기준

- [x] `UserEntity.version`(`@Version`) + `V4.1.9` 마이그레이션
- [x] `ProfileUpdateConflictException`/`UserErrorCode.PROFILE_UPDATE_CONFLICT`(`USER_409_8`) + `UserRepositoryImpl.updateProfile` 예외 변환(TDD)
- [x] `UpdateMemberProfileRequest`/`UpdateMyProfileRequest`/`CreateAccountRequest`에 `@Email` 추가(TDD)
- [x] `UpdateUserProfileServiceTest` 에러코드 구체화 + `Optional.empty()` 케이스 추가
- [x] `./gradlew build` 전체 통과
- [x] 문서 갱신(API.md/CHANGELOG.md/REVISION.md)

---

## ✅ 2026-08-12 · 구성원 목록 조회 번호 기반 페이지네이션

### 배경

`GET /api/users/members`가 공용 `SliceResponse`(`content`/`page`/`size`/`hasNext`)를 쓰고 있어서, 프론트가 "다음 페이지 있는지"만 알 수 있고 전체 페이지 수를 몰라 "1 2 3 4" 번호 버튼 UI를 그릴 수 없었다. 사용자가 로컬에 갖고 있던 이전 프로젝트(`module03-gymjjak`)의 `PageResponse`(`totalElements`/`totalPages`/`first`/`last`/`hasPrevious`) 패턴을 참고해서 반영했다.

### 확정된 정책

- **인메모리 유지, DB 레벨 전환은 안 함**: 이 API는 이미 전체 구성원을 메모리에 올린 뒤 필터·정렬·슬라이스하는 방식이라(`ListMembersService`), `totalElements`는 그 필터링된 리스트의 `size()`를 그대로 쓰면 돼서 추가 DB 조회 비용이 없다. 아래 "구성원 목록 조회 페이지네이션·역할 필터" 절에서 DB 레벨 페이지네이션(Pageable/LIMIT-OFFSET)을 보류하기로 한 결정은 이번에도 그대로 유지한다 — gymjjak처럼 `Pageable`+`@Query(countQuery=...)`로 가는 전환은 하지 않았다.
- **공용 `PageResult`/`SliceResponse`는 건드리지 않음**: 이 둘은 lecture/attendance/approval/notice/student/workspace 등 9개 이상의 다른 도메인이 함께 쓰는 공용 컴포넌트라, 필드를 추가하면 모든 소비처의 생성자 호출이 깨진다(이번 세션에서 겪은 `AuthUser` 5-args 생성자 변경과 같은 종류의 리스크). 대신 users 도메인 안에 `MemberPage`(application 결과 레코드)와 `MemberPageResponse`(presentation DTO, gymjjak `PageResponse`와 필드 순서까지 동일: `content`/`page`/`size`/`totalElements`/`totalPages`/`first`/`last`/`hasNext`/`hasPrevious`)를 새로 만들어 이 엔드포인트에만 적용했다.
- **`ListMembersUseCase.list(...)`의 반환 타입**을 `PageResult<MemberListItem>`에서 `MemberPage`로 바꿨다 — users 도메인 내부(usecase/service/controller/테스트)에만 영향이 있고, 다른 도메인 컴파일에는 영향이 없다.
- Swagger `@Operation` description에 남아있던 "같은 학원 소속 구성원 전체" 문구도 이번에 발견해서 제거했다(Phase 2에서 놓친 잔여 참조).

### 완료 기준

- [x] `MemberPage`(TDD: totalElements/totalPages 계산 검증, `ListMembersServiceTest`)
- [x] `ListMembersUseCase`/`ListMembersService` 반환 타입 교체
- [x] `MemberPageResponse` + `UserController.listMembers` 반영
- [x] 로컬 e2e: Swagger UI에서 `GET /api/users/members?page=0&size=2` 호출해 `totalElements=12`/`totalPages=6`/`first`/`last`/`hasPrevious` 확인
- [x] `./gradlew build` 전체 통과(1150+ 테스트)
- [x] 문서 갱신(API.md/README.md/CHANGELOG.md/REVISION.md/Notion)

---

## ✅ 2026-08-11 · academyId 스코핑 제거 (Phase 2)

### 배경

실제 운영 배포 모델이 "학원마다 별도 EC2 프로세스 + 별도 RDS 스키마"로 확정되면서(Phase 1의 학원 신청/승인 기능 폐기와 같은 배경), 이미 각 프로세스가 정확히 하나의 학원 데이터만 볼 수 있는 구조인데도 `users`/`role` 도메인과 JWT 인증 체인 전체가 여전히 `academyId`로 앱 레벨 스코핑을 하고 있었다. 상세 설계는 `docs/superpowers/specs/2026-08-11-academy-removal-design.md`의 Phase 2 섹션 참고.

### 확정된 정책

- **JWT/인증 체인**: `JwtClaims`/`JwtTokenProvider`/`AuthUser`/`JwtAuthenticationConverter`, `auth` 도메인의 `TokenIssuerUseCase`/`TokenService`에서 `academyId`를 완전히 제거했다. 새로 발급되는 토큰에는 `academyId` 클레임이 없다. `PLATFORM:SUPER_ADMIN` 권한 부여 로직은 그대로 유지했다.
- **`User`/`Role` 도메인 모델**: `academyId` 필드/생성자 파라미터를 제거했다. `UserRepository`/`RoleRepository`의 academyId 기반 메서드를 academyId 없는 버전으로 교체했다(`searchByAcademyId`→`search`, `findAllByAcademyId`→`findAll`, `existsByAcademyIdAndName`→`existsByName` 등).
- **크로스-BC 예외**: `UserRepository.findActiveUserIds(Long academyId, Set<Long> userIds)`는 시그니처를 그대로 유지했다 — messenger(`ChatMemberDirectoryPortAdapter`)와 workspace(`WorkspaceMemberDirectoryAdapter`/`AddWorkspaceMembersService`)가 이 포트를 academyId 인자와 함께 호출하고 있어서, 시그니처를 바꾸면 두 도메인의 컴파일이 깨진다. 내부 구현(`UserRepositoryImpl`)에서는 academyId 파라미터를 받아만 두고 실제 필터링에는 쓰지 않는다.
- **workspace의 다른 두 지점**: `AuthUser.academyId()` 접근자를 지우면서 workspace의 `AddWorkspaceMembersRequest`/`CreateWorkspaceRequest`가 `authUser.academyId()`를 호출하던 부분이 컴파일이 깨졌다 — 이 두 파일은 이미 죽은 파라미터를 전달만 하고 있었고(workspace 자체 CHANGELOG에 그렇게 기록돼 있음), 로직 변경 없이 `null`을 넘기도록 최소한으로 고쳤다. `AddWorkspaceMembersCommand`/`CreateWorkspaceCommand`/`WorkspaceService`/`AddWorkspaceMembersService` 등 workspace의 나머지 파일은 건드리지 않았다 — 그쪽 정리는 workspace 담당자의 후속 작업으로 남긴다.
- **테스트 전반**: `AuthUser`의 5-args 편의 생성자가 academyId를 받던 것을 없애면서, attendance/calendar/dataimport/google/memo/timetable/workspace 등 7개 도메인의 컨트롤러 테스트 약 19개가 `new AuthUser(...)` 호출에서 인자 1개를 제거하는 기계적 수정이 필요했다(로직 변경 없음).
- **DB 마이그레이션(`V4.1.8`)**: `users.academy_id`/`role.academy_id` 컬럼은 이번에 DROP하지 않고 nullable로만 바꿨다. messenger의 `ChatMemberInfoEntity`가 여전히 `users` 테이블의 `academy_id`에 매핑된 shim이라, 지금 컬럼을 지우면 messenger가 즉시 깨진다. `role`의 유니크 제약은 `(academy_id, name)`에서 `name` 단일 컬럼(`uk_role_name`)으로 바꿨다. 컬럼 실제 DROP은 messenger 쪽 shim 정리가 끝난 뒤 별도 후속 마이그레이션에서 진행한다.
- **범위 밖(명시적으로 보류)**: `file_metadata.academy_id`, messenger의 `ChatMemberInfoEntity`/`ChatMemberInfo`/`ChatMemberDirectoryPortAdapter` 정리는 이 작업에 포함하지 않고 messenger 담당자에게 크로스 도메인 요청으로 전달한다.

### 완료 기준

- [x] `JwtClaims`/`JwtTokenProvider`/`AuthUser`/`JwtAuthenticationConverter`/`TokenIssuerUseCase`/`TokenService`에서 academyId 제거
- [x] `User`/`Role` 도메인 모델 및 `UserRepository`/`RoleRepository`/구현체/서비스/컨트롤러/요청 DTO 전체에서 academyId 제거
- [x] `V4.1.8` 마이그레이션(컬럼 nullable화 + role 유니크 제약 변경), messenger shim과의 공존 확인
- [x] 로컬 e2e: 로그인 후 JWT에 academyId 클레임 없음 확인, 내 정보 조회/구성원 목록/검색/역할 목록 정상 동작 확인
- [x] `./gradlew build` 전체 통과(1150+ 테스트)
- [ ] messenger 담당자에게 `ChatMemberInfoEntity` shim 정리 요청 전달
- [ ] workspace 담당자에게 `AddWorkspaceMembersCommand`/`CreateWorkspaceCommand`의 죽은 academyId 파라미터 정리 요청 전달(선택, 급하지 않음)
- [ ] messenger/workspace 정리가 끝난 뒤 `users.academy_id`/`role.academy_id` 컬럼 실제 DROP (별도 후속 작업)

---

## ✅ 2026-08-11 · 구성원 재직 상태 변경 추가

### 배경

#361(내 정보 조회, #367)·#362(내 정보 수정, #371)·#363(내 비밀번호 변경, #372)·#364(구성원 상세 조회, #373)·#365(구성원 정보 수정, #374)에 이어지는 마지막 세부 작업(#366). 구성원의 퇴사/복직 처리를 API로 할 수 없었다.

### 확정된 정책

- `ACTIVE`/`RESIGNED`/`INACTIVE` 양방향 전환을 지원한다 — 퇴사 처리(`RESIGNED`) 이후 복직 시 다시 `ACTIVE`로 되돌리는 것도 같은 API로 가능하다. 단방향 상태 머신으로 제한할 이유가 없다고 판단했다.
- 대상 검증은 구성원 상세조회/정보수정과 동일한 `findById → academyId 필터 → accountType == MEMBER 필터` 패턴을 따랐다.

### 완료 기준

- [x] `UserRepository.changeStatus` + `UserEntity.changeStatus` 뮤테이터(TDD)
- [x] `ChangeUserStatusUseCase`/`ChangeUserStatusService` 구현(TDD)
- [x] `UserController`에 `PATCH /api/users/{userId}/status` 반영
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 없음 — #353(구성원 상세조회/수정/재직상태변경/비밀번호변경)의 마지막 세부 작업으로 이 PR을 끝으로 6개 API 분할이 모두 완료된다.

---

## ✅ 2026-08-11 · 구성원 정보 수정 추가

### 배경

#361(내 정보 조회, #367)·#362(내 정보 수정, #371)·#363(내 비밀번호 변경, #372)·#364(구성원 상세 조회, #373)에 이어지는 작업(#365). 관리자가 구성원의 정보를 고치려면 아직 DB를 직접 만져야 했다.

### 확정된 정책

- `UpdateUserProfileService`가 `UpdateMyProfileUseCase`에 이어 `UpdateMemberProfileUseCase`도 구현하도록 확장했다 — 본인 수정과 동일한 부분 수정(partial update) 방식과 `UserRepository.updateProfile`을 재사용한다.
- 본인 수정(`phone`/`email`만)과 달리 관리자는 `name`/`joinedAt`도 바꿀 수 있다. 다만 `roleId`는 이 API의 범위에서 제외했다 — 역할 변경은 이미 존재하는 `PATCH /api/users/{userId}/role`이 role 존재/소속 학원 검증까지 책임지고 있어서, 책임을 나누지 않고 그대로 유지했다.
- 대상 검증은 구성원 상세 조회(#364)와 동일한 `findById → academyId 필터 → accountType == MEMBER 필터` 패턴을 따랐다.

### 완료 기준

- [x] `UpdateMemberProfileUseCase` 추가 + `UpdateUserProfileService` 확장(TDD)
- [x] `UserController`에 `PATCH /api/users/{userId}` 반영
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 관리자용 구성원 재직상태변경 — 별도 PR(#366)에서 이어간다.
- `roleId` 변경은 기존 `PATCH /api/users/{userId}/role`을 그대로 쓴다(이 API의 범위 아님).

---

## ✅ 2026-08-11 · 구성원 상세 조회 추가

### 배경

#361(내 정보 조회, #367)·#362(내 정보 수정, #371)·#363(내 비밀번호 변경, #372)에 이어지는 작업(#364). 관리자가 구성원 목록(`GET /api/users/members`)에서 요약 정보만 볼 수 있었고, 개별 구성원의 상세 정보를 별도로 조회하는 API가 없었다.

### 확정된 정책

- `GetUserDetailService`가 `GetMyProfileUseCase`에 이어 `GetMemberDetailUseCase`도 구현하도록 확장했다 — 내 정보 조회와 동일한 `UserDetailResult`/`UserDetailResponse`를 그대로 재사용한다.
- 대상 검증은 `ChangeUserRoleService`의 `findById → academyId 필터 → accountType == MEMBER 필터` 패턴을 그대로 따랐다 — 대상이 없거나 다른 학원 소속이거나 학원 관리자 계정이면 전부 동일하게 `404 USER_404_1`로 응답해 존재 여부를 숨긴다.

### 완료 기준

- [x] `GetMemberDetailUseCase` 추가 + `GetUserDetailService` 확장(TDD)
- [x] `UserResponseCode.MEMBER_DETAIL_RETRIEVED`("USER_200_6") 추가
- [x] `UserController`에 `GET /api/users/{userId}` 반영
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 관리자용 구성원 정보 수정/재직상태변경 — 각각 별도 PR(#365~#366)에서 이어간다.

---

## ✅ 2026-08-11 · 내 비밀번호 변경 추가

### 배경

#361(내 정보 조회, #367)·#362(내 정보 수정, #371)에 이어지는 작업(#363). 로그인한 사용자가 최초 비밀번호 설정(`POST /api/users/password-setup`) 이후 스스로 비밀번호를 바꿀 방법이 없었다.

### 확정된 정책

- `PATCH /api/users/me/password`는 `POST /api/users/password-setup`(최초 1회 설정 전용)과 별개 API다 — 이미 인증된 본인 요청이라 계정 존재 여부를 숨길 필요가 없고, 현재 비밀번호가 틀리면 구체적인 오류(`USER_400_3`)를 그대로 반환한다.
- 현재 비밀번호 확인 → 새 비밀번호로 교체는 `changeRole`/`updateProfile`과 동일하게 `findById` 후 `UserEntity`의 package-private 뮤테이터를 거쳐 `flush()`하는 패턴을 따랐다.

### 완료 기준

- [x] `UserErrorCode.CURRENT_PASSWORD_MISMATCH`("USER_400_3") 추가
- [x] `UserRepository.changePassword` + `UserEntity.changePassword` 뮤테이터(TDD)
- [x] `ChangeMyPasswordUseCase`/`ChangeMyPasswordService` 구현(TDD)
- [x] `UserController`에 `PATCH /api/users/me/password` 반영
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 관리자용 구성원 상세조회/수정/재직상태변경 — 각각 별도 PR(#364~#366)에서 이어간다.

---

## ✅ 2026-08-11 · 내 정보 수정 + 계정생성 필드 선택화

### 배경

#361(내 정보 조회)에 이어지는 작업(#362). 본인의 연락처/이메일을 수정하는 API가 없었고, 계정 생성 시 phone/email이 필수라 원장이 직원 전체의 연락처를 일일이 입력해야 하는 문제가 있었다.

### 확정된 정책

- `PATCH /api/users/me`는 `phone`/`email`만 수정 가능하다 — 이름·역할·입사일은 관리자용 구성원 정보 수정(별도 PR)에서 다룬다.
- 값을 보내지 않은 필드는 기존 값을 유지하는 부분 수정(partial update)으로 구현했다.
- 계정 생성 시 `phone`/`email`을 선택 입력으로 바꿨다(`V4.1.7` 마이그레이션).
- `email` UNIQUE 제약 위반은 `EmailDuplicateException`(409)으로 변환한다 — phone/email이 선택값이 되면서 이메일 중복 케이스가 실제로 발생 가능해졌고, 기존 `UsernameDuplicateException`과 동일한 패턴을 따랐다.
- `UserRepositoryImplDataJpaTest`에 신규 저장(`save`) 테스트를 추가하는 과정에서, notice/messenger의 `users` 테이블 shim이 `@DataJpaTest` 전체 엔티티 스캔과 충돌해 `id` 컬럼의 IDENTITY 속성이 사라지는 문제를 발견했다. users 도메인 엔티티/리포지토리만 스캔하도록 테스트를 좁혀서 해결했다(다른 도메인 코드는 건드리지 않음).

### 완료 기준

- [x] `V4.1.7` 마이그레이션 + `CreateAccountRequest` 필수값 제거(TDD)
- [x] `UserRepository.updateProfile` + 이메일 중복 예외 변환(TDD)
- [x] `UpdateMyProfileUseCase`/`UpdateUserProfileService` 구현(TDD)
- [x] `UserController`에 `PATCH /api/users/me` 반영
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 비밀번호 변경, 관리자용 상세조회/수정/재직상태변경 — 각각 별도 PR(#363~#366)에서 이어간다.

---

## ✅ 2026-08-11 · 내 정보 조회 추가

### 배경

users 도메인에는 계정 발급/역할 변경/목록 조회/검색만 있고, 로그인한 사용자가 자기 자신의 정보를 조회하는 API가 없었다. 구성원 상세조회·수정·재직상태 변경·비밀번호 변경으로 이어지는 작업(#353)의 첫 단계다.

### 확정된 정책

- `/api/users/me` 경로를 사용한다 — attendance 도메인이 이미 쓰던 `/api/attendance/me` 컨벤션과 맞춘 것이다.
- 응답을 만드는 `GetUserDetailService`/`UserDetailResult`/`UserDetailResponse`는 이후 관리자용 구성원 상세조회(`GET /api/users/{userId}`)에서도 그대로 재사용할 수 있도록 설계했다.

### 완료 기준

- [x] `GetMyProfileUseCase`/`GetUserDetailService` 구현(TDD)
- [x] `UserController`에 `GET /api/users/me` 반영
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 정보 수정, 비밀번호 변경, 관리자용 상세조회/수정/재직상태변경 — 각각 별도 PR(#362~#366)에서 이어간다.

---

## ✅ 2026-08-11 · 학원 신청/승인 기능 폐기

### 배경

실제 운영 배포 모델이 "학원마다 별도 EC2 프로세스 + 별도 RDS 스키마"로 확정되면서, 앱 안에서 신규 학원을 등록한다는 개념 자체가 성립하지 않게 됐다. `academy` 테이블을 만드는 유일한 경로였던 학원 신청/승인 기능(약 30개 클래스)을 삭제했다. 상세 조사와 설계는 `docs/superpowers/specs/2026-08-11-academy-removal-design.md`를 참고.

### 확정된 정책

- 학원 신청/승인 관련 도메인·애플리케이션·인프라·프레젠테이션 계층 클래스와 테스트를 전부 삭제했다. `SubmitAcademyApplicationRequest`/`AcademyApplicationController` 등 5계층 전체가 대상이다.
- `academy_application` 테이블은 이번 작업으로 코드가 더 이상 참조하지 않는다. `academy` 테이블은 아직 `users.academy_id`/`role.academy_id`(Phase 2 전까지 유지)와 `file`/`messenger` 도메인의 `academy_id` 참조가 남아있어 활성 스키마 계약의 일부다 — Phase 2와 file/messenger 정리가 끝나기 전까지 `academy` 테이블을 DROP하면 안 된다. 두 테이블 모두 이번 작업에서 DROP하지 않았고, 각자의 잔여 참조가 모두 정리된 뒤 별도 후속 작업으로 DROP한다.
- `PLATFORM:SUPER_ADMIN` 권한 매커니즘(`AdminScope.PLATFORM`, `JwtAuthenticationConverter`의 authority 부여 로직) 자체는 건드리지 않았다 — 다른 팀원이 이 권한 체계를 쓰는 별도 기능(슈퍼 어드민 대시보드 등)을 개발 중이기 때문에, `SecurityConfig`에서 `/api/academy-applications*` 경로 매처 3개만 제거했다.
- 최초 학원 관리자(원장) 계정은 이제 학원 신청/승인이라는 별도 플로우 없이, 서버·스키마를 새로 배포한 뒤 그 배포 안에서 일반 계정 생성 절차로 수동 생성한다.

### 완료 기준

- [x] 학원 신청/승인 관련 프로덕션 클래스 30개 삭제
- [x] 학원 신청/승인 관련 테스트 클래스 10개 삭제
- [x] `UserErrorCode`에서 `ACADEMY_APPLICATION_NOT_FOUND`/`ACADEMY_APPLICATION_ALREADY_REVIEWED` 제거
- [x] `SecurityConfig`에서 `/api/academy-applications*` 매처 3개 제거, `PLATFORM:SUPER_ADMIN` authority 부여 로직은 유지 확인
- [x] `./gradlew build` 통과
- [x] 로컬 e2e로 기존 API(로그인/역할/구성원 목록 등) 정상 동작 확인

### 범위 밖 (명시적으로 보류)

- JWT/인증 체계 및 `users`/`role`의 `academyId` 완전 제거 — Phase 2, 별도 플랜.
- `academy`/`academy_application` 테이블 자체의 `DROP TABLE` — 안전하다고 판단되면 나중에 별도 후속 작업.
- `file`/`messenger` 도메인의 `academy_id` 정리 — 각 담당자에게 `MODULES.md`의 "타 모듈 변경 요청" 템플릿으로 별도 요청.

---

## ✅ 2026-08-11 · 구성원 목록 조회 페이지네이션·역할 필터

### 배경

관리자용 구성원 목록은 "학원당 구성원 규모가 작다"는 전제로 페이지네이션 없이 설계했다. 그런데 직원 수가 많은 학원도 서비스를 쓸 수 있어야 한다는 요구사항이 생겼고, 동시에 화면 목표였던 "역할별로 묶어 보여주는 조직도 뷰"를 어떻게 유지할지가 쟁점이 됐다 — 페이지네이션을 걸면 프론트가 전체 목록을 한 번에 못 받아서 클라이언트 사이드 그룹핑이 불가능해지기 때문이다.

### 확정된 정책

- `roleId` 쿼리 파라미터를 추가해서, 프론트가 역할 탭마다 독립적으로 페이지네이션된 목록을 요청하는 방식으로 해결했다. 중첩(nested) 그룹 응답 구조는 만들지 않았다.
- 페이지네이션은 기존 `GetWeeklyEmployeeAttendanceUseCase`/`WeeklyEmployeeAttendanceQueryService`가 쓰던 것과 동일한 인메모리 방식(`PageResult`/`SliceResponse`, DB 레벨 Pageable 아님)을 그대로 재사용했다 — 새 패턴을 도입하지 않았다.
- 근태 상태(`attendanceStatus`) 조회는 전체 구성원이 아니라 페이지에 포함된 구성원만 대상으로 축소했다 — 페이지네이션의 목적(불필요한 데이터 전송 감소)에 맞춘 것이다.
- 응답을 `List<MemberListResponse>`에서 `SliceResponse<MemberListResponse>`로 바꿨다 — 이 API가 아직 develop에 병합 전이라 하위 호환성 문제가 없다.

### 완료 기준

- [x] `ListMembersUseCase`/`ListMembersService`에 `roleId`/`page`/`size` 반영(TDD: 역할 필터/페이지네이션/정렬/근태 조회 범위 축소)
- [x] `UserController`에 `@Validated`·`page`/`size` 파라미터 검증 반영
- [x] 로컬 curl e2e(전체/역할 필터/페이지네이션/잘못된 파라미터 400)
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- DB 레벨 페이지네이션(Pageable/LIMIT-OFFSET) — CodeRabbit 리뷰에서도 같은 지적(전체 구성원을 로드한 뒤 인메모리로 필터·정렬·페이지 자르기를 하므로 학원 규모에 비례해 DB 조회량·메모리 사용량이 늘어남)이 나왔다. 지금은 목업 기준 학원당 구성원 규모가 작아 실익이 없다고 판단해 보류한다. **구성원 수가 실제로 커지거나 성능 지표에 문제가 확인되면 재검토한다.**
- `TodayAttendanceStatusAdapter`의 `leaveRequestRepository.findApprovedUserIds(today)`가 페이지에 포함된 userId가 아니라 학원 전체 승인 휴가자를 조회하는 것도 같은 이유로 지금은 보류한다.
- 중첩 그룹 응답 구조 — roleId 필터로 프론트가 탭 단위로 해결.

---

## ✅ 2026-08-11 · 구성원 목록에 오늘 근태 상태 포함 (`attendanceStatus`)

### 배경

관리자용 구성원 목록(`GET /api/users/members`)은 처음엔 근태 상태를 스코프 밖으로 두고, 프론트가 `GET /api/attendance/team/today`나 `GET /api/attendance/employees/weekly`를 별도 호출해 합치도록 했다. 그런데 이 두 API는 정책 시간·전체 요약·요일별 상세 배열까지 포함해 목록 화면이 실제로 쓸 값(오늘 상태 하나)에 비해 너무 무겁다는 문제가 제기됐다.

### 확정된 정책

- `users`가 `TodayAttendanceStatusPort`/`MemberTodayAttendanceStatus`를 정의하고, `attendance`가 이를 구현하는 방향으로 진행했다 — 이 프로젝트에서 `users`가 다른 도메인의 Port를 소비하는 첫 사례다(기존엔 항상 반대 방향이었다).
- Adapter(`TodayAttendanceStatusAdapter`)는 attendance의 기존 서비스(`TodayTeamAttendanceQueryService`)를 호출하지 않고, `AttendancePolicyRepository`/`LeaveRequestRepository`를 재사용하면서 `AttendanceRecordRepository`에 조회 메서드 하나(`findAllByUserIdsAndWorkDate`)만 추가해 독립적으로 상태를 계산한다 — 기존 서비스 로직은 전혀 수정하지 않았다.
- 근태 정책이 없어 상태 계산이 실패하면(`ATTENDANCE_POLICY_NOT_FOUND`), 그 오류를 감추지 않고 `GET /api/users/members` 전체를 `404 ATTENDANCE_404_1`로 실패시키기로 했다 — 부분 성공(근태만 null)보다 명확한 실패를 택했다.
- `attendanceStatus`는 `ACTIVE` 구성원에게만 채우고 `RESIGNED`/`INACTIVE`는 항상 `null`이다 — Adapter 자체는 이 필터링을 하지 않으므로 `ListMembersService`가 매핑 시점에 강제한다.

### 완료 기준

- [x] `AttendanceRecordRepository.findAllByUserIdsAndWorkDate` + 구현(TDD)
- [x] `TodayAttendanceStatusPort`/`MemberTodayAttendanceStatus` 정의, `TodayAttendanceStatusAdapter` 구현(TDD)
- [x] `ListMembersService`/`MemberListItem`/`MemberListResponse`에 `attendanceStatus` 통합(TDD)
- [x] 로컬 curl/Swagger e2e(전체 조회 시 필드 확인, 정책 없을 때 404)
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 근태 이력(주간/월간) 연동 — 필요 시 별도 브레인스토밍.
- attendance 기존 API(`/today`, `/weekly`) 응답 축소.

---

## ✅ 2026-08-10 · 관리자용 구성원 목록 조회 (`GET /api/users/members`)

### 배경

Figma 목업(구성원 관리 화면) 확인 중, 기존 "학원 구성원 검색"(`GET /api/users?keyword=`)으로는 화면(이름/이메일/역할명/연락처/입사일/상태)을 채울 수 없다는 게 확인됐다. 그 API는 워크스페이스/채팅방 멤버 선택용으로, 권한 없이 로그인만 하면 누구나 호출 가능하도록 의도적으로 `{userId, name, username}`만 반환하게 설계돼 있어 필드를 추가할 수 없었다(개인정보 노출).

### 확정된 정책

- 완전히 별도의 관리자 전용 엔드포인트(`GET /api/users/members`, `ACCOUNT:MANAGE` 권한)로 새로 만들었다.
- `status`는 `users` 도메인의 계정 상태(`ACTIVE`/`RESIGNED`/`INACTIVE`)만 원본 값으로 내려주고, "재직/비활성" 탭 분류(비활성 = RESIGNED+INACTIVE 묶음)는 프론트 책임으로 뒀다 — 백엔드는 단순 목록만 담당한다.
- 역할명은 `RoleRepository.findAllByAcademyId()`(기존 "역할 목록 조회"가 이미 쓰던 메서드)로 한 번에 조회해 애플리케이션 레벨에서 맵으로 합쳤다 — JPA 레벨 조인이나 새 프로젝션 쿼리를 만들지 않고 기존 리포지토리 메서드 재사용만으로 해결했다.
- 페이지네이션과 서버 사이드 키워드/상태 필터링(SQL WHERE 절)은 만들지 않고 애플리케이션 레벨에서 인메모리로 처리했다 — 학원당 구성원 규모가 작고(목업 기준 한 자릿수~두 자릿수), 기존 역할 목록 조회도 페이지네이션이 없는 전례를 따랐다.
- 근태 상태 통합은 처음엔 스코프 밖으로 뒀으나, 이후 별도 작업(위 2026-08-11 항목)으로 추가됐다.

### 완료 기준

- [x] `UserRepository.findAllByAcademyId()` + 구현(TDD)
- [x] `ListMembersUseCase`/`ListMembersService`(TDD: 키워드 없음/이름 검색/역할명 검색/빈 결과/roleId null 케이스)
- [x] `GET /api/users/members` 컨트롤러 핸들러 + `ACCOUNT:MANAGE` 권한
- [x] 로컬 curl e2e(전체 조회/이름 검색/역할명 검색/권한 없음 403)
- [x] `./gradlew build` 통과

### 범위 밖 (명시적으로 미룸)

- 구성원 정보 수정(이름/연락처/이메일/입사일 변경) API — 별도 브레인스토밍 예정.
- 계정 활성/비활성 전환 API — 별도 브레인스토밍 예정.

---

## ✅ 2026-08-10 · 학원 신청 접수 시점 requestedLoginId 중복확인 (`V4.1.6`)

### 배경

`ApproveAcademyApplicationService`는 승인 시점에만 `requestedLoginId` 중복을 체크했다(2026-08-10 CodeRabbit 리뷰 반영분). 접수 시점(`SubmitAcademyApplicationService`)에는 체크가 없어 신청자가 승인 시점에야 아이디 중복을 알게 됐고, `academy_application.requested_login_id`에 유니크 제약이 없어 두 신청서가 동시에 같은 아이디로 접수될 수 있었다.

### 확정된 정책

- 접수 시점에 이미 발급된 계정(`users.username`) + 현재 `PENDING`/`APPROVED` 상태인 다른 신청서와 겹치는지 확인한다. `REJECTED` 신청서는 대상에서 제외해 반려된 아이디로 재신청을 허용한다.
- 애플리케이션 사전 체크만으로는 동시 요청 레이스를 완전히 막을 수 없어, MySQL 생성 컬럼(`requested_login_id_active`, `status`가 `PENDING`/`APPROVED`일 때만 값을 갖고 그 외엔 `NULL`) + 부분 유니크 제약(`uk_academy_application_requested_login_id_active`)을 함께 걸었다. `markApproved`/`markRejected`가 `status`를 바꾸면 이 생성 컬럼이 자동 재계산되므로 애플리케이션 코드는 손대지 않았다.
- 제약 위반은 `AcademyApplicationRepositoryImpl.save()`에서 `DataIntegrityViolationException`을 캐치해 기존 `UsernameDuplicateException`(`409 USER_409_6`)으로 변환한다 — `RoleRepositoryImpl`의 역할 이름 중복 방어(`uk_role_academy_name`)와 동일한 패턴을 그대로 포팅했다.
- 승인 시점(2026-08-10 CodeRabbit 리뷰 반영분에서 추가된 체크)의 중복 확인은 그대로 유지한다 — 접수와 승인 사이의 시간차 동안 상황이 바뀔 수 있어(다른 경로로 같은 아이디의 계정이 먼저 생성되는 등) 최종 안전장치로 남겨둔다.

### 완료 기준

- [x] `V4.1.6__academy_application_requested_login_id_unique.sql` 마이그레이션
- [x] `AcademyApplicationRepository.existsActiveRequestedLoginId()` + 구현(TDD, `PENDING`/`APPROVED`/`REJECTED` 3케이스)
- [x] `AcademyApplicationRepositoryImpl.save()` 유니크 제약 위반 → `UsernameDuplicateException` 변환(TDD)
- [x] `SubmitAcademyApplicationService` 사전 중복 체크(TDD: 계정 중복/대기중 신청서 중복/정상 2케이스)
- [x] 로컬 curl e2e(정상 접수 → 같은 아이디 재접수 409 → 기존 계정 아이디 접수 409 → 반려 후 같은 아이디 재신청 정상) + Swagger(OpenAPI) 스키마 확인
- [x] `./gradlew build` 통과

---

## ✅ 2026-08-10 · 비밀번호 설정 링크 + 계정 발급 흐름 통합

### 배경

"슈퍼어드민인데 권한이 없다고 뜬다"는 버그 리포트를 조사하는 과정에서(원인은 결국 Wi-Fi IP 등록 정책과 academy_id 시딩 실수 두 가지로, 둘 다 코드 버그는 아니었다) 별개로 실제 설계 공백 하나를 발견했다: 학원 신청이 승인되면 원장 계정(`account_type=ADMIN`, `admin_scope=ACADEMY`)이 생성되지만 이 계정에 배정할 역할이 없어서, 원장이 로그인해도 아무 기능도 쓸 수 없었다. 이 문제를 계기로 계정 발급 체계 전반(원장 승인 발급 + 직원 계정 발급이 공유하는 `AccountIssuer`)의 임시 비밀번호 전달 방식도 함께 재검토했다 — 지금까지는 응답 바디에 임시 비밀번호를 평문으로 담아 관리자가 수동으로 전달하는 방식이었는데, 평문 비밀번호가 API 응답과 로그에 남는 게 계속 마음에 걸리던 부분이었다.

### 확정된 정책

- **별도 토큰 테이블을 만들지 않고 기존 `users.password`/`users.must_change_pw` 컬럼을 재사용한다.** 계정 발급 시 임시 비밀번호를 해싱해 `users.password`에 그대로 저장하고 `must_change_pw=true`로 세팅한다. 비밀번호 설정 API(`POST /api/users/password-setup`)는 아이디+임시 비밀번호로 `PasswordEncoder.matches()` 검증 후 새 비밀번호로 교체한다 — 이 임시 비밀번호 자체가 "토큰" 역할을 겸한다. 만료 시간은 별도로 두지 않았다: 설정에 성공하면 비밀번호가 즉시 바뀌어 링크가 자기 자신을 무효화하고(self-invalidating), `must_change_pw=true`라는 명시적 가드가 이미 설정을 마친 계정의 재사용(또는 실제 비밀번호가 유출된 경우의 오남용)을 막는다. 별도 테이블·만료 로직 없이 두 가지 방어가 이미 확보되는 구조라 추가 복잡도를 들이지 않기로 했다.
- **`must_change_pw`는 로그인 흐름을 막는 강제 로직으로 쓰지 않는다.** 원래 컬럼 의도는 "다음 로그인 시 비밀번호 변경을 강제"였지만, 이번엔 "최초 설정을 아직 안 마쳤음"을 나타내는 1회성 플래그로만 쓴다 — 비밀번호 설정 API가 성공하면 `false`로 바뀌고, 이 값을 이유로 다른 API 호출을 막는 필터나 체크는 추가하지 않았다. 로그인 자체를 막을지, 막는다면 어떤 API까지 막을지는 프론트 UX와 맞물린 별도 논의가 필요해 이번 스코프에서 제외했다.
- **원장 역할은 승인 시점의 권한 카탈로그 스냅샷으로 자동 생성한다.** `ApproveAcademyApplicationService`가 승인 시 `academy_id` 범위의 "원장" 역할을 새로 만들고(`Role.create`), 그 시점에 `PermissionRepository.findAll()`로 조회되는 모든 권한 코드를 그 역할에 배정한다(`RoleRepository.updatePermissions`). PLATFORM 관리자(`PlatformAdminPermissionPort`)처럼 "역할 없이 동적으로 전체 권한 부여"하는 방식은 채택하지 않았다 — `admin_scope=ACADEMY`에 그런 동적 바이패스를 추가하려면 `JwtAuthenticationConverter`의 분기 로직을 건드려야 하는데, 이는 이번 버그 수정 스코프를 넘어서는 별도 서브프로젝트로 판단해 미뤘다(아래 "범위 밖" 참고). 스냅샷 방식의 트레이드오프: 승인 이후 새 기능이 추가돼 권한 카탈로그가 늘어나도 기존 원장 역할엔 자동으로 반영되지 않는다 — 기존 역할 권한 조립 API(`PUT /api/roles/{roleId}/permissions`)로 수동 갱신해야 한다.
- **`AccountIssuer.issue()`가 임시 비밀번호 대신 비밀번호 설정 링크를 반환하도록 통일했다.** 원장 승인(`ApproveAcademyApplicationService`)과 직원 계정 발급(`CreateAccountService`)이 공유하는 `AccountIssuer`에 `PasswordSetupLinkBuilder`(신규, `app.frontend-url` 설정값 기반으로 `UriComponentsBuilder`가 링크를 조립)를 추가해, 두 흐름 모두 응답 필드명을 `passwordSetupLink`로 통일했다(`temporaryPassword`에서 변경). 프론트 URL은 `${APP_FRONTEND_URL:http://localhost:3000}`로 환경별 오버라이드 가능하게 뒀다.

### 범위 밖 (명시적으로 미룸)

- `admin_scope` 컬럼 구조 자체를 단순화하는 스키마 변경 — 별도 스펙으로 분리하기로 함(이번 조사 중 블라스트 반경을 확인했으나 착수하지 않음).
- 학원별 별도 스키마 도입으로 `academy` 테이블을 제거하는 테넌트 분리 마이그레이션 — 방향은 확정됐으나 사용자가 명시적으로 요청하기 전까지는 건드리지 않기로 함.
- 비밀번호 설정 링크가 만료되거나 분실됐을 때의 재발급(resend) 흐름 — 이번엔 다루지 않음.
- 이메일 자동 발송, 사업자등록증 검증(OCR·국세청 진위확인 API) — 기존부터 이어져 온 후속 작업, 이번에도 미룸(원장 신청 시 username 중복 확인은 같은 날 별도 작업으로 완료됨 — 위 "학원 신청 접수 시점 requestedLoginId 중복확인" 섹션 참고).

### 완료 기준

- [x] `UserRepository.completePasswordSetup(userId, newPasswordHash)` + `UserEntity.completePasswordSetup()` mutator + `DataJpaTest`
- [x] `PasswordSetupCommand`/`PasswordSetupUseCase`/`PasswordSetupService`(TDD: 아이디 없음/이미 설정 완료/임시 비밀번호 불일치/성공 4케이스)
- [x] `POST /api/users/password-setup` 컨트롤러 핸들러 + `SecurityConfig` `permitAll`
- [x] `PasswordSetupLinkBuilder`(`.encode()` 포함 — 특수문자 포함 임시 비밀번호가 쿼리 파라미터에서 깨지지 않도록)
- [x] `AccountIssuer`/`IssuedAccount`/`CreateAccountResult`/`ApproveAcademyApplicationResult` 및 대응 응답 DTO `temporaryPassword` → `passwordSetupLink` 필드 전환
- [x] `ApproveAcademyApplicationService`에 `RoleRepository`/`PermissionRepository` 의존성 추가, 원장 역할 자동 생성 + 전체 권한 배정 로직(TDD)
- [x] 로컬 curl e2e(학원 신청 승인 → 원장 역할·권한 자동 배정 확인 → 비밀번호 설정 링크로 최초 설정 → 새 비밀번호로 로그인 → 원장 권한으로 기능 호출 성공 확인)
- [x] Swagger(OpenAPI) 문서로 신규/변경 엔드포인트 응답 스키마 재검증
- [x] `./gradlew build` 통과(신규 `app.frontend-url` 설정값을 `src/test/resources/application.yaml`에도 반영해 무관한 도메인 통합 테스트 36건 컨텍스트 로딩 실패 해결)

---

## ✅ 2026-08-10 · CodeRabbit 리뷰 반영 (PR #301)

학원 신청 접수 API(아래 항목) 리뷰에서 나온 지적 중 하나를 실제 버그로 확인해 고쳤다: `ApproveAcademyApplicationService`가 `requestedLoginId` 중복 확인 없이 바로 계정을 생성하고 있었다(`CreateAccountService`는 `existsByUsername` 사전 체크가 있는데 승인 서비스만 빠져 있었음). 지금까지는 크래시 없이 `GlobalExceptionHandler`의 `DataIntegrityViolationException` 처리로 `409 COMMON_409_1`(일반 충돌)까지는 방어됐지만, 원인을 알 수 없는 일반 메시지였다. `UserRepository.existsByUsername(...)` 사전 체크를 추가해 `409 USER_409_6`("이미 사용 중인 아이디입니다")로 명확하게 응답하도록 고쳤다(신규 예외 클래스 없이 기존 `UsernameDuplicateException` 재사용). `requestedLoginId` 중복 확인 자체를 접수 시점에 막는 것은 여전히 후속 작업으로 남아있다 — 이번 수정은 승인 시점 충돌이 발생했을 때의 응답만 명확히 한 것.

그 외 README 문구 정확도, 마이그레이션 SQLFluff 포맷팅, API.md 검증·정책 설명 보강, 실패 케이스(빈 `requestedLoginId`/`plan` 누락 시 400) 테스트 추가도 함께 반영했다.

---

## ✅ 2026-08-10 · 학원 신청 접수 API 구현, 최소 스코프 (`POST /api/academy-applications`, `V4.1.5`)

### 배경

`2026-08-07-academy-application-design.md`에서 "제외됨 — 참고용"으로 남겨뒀던 신청 접수(`POST`)를 이번에 구현했다. 원래는 사업자등록증 파일 업로드(presigned URL 또는 서버 직접 수신) + OCR(사업자등록증 텍스트 추출) + 국세청 사업자등록정보 진위확인 API + 소유권 검증(전화확인/본인인증) + 악의적 공격 방어(글로벌 rate limit, `businessNo` 유니크 제약) 조합까지 상세 설계했으나, **팀 논의 결과 이 설계 전체를 보류하고 훨씬 단순한 버전으로 구현하기로 결정했다** — 사업자등록증 검증 자체를 안 할 거면 파일을 받을 필요도 없다는 판단.

### 이번에 뺀 것 (전부 향후 별도 작업)

- 사업자등록증 파일 업로드/OCR/국세청 진위확인 API 연동
- 소유권 검증(전화확인/본인인증)
- 악의적 공격 방어(rate limit, `businessNo` 유니크 제약)
- `requestedLoginId` 중복 확인(기존에도 미뤄뒀던 항목, 계속 미룸)
- 결제 연동 — `plan`은 신청 시점 선택값만 저장, 실제 리소스(EC2/S3/RDS) 프로비저닝·결제 처리는 이 프로젝트 스코프 밖

### 신규 도입

- `Plan` enum(`FREE`/`PAID`) — 향후 등급이 늘어나면 enum 상수만 추가하면 됨
- `academy_application.business_no`를 `NOT NULL → NULL` 허용으로 완화(컬럼은 유지 — 나중에 검증 기능이 붙을 때 재사용 목적)
- `AcademyApplication.submit(...)` 생성 팩토리 신규(지금까지 `restore(...)`만 있었음)
- `AcademyApplicationRepository.save(...)` 신규

### 로컬 e2e 검증 중 발견해 함께 고친 버그 2개

1. **`plan` NOT NULL 컬럼을 기본값 없이 바로 추가하면 기존 행이 빈 문자열이 됨** — MySQL이 strict 모드가 아니면 `ADD COLUMN ... NOT NULL`에 암묵적 기본값(VARCHAR는 빈 문자열)을 채우는데, 이걸 Hibernate가 enum으로 역직렬화하지 못해 목록 조회가 500으로 깨졌다. `NULL 허용으로 추가 → 'FREE'로 백필 → NOT NULL로 재변경` 3단계로 수정.
2. **`academy.business_no`도 nullable로 함께 완화해야 했음** — 접수 시점에 사업자등록번호를 안 받으니 신청서의 `businessNo`가 항상 `null`인데, 승인 시 `Academy.create(...)`가 그 값을 그대로 `academy.business_no`에 복사하다가 기존 `NOT NULL` 제약에 걸려 **승인 자체가 항상 실패**했다. `academy.business_no`를 nullable로 완화(`uk_academy_business_no` UNIQUE 제약은 유지 — MySQL은 NULL 여러 개를 유니크 위반으로 보지 않음).

### 접근 제어

`POST /api/academy-applications`만 `SecurityConfig`에서 `permitAll` — 기존 목록/상세/승인/반려 4개는 그대로 `PLATFORM:SUPER_ADMIN` 필요.

### 나중에 검증 기능을 다시 붙일 때 참고

설계 검토 과정에서 결론 낸 것들(구현은 안 했지만 기록):

- 파일 업로드는 `academy_application` 접수 API만 멀티파트로 직접 수신하고(기존 `file` 모듈의 인증 필수 presigned-URL 흐름은 안 건드림), `users` 도메인이 `BusinessLicenseStoragePort`를 소유하고 `file` 모듈이 그 어댑터를 구현하는 구조(`approval`의 `AttachmentContentPort` 패턴과 동일)가 이 코드베이스 아키텍처에 맞다.
- 검증 순서는 필드검증 → OCR(사업자등록증에서 실제 사업자번호 추출) → 국세청 진위확인(OCR 값 기준) → 중복 체크(OCR 값 기준, `PENDING`+`APPROVED`만 유니크, `REJECTED`는 재신청 허용) → 전부 통과해야 S3 업로드, 순으로 가야 한다(중복 체크가 타이핑 값이 아니라 검증된 값 기준이어야 함).
- 국세청 API는 OCR(예: CLOVA OCR 사업자등록증 특화모델)과 조합하는 게 기본안 — CLOVA eKYC(OCR+진위확인 통합)는 금융 클라우드존 B2B 상품이라 접근성이 불확실해 폴백으로만 고려.
- 자동 검증(OCR+국세청)은 "데이터가 실존 사업자와 일치하는가"만 확인하고 "제출자가 그 사업자 소유주인가"는 확인 못 한다 — 이 갭은 SUPER_ADMIN 승인 단계 수동 검토(서류 대조·전화 확인)에 계속 의존해야 한다.

### 완료 기준

- [x] `V4.1.5__academy_application_plan.sql` 마이그레이션(`business_no` nullable 완화 2곳, `plan` 컬럼 추가+백필)
- [x] `Plan` enum, `AcademyApplication.submit(...)` 생성 팩토리
- [x] `AcademyApplicationRepository.save(...)` + `DataJpaTest`
- [x] `SubmitAcademyApplicationService`(TDD, 로깅 컨벤션 적용)
- [x] 요청/응답 DTO, 응답 코드, 기존 `AcademyApplicationResponse`에 `plan` 노출
- [x] `AcademyApplicationController` POST 핸들러, `SecurityConfig` `permitAll`
- [x] 로컬 curl e2e(접수 → 400 검증 → 목록/상세 노출 → 승인 → 발급된 계정으로 실제 로그인까지 확인) + `./gradlew build` 통과

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

- **기존 call site들(`AuthUser`, `User.restore(...)`)의 시그니처를 유지한 방법**: 두 곳의 사정이 달라 서로 다른 방식을 썼다. `AuthUser`(record)는 `workspace`/`attendance`/`calendar`/`memo` 도메인의 컨트롤러 테스트 10곳이 기존 5-인자 생성자를 그대로 호출하고 있어, 7-인자 정규 생성자에 `accountType=MEMBER`, `adminScope=null`을 기본값으로 채우는 5-인자 delegating constructor를 남겨서 그 10곳을 전혀 수정하지 않았다. 반면 `User.restore(...)`는 delegating constructor 없이 15-인자 시그니처 하나뿐이다 — 이 메서드를 직접 호출하는 곳은 `users` 모듈 내부(`UserRepositoryImpl.toDomain()`)와, `users.infrastructure.persistence` 패키지에 있는 테스트 3개(`WorkspaceUserInfoAdapterTest`/`LectureTeacherDirectoryAdapterTest`/`ApprovalApproverDirectoryAdapterTest` — 이름은 다른 도메인을 가리키지만 실제로는 `users`가 그 도메인들에 제공하는 조회 어댑터를 `users` 자신이 테스트하는 파일)뿐이라, 다른 도메인 소유 코드를 건드릴 필요 없이 이 3개 테스트만 새 시그니처로 같이 갱신했다.

- **관리자 조직(academy) 및 슈퍼 어드민 계정을 schema migration이 아닌 실제 academy 행 + 수동 SQL INSERT로 만든 이유**: "MUDO 관리자 조직"을 별도 엔티티로 만드는 건 다른 도메인들의 실제 academy 데이터를 건드린다는 뜻이다. migration이면 모든 개발자/배포 환경의 DB에 이 "fake" 행이 생기게 된다 — 로컬에선 테스트 및 수동 검증에 쓰겠지만, staging/production에도 의도치 않게 생긴다. 대신 "실제로 필요한 환경의 DBA가 필요할 때 수동으로 INSERT한다"는 정책으로, schema는 그대로 두고 로컬 개발 편의상 수동 스크립트로 제공했다. 또한 `admin_scope=ACADEMY`는 아직 권한 로직이 미연동 상태라서, 지금 당장 필요하지 않은 컬럼값에 대한 데이터를 환경 전체에 배포할 필요가 없다는 판단도 있었다.

### 완료 기준

- [x] `users.account_type`/`admin_scope` 컬럼 추가(`V4.1.1`)
- [x] JWT 발급(`TokenService.issue/issueAccessToken`) 및 파싱(`JwtTokenProvider.parseAccessToken`) 메서드에 `accountType`/`adminScope` 매개변수 전파
- [x] `JwtAuthenticationConverter.toAuthentication()`에서 `accountType==ADMIN && adminScope==PLATFORM`인 경우, `RolePermissionLookupPort` 대신 `PlatformAdminPermissionPort.allPermissionCodes()` 호출해 `roleName="SUPER_ADMIN"` 고정으로 전체 권한 부여
- [x] `AuthUser`(delegating constructor로 기존 5-인자 호출 사이트 유지), `User.restore()`(호출부 3곳 직접 갱신) 시그니처 확장
- [x] 로컬 DB에 슈퍼 어드민 계정 수동 시드(username=superadmin, account_type=ADMIN, admin_scope=PLATFORM, role_id=NULL)
- [x] 로컬 e2e 검증: 슈퍼 어드민으로 로그인 → JWT가 accountType/adminScope 포함 → 모든 권한이 authorities로 부여됨 확인
- [x] `./gradlew test` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `AccountType` enum (ADMIN/MEMBER) 추가, `AdminScope` enum (PLATFORM/ACADEMY) 추가. `AuthUser` record에 `accountType`/`adminScope` 필드 추가 |
| Persistence | `users.account_type`/`admin_scope` 컬럼 추가(`V4.1.1`), `UserEntity`에 두 필드 추가 |
| Infrastructure | `PlatformAdminPermissionAdapter`/`PlatformAdminPermissionPort` 신규 구현 |
| Security(global) | `JwtClaims`에 `accountType`/`adminScope` 필드 추가, `JwtTokenProvider.createAccessToken/parseAccessToken`에 두 매개변수 추가. `JwtAuthenticationConverter`에 platform admin 체크 로직 추가(`RolePermissionLookupPort` vs `PlatformAdminPermissionPort` 분기) |
| Application(auth) | `TokenService.issue/issueAccessToken` 메서드 시그니처에 두 매개변수 추가 |
| Application(users) | `LoginService`/`RefreshService`에서 `TokenIssuerUseCase` 호출 시 `accountType`/`adminScope` 전달 로직 추가 |
| Domain(users) | `User.restore()` 생성자 확장(delegating constructor 없이 15-인자 단일 시그니처, 호출부 3곳 직접 갱신) |

---

## ✅ 2026-08-07 · 학원 신청 목록 조회 API 구현 (`GET /api/academy-applications`, 이슈 #165, PR 1/3)

### 배경

당초 "SUPER ADMIN이 학원 계정을 직접 발급"으로 설계했으나, 팀 기획 자료(모체 레포 Gym-Jjak_BE의 `organization_applications`→`organizations` 승인 패턴)를 확인한 결과 "학원이 신청 → SUPER ADMIN 승인/반려 → 승인 시점에 계정 발급" 구조였다. `docs/superpowers/specs/2026-08-07-academy-application-design.md` 참고.

전체 워크플로우(신청 접수/목록/상세/승인/반려 4개 엔드포인트)를 한 번에 구현하려 했으나, 리뷰 과정에서 "이슈 하나·PR 하나·기능 하나" 원칙에 어긋난다는 지적이 있었다. 게다가 신청 접수는 사업자등록증 파일 업로드 없이는 반쪽짜리로 판단해 이번 스코프에서 완전히 제외했다. 그래서 **목록 조회(PR 1) → 상세 조회(PR 2, 신규 추가) → 승인/반려(PR 3)** 순서로 3개 PR로 나눠 진행하기로 했다. 이 PR은 그중 첫 번째로, `academy_application` 테이블과 목록 조회만 다룬다.

### 확정된 정책

- **신청 접수(`POST`) API는 이번 스코프에서 완전히 제외했다.** 파일 업로드(`business_license_file_id`) 없이는 실사용이 불가능한 반쪽짜리라, presigned URL 발급 등 파일 업로드 인프라가 갖춰진 뒤 별도로 다시 설계한다. 테스트용 신청서 데이터는 로컬 DB에 수동 SQL로 직접 넣는다(SUPER ADMIN 계정 수동 시드와 동일한 방식).
- **`AcademyApplication` 도메인 모델은 `restore()` 팩토리 하나만 가진다.** `submit()` 생성 팩토리, `ensurePending()` 가드는 각각 접수 API·승인/반려 API가 추가되는 PR에서 함께 들어온다 — 지금 당장 호출부가 없는 메서드를 미리 만들어두면 리뷰어가 "왜 있는데 아무도 안 부르냐"고 묻게 된다.
- **`AcademyApplicationRepository`는 `findAll()` 하나만 가진다.** `findById`(PR 2), `markApproved`/`markRejected`(PR 3)도 같은 이유로 필요한 PR에서 추가한다. `AcademyApplicationEntity`도 지금은 읽기 전용(빌더·mutator 메서드 없음)이다.
- **목록 조회는 이 코드베이스에서 처음으로 `@PreAuthorize` 대신 `SecurityConfig` 필터체인의 URL 매칭으로 막는다.** `ACADEMY:CREATE` 같은 권한 코드를 카탈로그에 추가하는 방식은 안전하지 않다 — 카탈로그 코드는 학원 관리자가 자기 역할에 자유롭게 배정할 수 있어서, 넣는 순간 학원 관리자가 신청 목록을 볼 수 있게 된다. 대신 `JwtAuthenticationConverter`가 `accountType==ADMIN && adminScope==PLATFORM`일 때만 카탈로그에 없는 합성 authority `PLATFORM:SUPER_ADMIN`을 추가로 부여하고, `SecurityConfig`가 `GET /api/academy-applications`를 `hasAuthority("PLATFORM:SUPER_ADMIN")`으로 정확히 매칭한다. 이 authority 부여 로직 자체는 이후 PR(상세/승인/반려)에서도 그대로 재사용된다.

### 완료 기준

- [x] `be4/V4.1.2__create_academy_application.sql` 마이그레이션(`academy_application` 테이블, `academy.application_id` 컬럼)
- [x] `AcademyApplicationStatus` enum, `AcademyApplication` 도메인 모델(`restore()`만)
- [x] `AcademyApplicationRepository.findAll()` + JPA 구현체
- [x] `ListAcademyApplicationsService`
- [x] `AcademyApplicationController`(`GET /api/academy-applications`)
- [x] `JwtAuthenticationConverter`에 `PLATFORM:SUPER_ADMIN` 합성 authority 추가(TDD), `SecurityConfig`에 경로 규칙 추가
- [x] 로컬 curl end-to-end 검증(수동 시드한 신청서로 목록 조회, 비SUPER ADMIN 계정으로 403 확인)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Migration | `V4.1.2`(`academy_application` 테이블 신설, `academy.application_id` 컬럼 추가) |
| Domain(users) | `AcademyApplicationStatus` enum, `AcademyApplication` 도메인 모델(`restore()`) 신규 |
| Domain(users) | `AcademyApplicationRepository` 인터페이스(`findAll()`) 신규 |
| Persistence(users) | `AcademyApplicationEntity`(읽기 전용)/`AcademyApplicationJpaRepository`/`AcademyApplicationRepositoryImpl` 신규 |
| Application(users) | `ListAcademyApplicationsUseCase`/`ListAcademyApplicationsService` 신규 |
| Presentation(users) | `AcademyApplicationController`(`GET` 목록) 신규, `AcademyApplicationResponseCode`, `AcademyApplicationResponse` 신규 |
| Security(global) | `JwtAuthenticationConverter`에 `PLATFORM:SUPER_ADMIN` 합성 authority 부여 로직 추가. `SecurityConfig`에 목록 조회 경로 접근 규칙 추가(이 코드베이스 최초의 필터체인 URL 매칭 기반 인가) |

---

## ✅ 2026-08-07 · 학원 신청 상세 조회 API 구현 (`GET /api/academy-applications/{applicationId}`, 이슈 #165, PR 2/3)

### 배경

PR 1(목록 조회)이 develop에 머지된 뒤 그 위에서 이어가는 두 번째 PR. 목록 조회는 이미 있지만, 원래 설계엔 없던 상세 조회를 새로 추가한다 — 나중에 프론트 상세 화면 디자인이 확정되면 목록과 다른 필드가 필요해질 수 있어, 처음부터 별도 엔드포인트로 분리해뒀다.

### 확정된 정책

- `AcademyApplicationRepository`에 `findById(Long)`을 추가했다. `findAll()`과 마찬가지로 조회 전용이라 별다른 트랜잭션 고려사항은 없다.
- 신청서를 찾지 못하면 `AcademyApplicationNotFoundException`(`USER_404_3`)을 던진다 — 기존 `RoleNotFoundException`(`USER_404_2`)과 동일한 패턴.
- `GetAcademyApplicationService`를 신규로 만들었다 — PR 1 이전 설계(승인/반려 서비스에 조회 로직이 내장돼 있던 버전)와 달리, 목록/상세 조회가 이미 별도 서비스(`ListAcademyApplicationsService`)로 분리된 상태라 같은 원칙을 그대로 따랐다.
- `SecurityConfig`의 GET 규칙을 `/api/academy-applications` 단일 경로에서 `/api/academy-applications`, `/api/academy-applications/*` 두 패턴으로 확장했다 — 목록/상세 모두 동일하게 `PLATFORM:SUPER_ADMIN`으로 인가한다.

### 완료 기준

- [x] `AcademyApplicationRepository.findById(Long)` + JPA 구현체
- [x] `AcademyApplicationNotFoundException` + `UserErrorCode.ACADEMY_APPLICATION_NOT_FOUND`(`USER_404_3`)
- [x] `GetAcademyApplicationService`(TDD: 정상 조회 / 404)
- [x] `AcademyApplicationController`에 `GET /{applicationId}` 핸들러 추가
- [x] `SecurityConfig` GET 규칙에 `/api/academy-applications/*` 패턴 추가
- [x] `AcademyApplicationSecurityIntegrationTest`에 상세 조회 401/403/200 케이스 추가
- [x] 로컬 curl end-to-end 검증(상세 조회 성공, 존재하지 않는 ID 404, 비SUPER ADMIN 403)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `AcademyApplicationRepository.findById(Long)` 추가. `AcademyApplicationNotFoundException` 신규, `UserErrorCode.ACADEMY_APPLICATION_NOT_FOUND`(`USER_404_3`) 추가 |
| Persistence(users) | `AcademyApplicationRepositoryImpl.findById` 구현 |
| Application(users) | `GetAcademyApplicationUseCase`/`GetAcademyApplicationService` 신규 |
| Presentation(users) | `AcademyApplicationController`에 `GET /{applicationId}` 핸들러 추가, `AcademyApplicationResponseCode.ACADEMY_APPLICATION_200_2` 추가 |
| Security(global) | `SecurityConfig` GET 규칙에 `/api/academy-applications/*` 패턴 추가 |

---

## ✅ 2026-08-07 · 역할 목록 조회 API 구현 (`GET /api/roles`, 이슈 #183, 역할 CRUD 4개 중 1번째)

### 배경

역할 생성(#59)과 권한 조립(#84)만 있고 조회가 안 되는 상태라 프론트 "역할 설정" 화면이 실제로 동작하지 않았다. 역할 관리 API 7개 중 남은 4개(목록/상세/수정/삭제)를 "1 이슈 = 1 PR = 1 기능" 원칙에 따라 나눠 진행하며, 이 PR은 목록 조회다. 설계: `docs/superpowers/specs/2026-08-06-role-crud-design.md`, 계획: `docs/superpowers/plans/2026-08-07-role-crud.md`.

### 확정된 정책

- `RoleRepository.findAllByAcademyId()`는 목록 응답이 권한 정보를 안 내려주므로, 기존 `toDomain()`(항상 `permissions` LAZY 컬렉션을 건드림)을 재사용하지 않고 권한 없이 매핑하는 별도 `toDomainWithoutPermissions()`를 뒀다 — 안 그러면 역할마다 권한 조회 쿼리가 추가로 나가는 N+1이 생긴다.
- 원래 스펙에는 `Role.withNameAndDescription()`을 추가하기로 돼 있었으나, 실제 수정 흐름(다음 PR)은 `RoleRepository.updateNameAndDescription()`(관리 엔티티 직접 mutate)만 쓰고 이 메서드를 호출하지 않아 스코프에서 뺐다. 확인해보니 같은 패턴으로 먼저 추가됐던 `Role.withPermissionCodes()`도 코드베이스 어디에서도 호출되지 않는 죽은 코드였다 — 같은 실수를 반복하지 않기로 했다.

### 완료 기준

- [x] `RoleJpaRepository.findAllByAcademyId` 추가
- [x] `RoleRepository.findAllByAcademyId` 추가, `RoleRepositoryImpl`에 구현(`toDomainWithoutPermissions` 포함)
- [x] `ListRolesUseCase`/`ListRolesService`(TDD)
- [x] `RoleResponseCode.ROLE_LIST_FOUND`(`ROLE_200_1`), `RoleListResponse`
- [x] `RoleController`에 `GET` 목록 핸들러 추가
- [x] 로컬 curl end-to-end 검증
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `RoleRepository.findAllByAcademyId(Long)` 추가 |
| Persistence(users) | `RoleJpaRepository.findAllByAcademyId` 추가, `RoleRepositoryImpl`에 구현 + `toDomainWithoutPermissions` 신규 |
| Application(users) | `ListRolesUseCase`/`ListRolesService` 신규 |
| Presentation(users) | `RoleController`에 `GET /api/roles` 핸들러 추가, `RoleResponseCode.ROLE_LIST_FOUND`, `RoleListResponse` 신규 |

---

## ✅ 2026-08-07 · 역할 상세 조회 API 구현 (`GET /api/roles/{roleId}`, 이슈 #185, 역할 CRUD 4개 중 2번째)

### 배경

역할 목록 조회(#183, PR #184)에서는 이름/설명만 내려주고 `permissionCodes`를 뺐다 — 실제로 어떤 권한이 담겼는지 확인하려면 별도 상세 조회가 필요하다. 계획: `docs/superpowers/plans/2026-08-07-role-crud.md` Task 6.

### 확정된 정책

- 리포지토리 변경이 없다. 기존 `RoleRepository.findById()`(권한 조립 API가 이미 쓰고 있던, `permissions`까지 채워서 반환하는 조회)를 그대로 재사용한다 — 목록 조회 때와 달리 상세 조회는 애초에 권한 정보가 필요하므로 별도 매퍼가 필요 없다.
- 역할이 아예 없는 경우와 다른 학원 소유인 경우를 동일하게 `RoleNotFoundException`(`404 USER_404_2`)으로 처리한다 — 권한 조립 API(`updatePermissions`)에서 이미 확립된 정책을 그대로 따른다.
- PR #184(역할 목록 조회)가 develop에 머지되기 전에 그 브랜치(`feature/users/role-list`) 위에서 이어 작업했다 — `RoleController.java`를 두 PR이 함께 수정하므로, develop에서 병렬로 브랜치를 따면 머지 충돌이 난다.

### 완료 기준

- [x] `GetRoleUseCase`/`GetRoleService`(TDD, 3케이스: 정상 조회/미존재 404/다른 학원 소유 404)
- [x] `RoleResponseCode.ROLE_DETAIL_FOUND`(`ROLE_200_2`), `RoleDetailResponse`(`permissionCodes` 포함)
- [x] `RoleController`에 `GET /{roleId}` 핸들러 추가
- [x] 로컬 curl end-to-end 검증
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | 없음(기존 `RoleRepository.findById` 재사용) |
| Application(users) | `GetRoleUseCase`/`GetRoleService` 신규 |
| Presentation(users) | `RoleController`에 `GET /api/roles/{roleId}` 핸들러 추가, `RoleResponseCode.ROLE_DETAIL_FOUND`, `RoleDetailResponse` 신규 |

---

## ✅ 2026-08-07 · 역할 수정 API 구현 (`PUT /api/roles/{roleId}`, 이슈 #187, 역할 CRUD 4개 중 3번째)

### 배경

역할 생성 후 이름/설명을 잘못 지었거나 바꾸고 싶으면 지금까지는 방법이 없었다(삭제 API도 아직 없어 재생성도 불가능). 계획: `docs/superpowers/plans/2026-08-07-role-crud.md` Task 1, Task 2 나머지, Task 7.

### 확정된 정책

- **이름/설명 수정은 `RoleRepository.updatePermissions()`와 동일한 관리(managed) 엔티티 직접 mutate 패턴을 쓴다.** `RoleEntity.update(name, description)`을 package-private으로 추가하고, `RoleRepositoryImpl.updateNameAndDescription()`이 `findWithPermissionsById()`로 로드한 관리 엔티티를 직접 mutate한다. 트랜잭션 커밋 시점에 dirty-checking으로 반영되므로 `@Transactional`이 걸린 서비스 계층에서만 호출해야 한다.
- **원래 스펙에 있던 `Role.withNameAndDescription()`(불변 도메인 객체의 with-copy 메서드)은 추가하지 않는다.** 실제 흐름이 `RoleRepository.updateNameAndDescription()`(관리 엔티티 mutate)만 쓰고 도메인 객체의 with-copy 메서드를 전혀 거치지 않기 때문이다. 같은 패턴으로 먼저 추가됐던 `Role.withPermissionCodes()`가 코드베이스 어디서도 호출되지 않는 죽은 코드였던 것이 확인돼(역할 목록 조회 PR에서), 같은 실수를 반복하지 않기로 했다.
- **이름 중복 검사는 자기 자신을 제외한다(`existsByAcademyIdAndNameAndIdNot`).** 이름을 바꾸지 않고 설명만 고치는 수정 요청이 "자기 자신과 이름이 겹친다"는 이유로 거부되면 안 되기 때문이다.
- **권한 목록(`permissionCodes`)은 이 API의 스코프가 아니다.** 기존 `PUT /api/roles/{roleId}/permissions`(권한 조립)를 그대로 쓴다 — 이름/설명 수정과 권한 조립은 서로 다른 빈도로, 다른 화면에서 일어나는 별개의 연산이라고 판단했다.

### 완료 기준

- [x] `RoleEntity.update(name, description)` 추가
- [x] `RoleRepository`/`RoleJpaRepository`/`RoleRepositoryImpl`에 `existsByAcademyIdAndNameAndIdNot`/`updateNameAndDescription` 추가, `RoleRepositoryImplDataJpaTest`에 케이스 추가
- [x] `UpdateRoleCommand`/`UpdateRoleUseCase`/`UpdateRoleService`(TDD, 4케이스: 미존재 404/다른 학원 404/이름 중복 409/정상 수정)
- [x] `UpdateRoleRequest`
- [x] `RoleController`에 `PUT /{roleId}` 핸들러 추가
- [x] 로컬 curl end-to-end 검증(정상 수정 204/이름 중복 409/자기 이름 유지 204/권한 없음 403)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Persistence(users) | `RoleEntity.update(name, description)` 추가, `RoleJpaRepository.existsByAcademyIdAndNameAndIdNot` 추가, `RoleRepositoryImpl`에 `existsByAcademyIdAndNameAndIdNot`/`updateNameAndDescription` 구현 |
| Domain(users) | `RoleRepository`에 `existsByAcademyIdAndNameAndIdNot`/`updateNameAndDescription` 추가 |
| Application(users) | `UpdateRoleCommand`/`UpdateRoleUseCase`/`UpdateRoleService` 신규 |
| Presentation(users) | `RoleController`에 `PUT /api/roles/{roleId}` 핸들러 추가, `UpdateRoleRequest` 신규 |

---

## ✅ 2026-08-07 · 역할 삭제 API 구현 (`DELETE /api/roles/{roleId}`, 이슈 #189, 역할 CRUD 4개 중 4번째)

### 배경

역할 관리 API 7개 중 마지막 하나. 이 PR로 생성/목록/상세/수정/삭제/권한 조립/권한 카탈로그 조회가 모두 갖춰진다. 계획: `docs/superpowers/plans/2026-08-07-role-crud.md` Task 3, Task 4, Task 8.

### 확정된 정책

- **삭제 전에 `UserRepository.existsByRoleId(roleId)`로 배정된 구성원이 있는지 명시적으로 체크한다.** DB의 FK 제약(예: `ON DELETE RESTRICT`)에 기대는 대신 애플리케이션 레이어에서 먼저 검사해, "왜 삭제가 안 되는지" 사용자가 이해할 수 있는 전용 에러 코드(`USER_409_2`)로 안내한다. FK 예외를 잡아서 변환하는 방식보다 이 방식이 의도를 명확히 드러낸다.
- **`ROLE_IN_USE`는 학원 범위를 따로 확인하지 않는다.** 애초에 `academyId`로 스코프가 걸린 역할(`findById` 필터)만 이 지점까지 도달하므로, 그 역할을 쓰는 사용자는 같은 학원 소속일 수밖에 없다.

### 완료 기준

- [x] `UserErrorCode.ROLE_IN_USE`(`USER_409_2`) + `RoleInUseException`
- [x] `RoleRepository.deleteById` 추가, `RoleRepositoryImpl` 구현(`RoleJpaRepository`가 상속하는 `JpaRepository.deleteById` 재사용)
- [x] `UserRepository`/`UserJpaRepository`/`UserRepositoryImpl`에 `existsByRoleId` 추가
- [x] `DeleteRoleCommand`/`DeleteRoleUseCase`/`DeleteRoleService`(TDD, 4케이스: 미존재 404/다른 학원 404/사용 중 409/정상 삭제)
- [x] `RoleController`에 `DELETE /{roleId}` 핸들러 추가
- [x] 로컬 curl end-to-end 검증(사용 중인 역할 삭제 시도 409, 미사용 역할 삭제 204, 미존재 404, 권한 없음 403)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `UserErrorCode.ROLE_IN_USE` 추가, `RoleInUseException` 신규, `RoleRepository.deleteById`/`UserRepository.existsByRoleId` 추가 |
| Persistence(users) | `RoleRepositoryImpl.deleteById`/`UserRepositoryImpl.existsByRoleId` 구현, `UserJpaRepository.existsByRoleId` 추가 |
| Application(users) | `DeleteRoleCommand`/`DeleteRoleUseCase`/`DeleteRoleService` 신규 |
| Presentation(users) | `RoleController`에 `DELETE /api/roles/{roleId}` 핸들러 추가 |

---

## ✅ 2026-08-07 · 역할 수정/삭제 동시성 방어 보강 (셀프 리뷰)

### 배경

역할 CRUD 4개 PR을 다 올린 뒤 직접 코드 리뷰를 하다가, `updateNameAndDescription()`/`deleteById()`가 애플리케이션 레벨 사전 체크(이름 중복/사용 중 확인)만 믿고 있고, 그 체크와 실제 쓰기 사이의 TOCTOU(check-then-act) 틈에서 DB 제약조건(유니크 제약 `uk_role_academy_name`, FK `fk_users_role`)에 걸리면 그 예외를 잡아주는 코드가 없어서 문서화된 409 대신 원시 500이 나가는 걸 발견했다. `save()`는 이미 `saveAndFlush()`를 `DataIntegrityViolationException`으로 감싸는 방어 로직이 있는데, 나중에 추가된 두 메서드에는 이 패턴이 빠져 있었다.

### 확정된 정책

- **`RoleRepositoryImpl`의 모든 쓰기 경로(`save`/`updateNameAndDescription`/`deleteById`)가 동일한 방어 패턴을 따른다**: 변경 직후 `roleJpaRepository.flush()`를 명시적으로 호출해 SQL을 즉시 실행시키고, `DataIntegrityViolationException`을 잡아 메시지에 특정 제약조건 이름이 포함되어 있는지 확인한 뒤 알맞은 도메인 예외로 변환한다. `entity.update(...)`나 `deleteById(...)`만으로는 Hibernate가 SQL을 트랜잭션 커밋 시점까지 미루기 때문에, 명시적 flush 없이는 이 메서드 안에서 예외를 잡을 수 없다.
- **이건 애플리케이션 레벨 사전 체크(`existsByAcademyIdAndNameAndIdNot`/`existsByRoleId`)를 대체하는 게 아니라 보강한다.** 사전 체크는 여전히 정상 경로에서 빠른 실패와 명확한 흐름을 제공하고, DB 제약조건 catch는 사전 체크와 실제 쓰기 사이의 경합 상황을 잡아주는 마지막 방어선이다.
- **`RoleInUseException`에 `Throwable cause`를 받는 생성자를 추가**해 `RoleNameDuplicateException`과 동일한 패턴을 따르게 했다.
- **경합 자체는 순차 curl로 재현이 안 되므로, 실제 MySQL이 던지는 것과 동일한 형식의 예외 메시지를 강제로 발생시키는 Mockito 단위 테스트(`RoleRepositoryImplTest`)로 검증했다** — `save()`에 이미 있던 것과 같은 스타일.

### 완료 기준

- [x] `RoleRepositoryImpl.updateNameAndDescription()`에 flush + `DataIntegrityViolationException` catch 추가
- [x] `RoleRepositoryImpl.deleteById()`에 flush + `DataIntegrityViolationException` catch 추가
- [x] `RoleInUseException(Throwable cause)` 생성자 추가
- [x] `isRoleNameConflict` → `containsConstraint(Throwable, String)`로 일반화해 3곳(`save`/`updateNameAndDescription`/`deleteById`)에서 재사용
- [x] `RoleRepositoryImplTest`에 4케이스 추가(이름 중복 변환/무관한 위반 통과, 사용 중 변환/무관한 위반 통과)
- [x] `./gradlew build` 통과, 로컬 curl로 회귀 없음 확인

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `RoleInUseException(Throwable cause)` 생성자 추가 |
| Persistence(users) | `RoleRepositoryImpl.updateNameAndDescription`/`deleteById`에 flush+catch 추가, `containsConstraint` 헬퍼로 일반화 |
| Test(users) | `RoleRepositoryImplTest`에 4케이스 추가 |

---

## ✅ 2026-08-07 · 역할 삭제 정책을 "재직 중인 구성원 기준"으로 조정

### 배경

역할 삭제(`existsByRoleId`)가 상태와 관계없이 role_id를 참조하는 계정이 하나라도 있으면 무조건 막는 구조였는데, 이 시스템엔 계정을 물리적으로 삭제하는 기능이 없다(퇴사 처리는 `status=RESIGNED`로 바꾸는 소프트 딜리트뿐). 그러면 한 번이라도 누군가에게 배정된 역할은 그 사람이 퇴사해도 `role_id`가 그대로 남아있어 **영원히 삭제할 수 없는** 역할이 생긴다는 문제가 발견됐다.

### 확정된 정책

- **삭제를 막는 기준을 `ACTIVE` 상태 구성원으로 좁혔다** (`existsByRoleId` → `existsActiveByRoleId`). 퇴사자(`RESIGNED`)/비활성(`INACTIVE`) 계정이 이 역할을 들고 있어도 삭제를 막지 않는다 — 어차피 로그인이 제한된 계정이라 역할 정보가 실질적 권한에 영향을 주지 않는다.
- **삭제 시 그 역할을 들고 있던 나머지(비활성) 계정들의 `role_id`를 명시적으로 NULL 처리한다** (`UserRepository.clearRoleId`). DB의 `fk_users_role` FK가 `RESTRICT`라 role_id를 참조하는 행이 남아있으면 삭제 자체가 실패하기 때문에, 역할을 지우기 전에 반드시 이 정리가 선행되어야 한다.
- FK를 `ON DELETE SET NULL`로 마이그레이션하는 대안도 검토했으나, 그러면 계정의 role_id가 조용히 NULL이 되어 "왜 이렇게 됐는지" 코드만 봐서는 추적하기 어려워진다고 판단해 애플리케이션 레벨에서 명시적으로 처리하는 쪽을 택했다.
- `ACTIVE` 여부 확인이 먼저 통과했다는 건 이 시점에 이 역할을 쓰는 사람이 전부 비활성 상태라는 뜻이므로, `clearRoleId`는 상태를 다시 필터링하지 않고 해당 `role_id`를 가진 모든 행을 정리한다.

### 완료 기준

- [x] `UserRepository.existsByRoleId` → `existsActiveByRoleId`로 변경, `clearRoleId` 추가
- [x] `UserJpaRepository.existsByRoleIdAndStatus` 파생 쿼리, `clearRoleId` 벌크 업데이트(`@Modifying`) 추가
- [x] `DeleteRoleService`가 삭제 전 `clearRoleId` 호출하도록 수정
- [x] `DeleteRoleServiceTest` 케이스 갱신(재직 중 구성원만 차단, 비활성 구성원은 자동 정리 후 삭제)
- [x] 로컬 curl/DB로 end-to-end 검증(퇴사자만 보유한 역할 삭제 → 204 + role_id 정리 확인, 재직 중 구성원 보유 역할 → 여전히 409)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `UserRepository.existsByRoleId` → `existsActiveByRoleId`, `clearRoleId` 추가 |
| Persistence(users) | `UserJpaRepository.existsByRoleIdAndStatus`/`clearRoleId` 추가, `UserRepositoryImpl` 구현 |
| Application(users) | `DeleteRoleService`가 삭제 전 `clearRoleId` 호출하도록 수정 |

---

## ✅ 2026-08-07 · 학원 신청 승인/반려 API 구현 (`POST .../approve`, `POST .../reject`, 이슈 #165, PR 3/3)

### 배경

PR 1(목록)·PR 2(상세)에 이어지는 세 번째이자 마지막 PR. 승인 시점에 academy와 최초 관리자 계정을 함께 발급하는 핵심 트랜잭션을 구현한다. "계정 발급 체계" 2단계(학원 신청/승인 워크플로우)가 이 PR로 완료된다.

### 확정된 정책

- **academy↔user 순환참조는 "academy를 `user_id=NULL`로 먼저 만들고 나중에 채우는" 순서로 푼다.** `users.academy_id`가 `NOT NULL`이라 user를 academy보다 먼저 만들 수 없다 — SUPER ADMIN 인증 연결(PR #156) 때 수동 시드로 이미 검증한 것과 동일한 원리를 승인 트랜잭션에 그대로 적용했다.
- **`AcademyRepository.save()`는 생성 전용, `assignUser()`/`AcademyApplicationRepository`의 `markApproved()`/`markRejected()`는 관리 엔티티 직접 mutate.** `academy`/`academy_application` 둘 다 `updated_at`이 `ON UPDATE CURRENT_TIMESTAMP`인데, detached 도메인 객체를 다시 `save()`(merge)하면 Hibernate가 로드 시점의 `updated_at` 값을 그대로 UPDATE문에 실어보내 MySQL의 자동 갱신을 덮어써버린다. 기존 `RoleRepository.save()`(생성 전용)/`updatePermissions()`(관리 엔티티 직접 mutate) 분리 패턴을 그대로 따라 이 문제를 피했다.
- **임시 비밀번호는 승인 응답에 평문으로 한 번만 내려준다.** 이메일 발송 인프라가 없어 SUPER ADMIN이 신청자에게 수동으로 전달해야 한다 — 이메일 발송이 생기면 이 응답 필드는 제거될 예정.
- **`ensurePending()` 가드**로 이미 검토된(승인/반려) 신청서의 재승인/재반려를 막는다(`AcademyApplicationAlreadyReviewedException`, `USER_409_5`).
- SecurityConfig의 approve/reject 경로는 목록/상세와 동일하게 `PLATFORM:SUPER_ADMIN` 필터체인 인가를 재사용한다.

### 완료 기준

- [x] `AcademyStatus` enum, `Academy` 도메인 모델(`create`/`restore`)
- [x] `User.create(...)` 팩토리 추가(최초의 사용자 생성 경로)
- [x] `AcademyRepository`(신규) + `AcademyManagementJpaRepository`/`AcademyManagementRepositoryImpl`(빈 이름 충돌 회피 명명), `AcademyManagementRepositoryImplDataJpaTest`(더티체킹 검증)
- [x] `UserRepository.save(User)` 추가 + 구현
- [x] `AcademyApplication.ensurePending()`, `AcademyApplicationRepository.markApproved`/`markRejected` + 구현
- [x] `AcademyApplicationAlreadyReviewedException` + `UserErrorCode.ACADEMY_APPLICATION_ALREADY_REVIEWED`(`USER_409_5`)
- [x] `ApproveAcademyApplicationService`(TDD, 핵심 순환참조 트랜잭션)/`RejectAcademyApplicationService`(TDD)
- [x] `AcademyApplicationController` approve/reject 엔드포인트
- [x] `SecurityConfig`에 approve/reject 경로 규칙 추가
- [x] `AcademyApplicationApprovedEvent` 정의 및 발행
- [x] 서비스 단위 테스트 2세트 + `AcademyApplicationSecurityIntegrationTest`에 approve/reject 401/403/200(204) 케이스 추가
- [x] 로컬 curl end-to-end 검증(승인 → 임시 비밀번호로 로그인 성공 → 그 계정으로 SUPER ADMIN 전용 엔드포인트 호출 시 403 → 반려 플로우까지 확인)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `AcademyStatus` enum, `Academy` 도메인 모델 신규. `User.create()` 팩토리 추가. `AcademyApplication.ensurePending()` 추가. `AcademyApplicationAlreadyReviewedException` 신규, `UserErrorCode.ACADEMY_APPLICATION_ALREADY_REVIEWED`(`USER_409_5`) 추가 |
| Domain(users) | `AcademyRepository` 인터페이스 신규(`save`/`assignUser`). `AcademyApplicationRepository`에 `markApproved`/`markRejected` 추가. `UserRepository`에 `save` 추가 |
| Persistence(users) | `AcademyEntity`/`AcademyManagementJpaRepository`/`AcademyManagementRepositoryImpl` 신규. `AcademyApplicationEntity`에 `markApproved`/`markRejected` mutator 추가. `UserRepositoryImpl.save` 구현 |
| Application(users) | `ApproveAcademyApplicationService`/`RejectAcademyApplicationService` 및 대응 Command/UseCase/Result 신규 |
| Presentation(users) | `AcademyApplicationController`에 approve/reject 핸들러 추가, `AcademyApplicationResponseCode.ACADEMY_APPLICATION_200_3`, `RejectAcademyApplicationRequest`/`AcademyApplicationApproveResponse` 신규 |
| Domain event(users) | `AcademyApplicationApprovedEvent` 신규(리스너 없음, 알림 발송 인프라 부재) |
| Security(global) | `SecurityConfig`에 approve/reject 경로 접근 규칙 추가 |

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

## ✅ 2026-08-08 · 사용자 역할 변경 API 구현 (`PATCH /api/users/{userId}/role`, 이슈 #208)

### 배경

역할 CRUD(생성/목록/상세/수정/삭제/권한조립/카탈로그조회) 7개가 develop에 머지 완료됐다. 그런데 역할 삭제 정책을 "재직 중(ACTIVE)인 구성원이 이 역할을 쓰고 있으면 삭제를 막는다"로 정하면서(`existsActiveByRoleId`), 실제로 구성원의 역할을 다른 역할로 바꾸는 방법이 시스템에 전혀 없다는 게 드러났다 — 역할을 삭제하려면 그 역할을 쓰는 사람들을 먼저 다른 역할로 옮겨야 하는데, 그 기능 자체가 없었다. 설계: `docs/superpowers/specs/2026-08-08-user-role-change-design.md`.

### 확정된 정책

- **대상은 일반 직원 계정(`accountType=MEMBER`)만.** 학원 관리자(`admin_scope=ACADEMY`) 계정은 애초에 카탈로그 역할을 쓰는 구조가 아니고(권한 로직 자체가 아직 미연동 상태), 이 API의 대상이 아니다.
- **역할 해제(`roleId`를 `null`로 만들기)는 이 API 스코프가 아니다.** 재직 중인 계정을 "역할 없음"으로 만들면 로그인은 되지만 모든 `@PreAuthorize` 엔드포인트에서 403이 나는 사실상 먹통 계정이 된다(권한 조회가 `roleId=null`이면 즉시 빈 권한을 반환하기 때문). `roleId`가 `null`이 되는 경우는 계속 시스템이 내부적으로 처리하는 것(퇴사자 역할 삭제 시 `clearRoleId`의 자동 정리)으로만 남긴다.
- **신규 권한 코드 `ACCOUNT:MANAGE` 도입.** 2026-08-08 팀 회의에서 "관련 있는 행위는 백엔드에서 하나의 코드로 묶어둔다"는 원칙으로 정리됨(예전 "백엔드는 세분화, 프론트가 그룹 토글로 묶어 보여준다" 원칙은 폐기). 역할 변경은 역할 정의(`ROLE:*`)가 아니라 계정 관리(`ACCOUNT:*`) 리소스에 속한다고 판단 — 이미 시드된 `ACCOUNT:CREATE`(학원 직원 계정 발급, 아직 구현 API 없음)와 같은 리소스. `V4.1.3` 마이그레이션으로 시드.
- **`RoleRepository.updateNameAndDescription()`/`AcademyRepository.assignUser()`와 동일한 관리 엔티티 직접 mutate 패턴을 재사용.** `UserEntity.changeRole(Long roleId)` package-private mutator를 추가하고, `UserRepositoryImpl.changeRole()`이 managed entity를 로드해 mutator만 호출한다(`save()` 호출 없이 트랜잭션 커밋 시점 dirty-checking으로 반영). `User` 도메인 객체에 불변 with-copy 메서드(`withRoleId`)는 추가하지 않았다 — 실제 수정 흐름이 이 메서드를 거치지 않아 아무도 안 부르는 죽은 코드가 될 것이기 때문(`Role.withPermissionCodes()`에서 이미 겪은 실수를 반복하지 않음).
- **검증(대상 계정/역할의 존재·학원 스코프 확인)은 서비스 계층에서 기존 `findById()`(불변 도메인 객체 반환)로 먼저 하고, 통과하면 mutate 메서드를 호출하는 2단계 구조.** 대상 계정 미존재/다른 학원/관리자 계정(MEMBER 아님)은 전부 동일하게 `404 USER_404_1`로 응답 — 다른 학원 계정 존재 여부나 관리자 계정 여부가 노출되지 않도록.
- 이 코드베이스 최초의 "개별 계정 관리" 컨트롤러(`UserController`)를 신설했다 — 지금까지는 로그인/로그아웃/역할·권한 카탈로그 컨트롤러만 있었음.

### 완료 기준

- [x] `permission` 카탈로그에 `ACCOUNT:MANAGE` 추가(`V4.1.3`), 로컬 DB 적용 확인
- [x] `UserEntity.changeRole(roleId)` mutator, `UserRepository.changeRole(userId, roleId)` + `UserRepositoryImpl` 구현
- [x] `ChangeUserRoleCommand`/`ChangeUserRoleUseCase`/`ChangeUserRoleService`(TDD, 6케이스: 대상 미존재/다른 학원/비MEMBER, 역할 미존재/다른 학원, 정상 변경)
- [x] `UserController` 신설, `PATCH /api/users/{userId}/role` 핸들러 추가(`ACCOUNT:MANAGE` 필요)
- [x] 로컬 curl/DB로 end-to-end 검증(정상 변경 204 + DB 반영 확인, 대상 계정 다른 학원 404, 역할 미존재/다른 학원 404, 권한 없는 계정 403)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `UserRepository`에 `changeRole(Long userId, Long roleId)` 추가 |
| Persistence(users) | `UserEntity.changeRole(Long roleId)` mutator 추가, `UserRepositoryImpl.changeRole` 구현 |
| Application(users) | `ChangeUserRoleCommand`/`ChangeUserRoleUseCase`/`ChangeUserRoleService` 신규 |
| Presentation(users) | `UserController`(신규, `PATCH /{userId}/role`), `ChangeUserRoleRequest` 신규 |
| Migration | `V4.1.3`(`permission` 테이블에 `ACCOUNT:MANAGE` 시드) |

---

## ✅ 2026-08-08 · 학원 구성원 검색 API 구현 (`GET /api/users?keyword=`, 이슈 #218)

### 배경

`POST /api/workspaces/{workspaceId}/members`(워크스페이스 참여자 추가), `POST /api/workspaces`(워크스페이스 생성), 채팅방 생성(`CreateChatRoomRequest.participantIds`)이 전부 `Long` userId 배열을 직접 받는데, 프론트가 그 userId를 알아낼 방법이 `users` 도메인에 전혀 없었다. 근태 도메인의 `GET /api/attendance/employees/weekly?keyword=`가 이름 검색을 지원하긴 하지만 `ATTENDANCE:READ` 권한이 필요해서, 워크스페이스/채팅방을 만들려는 일반 직원이 그 권한을 갖고 있다는 보장이 없었다. 설계: `docs/superpowers/specs/2026-08-08-user-search-design.md`.

### 확정된 정책

- **권한 체크 없음.** 이 API를 쓰는 워크스페이스 생성/채팅방 생성 둘 다 현재 `@PreAuthorize`가 안 걸려있는 상태(둘 다 TODO)라, 검색만 특정 권한으로 묶으면 그 권한 없는 사람이 워크스페이스/채팅방은 만들 수 있는데 상대를 검색은 못 하는 모순이 생긴다. 로그인만 되면 호출 가능하게 뒀다.
- **`accountType` 무관하게 전체 포함**(일반 직원 + 학원 관리자) — 원장도 워크스페이스/채팅방에 참여할 수 있어야 한다.
- **`status = ACTIVE`인 계정만** 검색 대상 — 퇴사자를 새 워크스페이스/채팅방에 넣을 이유가 없다.
- `keyword`가 없거나 빈 문자열이면 전체 목록을 반환한다 — 근태 주간 조회의 keyword 처리 방식(`keyword == null ? "" : keyword.trim()`)과 동일한 패턴.
- 페이지네이션 없음 — 학원 하나의 구성원 수가 많지 않아 기존 `GET /api/roles`와 동일한 전례를 따른다.
- 응답에 `username`도 함께 내려준다 — 동명이인 구분용(이름만으로는 같은 사람인지 구분 못 하는 경우 방지).

### 완료 기준

- [x] `UserJpaRepository`에 검색용 파생 쿼리 2개 추가(`findAllByAcademyIdAndStatusAndNameContainingIgnoreCase`, `findAllByAcademyIdAndStatus`)
- [x] `UserRepository.searchByAcademyId(academyId, keyword)` + `UserRepositoryImpl` 구현
- [x] `SearchUsersUseCase`/`SearchUsersService`(TDD, 3케이스: 키워드 매칭/키워드 없음(전체)/매칭 없음)
- [x] `UserResponseCode.USER_SEARCHED`(`USER_200_3`), `UserSearchResponse` 추가
- [x] `UserController`에 `GET /api/users?keyword=` 핸들러 추가(권한 체크 없음)
- [x] 로컬 curl/DB로 end-to-end 검증(권한 없이 200 확인, 키워드 검색/전체 조회, 다른 학원 제외, `ACTIVE`만, `ADMIN`/`MEMBER` 둘 다 포함)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `UserRepository`에 `searchByAcademyId(Long academyId, String keyword)` 추가 |
| Persistence(users) | `UserJpaRepository`에 검색용 파생 쿼리 2개 추가, `UserRepositoryImpl.searchByAcademyId` 구현 |
| Application(users) | `SearchUsersUseCase`/`SearchUsersService` 신규 |
| Presentation(users) | `UserController`에 `GET` 핸들러 추가, `UserResponseCode.USER_SEARCHED`(`USER_200_3`), `UserSearchResponse` 신규 |

---

## ✅ 2026-08-08 · 역할 색상(color) + 인원수(memberCount) 추가 (이슈 #226)

### 배경

프론트 역할 설정 화면에 color(뱃지 색상)와 memberCount(역할별 인원수)가 있는데 백엔드 `role` 테이블/API엔 둘 다 없던 기존 갭. 역할 CRUD가 끝나면 사용자에게 상기하기로 해뒀던 항목을 이번에 함께 처리했다. 설계: `docs/superpowers/specs/2026-08-08-role-color-design.md`.

### 확정된 정책

- **`color`는 `name`/`description`과 완전히 동일한 경로로 흐른다.** 역할 생성/수정 요청에서 받고, 목록/상세 응답에 그대로 내려준다. 형식 검증(hex 등)은 하지 않는다 — 팀 결정으로 프론트 책임.
- **`Role.create()`/`Role.restore()`는 기존 6-인자 시그니처를 유지한 채 7-인자(color 포함) 오버로드를 추가하는 방식으로 확장했다.** 코드베이스에 이 두 메서드 호출부가 11곳 있었는데(대부분 테스트), 전부 고치는 대신 이전에 `AuthUser`가 같은 이유로 delegating 생성자를 쓴 전례를 그대로 따라 기존 호출부를 안 건드렸다.
- **`memberCount`는 `role` 테이블 컬럼이 아니라 매 조회 시 `users` 테이블에서 계산하는 파생값이다.** `status = ACTIVE`인 구성원만 센다(역할 삭제 정책의 `existsActiveByRoleId`와 동일한 기준). 인원 배정이 바뀔 때마다 캐시를 동기화해야 하는 컬럼을 만드는 대신, 학원 규모상 매번 계산해도 부담 없다고 판단했다.
- **`memberCount`는 `Role` 도메인 모델에 넣지 않았다.** 다른 애그리거트(User)의 파생 통계를 순수 도메인 객체에 섞으면 `updatePermissions()` 등 count와 무관한 흐름까지 불필요하게 count 조회를 끌고 다니게 된다. 대신 attendance 도메인의 `WeeklyEmployeeAttendanceView`와 같은 패턴으로 응용 계층에 `RoleView(Role role, long memberCount)`를 신설해 `ListRolesUseCase`/`GetRoleUseCase`가 이걸 반환하도록 바꿨다.
- **`UserRepository.countActiveByRoleIds(Set<Long>)`는 역할 여러 개를 한 번에 집계하는 배치 쿼리다.** 역할 목록 조회에서 역할 N개마다 개별 쿼리를 날리는 N+1을 피하기 위해, GROUP BY로 한 번에 가져와 `Map<Long, Long>`으로 변환한다. 상세 조회(역할 하나)도 같은 메서드를 `Set.of(roleId)`로 호출해서 재사용한다.

### 완료 기준

- [x] `role.color` 컬럼 마이그레이션(`V4.1.4`) 적용
- [x] `Role` 도메인 모델에 `color` + 7-인자 오버로드 추가(6-인자 하위호환 유지)
- [x] `RoleEntity`/`RoleRepository`/`RoleRepositoryImpl`에 `color` 배관(`updateNameAndDescription` 4-인자로 확장)
- [x] `UserRepository.countActiveByRoleIds` 추가(TDD, N+1 없이 배치 집계)
- [x] `RoleView` 신설, `ListRolesUseCase`/`GetRoleUseCase` 반환 타입을 `RoleView` 기반으로 변경(TDD)
- [x] 역할 생성/수정 API에 `color` 추가(TDD)
- [x] `RoleListResponse`/`RoleDetailResponse`에 `color`/`memberCount` 반영
- [x] 로컬 curl로 end-to-end 검증(생성/목록/상세/수정 전부, 역할 배정 후 memberCount 실제 증가 확인)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `Role`에 `color` 필드 + 7-인자 `create`/`restore` 오버로드 추가. `UserRepository`에 `countActiveByRoleIds(Set<Long>)` 추가 |
| Persistence(users) | `RoleEntity`에 `color` 컬럼 + `update()` 3-인자 확장. `RoleJpaRepository`는 무변경, `RoleRepositoryImpl`에 `color` 배관. `UserJpaRepository`에 집계 쿼리 + `RoleMemberCountRow` 프로젝션 신규, `UserRepositoryImpl.countActiveByRoleIds` 구현 |
| Application(users) | `RoleView` 신규(`application/query`). `ListRolesUseCase`/`GetRoleUseCase` 반환 타입을 `RoleView` 기반으로 변경. `CreateRoleCommand`/`UpdateRoleCommand`에 `color` 추가 |
| Presentation(users) | `CreateRoleRequest`/`UpdateRoleRequest`에 `color` 추가. `RoleListResponse`/`RoleDetailResponse`에 `color`/`memberCount` 추가(`from(RoleView)`로 팩토리 시그니처 변경, `RoleController` 자체는 무변경) |
| Migration | `V4.1.4`(`role` 테이블에 `color VARCHAR(20) NULL` 컬럼 추가) |

---

## ✅ 2026-08-09 · PR #231 CodeRabbit 피드백 반영 + `users` 도메인 로깅 컨벤션 전면 적용

### 배경

두 가지 독립 작업을 사용자 요청으로 하나의 PR에 묶었다. (1) 역할 색상/인원수 PR(#231)에 CodeRabbit이 남긴 6건 중, 검토 결과 기술적으로 타당한 4건(`superpowers:receiving-code-review` 기준으로 codebase 컨벤션과 대조해 검증). (2) `docs/LOGGING_CONVENTION.md`(Service 구현체의 public 메서드 대상 비즈니스 이벤트 로깅 컨벤션)가 문서만 만들어지고 실제로는 한 곳에도 적용되지 않은 상태였는데, `users` 도메인 Service 클래스 17개 전부에 소급 적용했다.

### 확정된 정책 — CodeRabbit 피드백 반영

- **`color`에 `@Size(max = 20)` 검증을 추가했다.** `role.color` 컬럼이 `VARCHAR(20)`인데 `name`/`description`엔 이미 있던 길이 검증이 `color`에만 빠져있던 불일치였다 — 신규 요구사항이 아니라 기존 컨벤션 누락을 맞춘 것.
- **`UserRepositoryImplDataJpaTest`에 `countActiveByRoleIds` 실 DB 검증 테스트를 추가했다.** 기존엔 Mockito 목 기반 단위 테스트만 있어서, GROUP BY 집계 쿼리가 실제 DB에서 의도대로 동작하는지 확인하는 테스트가 없었다.
- **`CreateRoleServiceTest`가 `color` 값이 실제로 전달되는지 검증하지 않고 있었다.** 목 `save()`의 `thenAnswer`에서 반환값만 만들고 인자로 들어온 `color`는 확인하지 않아, 필드가 누락돼도 테스트가 통과하는 상태였다 — assertion을 추가했다.
- **`UserRepositoryImplTest`의 빈 `Set` 케이스에 `verifyNoInteractions(jpaRepository)`를 추가했다.** 빈 입력일 때 리포지토리를 아예 호출하지 않는 얼리 리턴 경로인데, 이를 검증하는 assertion이 없었다.
- **6건 중 이 4건만 반영했다** — 나머지 2건은 이미 팀의 기존 컨벤션(짐짝 전례)과 일치하거나 이 PR 범위를 넘는 설계 변경이라 판단해 제외했다.

### 확정된 정책 — 로깅 컨벤션 소급 적용

- **`PerformanceLogAspect`(AOP, 실행시간 측정)와는 별개다.** 기존에 이미 있던 이 Aspect는 `get*`/`find*` 메서드의 실행 시간만 재는 순수 성능 로깅이고, `LOGGING_CONVENTION.md`가 요구하는 "비즈니스 이벤트 로깅"(`event=<도메인>_<행위>_시작/완료/실패`)과는 목적이 다르다. 이벤트명·파라미터가 서비스마다 도메인 특화라 AOP로 자동화할 수 없어, 17개 Service 클래스 각각에 수동으로 적용했다.
- **예외를 던지는 메서드만 `try/catch` + `_실패` 로그를 추가했다.** 순수 조회(예: `ListRolesService`, `SearchUsersService`, `UserDirectoryService`, `ListAcademyApplicationsService`)는 `_시작`/`_완료`만 두르고 `try/catch`를 넣지 않았다 — 존재하지 않는 실패 경로에 대한 방어 코드를 만들지 않는다는 원칙.
- **비밀번호·토큰류는 어떤 로그에도 남기지 않는다.** 기존에 `LoginCommand.toString()` 마스킹으로 확립된 원칙을 로그 문에도 동일하게 적용했다: `LoginService`는 `username`만, `RefreshService`는 refreshToken 값 자체를 남기지 않는다. `ApproveAcademyApplicationService`는 학원 승인 시 발급하는 임시 비밀번호(`temporaryPassword`)를 완료 로그에도 포함하지 않고, 대신 결과를 나타내는 값으로 `academyId`/`userId`를 남긴다.
- **이벤트명 접두어는 `<도메인>_<행위>` 스킴을 서비스 17개 전체에 일관 적용했다**: `auth_login`/`auth_logout`/`auth_token_reissue`(인증 3개), `role_create`/`role_list`/`role_get`/`role_update`/`role_delete`(역할 CRUD 5개), `role_permission_assign`/`permission_catalog_list`/`user_role_change`/`user_search`/`user_directory_find_active_ids`(권한·사용자 5개), `academy_application_list`/`academy_application_get`/`academy_application_approve`/`academy_application_reject`(학원 신청 4개).

### 완료 기준

- [x] CodeRabbit 피드백 4건 반영(`@Size` 검증, DataJpaTest 추가, 목 검증 보강 2건)
- [x] 인증 서비스 3개(`LoginService`/`LogoutService`/`RefreshService`)에 로깅 적용
- [x] 역할 CRUD 서비스 5개(`CreateRoleService`/`ListRolesService`/`GetRoleService`/`UpdateRoleService`/`DeleteRoleService`)에 로깅 적용
- [x] 권한·사용자 서비스 5개(`AssignRolePermissionsService`/`PermissionQueryService`/`ChangeUserRoleService`/`SearchUsersService`/`UserDirectoryService`)에 로깅 적용
- [x] 학원 신청 서비스 4개(`ListAcademyApplicationsService`/`GetAcademyApplicationService`/`ApproveAcademyApplicationService`/`RejectAcademyApplicationService`)에 로깅 적용
- [x] `./gradlew build` 통과(전체 테스트 포함)

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Presentation(users) | `CreateRoleRequest`/`UpdateRoleRequest`에 `color` `@Size(max = 20)` 추가 |
| Application(users) | Service 클래스 17개에 `@Slf4j` + `LOGGING_CONVENTION.md` 기준 이벤트 로그 추가(코드 로직 변경 없음) |
| Test(users) | `UserRepositoryImplDataJpaTest`(`countActiveByRoleIds` 실 DB 케이스 2건), `CreateRoleServiceTest`(`color` 값 검증), `UserRepositoryImplTest`(`verifyNoInteractions` 추가) |
| Docs(users) | `CHANGELOG.md`/`API.md`의 `color` 필드 설명에 20자 제한 명시 |

---

## ✅ 2026-08-09 · 직원 계정 발급 API 구현 (`POST /api/users`, 이슈 #267, "계정 발급 체계" 3단계)

### 배경

계정 발급 체계는 3단계로 나뉜다: 학원 자체 신청(1단계) → SUPER ADMIN 승인 시 원장 계정 자동 발급(2단계) → **원장이 자기 학원 소속 직원 계정을 직접 발급(3단계, 이번 작업)**. 역할 관리 API 7개가 이미 갖춰져있어 계정 생성 시 역할 배정이 가능한 기반은 마련된 상태였다. 설계: `docs/superpowers/specs/2026-08-09-employee-account-creation-design.md`.

### 확정된 정책

- **권한 코드는 `ACCOUNT:MANAGE`를 재사용한다.** 베이스라인부터 시드돼있던 `ACCOUNT:CREATE`는 8/8일 정한 "관련 행위는 하나의 코드로 묶는다" 원칙에 따라 같은 리소스로 판단해 이번에 새로 쓰지 않았다(카탈로그에는 남아있으나 미사용).
- **`roleId`는 생성 요청에서 필수다.** 직원 계정은 만들 때부터 역할이 있어야 실제 업무(권한 체크)가 가능하다. 원장 계정(role_id=null)과는 다른 정책이다.
- **원장 계정 발급과 직원 계정 발급이 임시 비밀번호 생성 로직을 공유하도록 `AccountIssuer`(`application/service/support/AccountIssuer.java`)로 뽑았다.** 기존 `ApproveAcademyApplicationService`가 직접 갖고 있던 `generateTemporaryPassword()`+`User.create()`+저장 로직을 이 협력 객체로 옮기고, 두 서비스가 함께 호출한다. `User.create()`도 이참에 `roleId` 파라미터를 받도록 확장했다(호출부가 이 리팩터링 하나뿐이라 오버로드 없이 시그니처를 직접 바꿈).
- **username 중복은 사전 체크 + DB 유니크 제약 백스톱 이중 방어다.** `UserRepository.existsByUsername()`으로 먼저 확인하고, `UserRepositoryImpl.save()`가 `saveAndFlush()` + `uk_users_username` 위반 캐치로 `UsernameDuplicateException`(`USER_409_6`) 변환까지 방어한다 — 역할 이름 중복 방어(`RoleRepositoryImpl.save()`)와 동일한 패턴.
- **이메일 발송은 이번 스코프에서 제외했다.** 코드베이스에 메일 발송 인프라가 전혀 없다(`spring-boot-starter-mail` 의존성은 주석 처리된 채 미사용). 이번엔 원장 계정 발급과 동일하게 API 응답으로 임시 비밀번호를 그대로 반환하고, 학원 관리자가 직접 전달한다.
- **`mustChangePw` 로그인 강제 로직도 이번 스코프에서 제외했다.** 이 필드는 도메인 모델/DB에 저장은 되지만, `LoginService`를 포함해 어디서도 읽거나 강제하지 않는 상태다. 로그인 흐름 연동은 별도 작업으로 미룬다.
- **원장 계정 신청(`SubmitAcademyApplicationService`)의 `requestedLoginId` 중복 확인도 이번 스코프에서 제외했다.** 이번에 만든 `existsByUsername`/`UsernameDuplicateException` 패턴을 그대로 재사용할 수 있을 것으로 보이나 별도 작업으로 남겨둔다.

### 완료 기준

- [x] `User.create()`에 `roleId` 파라미터 추가
- [x] `AccountIssuer`/`IssuedAccount` 신규, `ApproveAcademyApplicationService`를 `AccountIssuer` 사용으로 리팩터링(테스트 갱신 포함)
- [x] `UserRepository.existsByUsername` 추가, `UserErrorCode.USERNAME_DUPLICATE`(`USER_409_6`) + `UsernameDuplicateException` 신규, `UserRepositoryImpl.save()` DB 백스톱(TDD)
- [x] `CreateAccountCommand`/`CreateAccountUseCase`/`CreateAccountService`(TDD, 4케이스: username 중복/roleId 없음/roleId 다른 학원/정상 생성)
- [x] `UserController`에 `POST /api/users` 핸들러 추가, `CreateAccountRequest`/`AccountCreateResponse`, `UserResponseCode.ACCOUNT_CREATED`(`USER_201_1`)
- [x] 로컬 curl end-to-end 검증(정상 생성 201, username 중복 409, roleId 없음 404, 인증 없음 401, 권한 없음 403)
- [x] `./gradlew build` 통과

### 🧩 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain(users) | `User.create()`에 `roleId` 파라미터 추가. `UserRepository`에 `existsByUsername` 추가. `UserErrorCode.USERNAME_DUPLICATE`(`USER_409_6`), `UsernameDuplicateException` 신규 |
| Application(users) | `application/service/support/AccountIssuer`(신규 협력 객체)·`IssuedAccount`(신규 record). `CreateAccountCommand`/`CreateAccountUseCase`/`CreateAccountService` 신규. `ApproveAcademyApplicationService`를 `AccountIssuer` 사용으로 리팩터링(생성자 시그니처 변경: `PasswordEncoder` 제거, `AccountIssuer` 추가) |
| Persistence(users) | `UserJpaRepository.existsByUsername` 추가. `UserRepositoryImpl.save()`가 `saveAndFlush()` + `uk_users_username` 위반 캐치로 `UsernameDuplicateException` 변환 |
| Presentation(users) | `UserController`에 `POST /api/users` 핸들러 추가(`ACCOUNT:MANAGE` 필요). `CreateAccountRequest`, `AccountCreateResponse`, `UserResponseCode.ACCOUNT_CREATED`(`USER_201_1`) 신규 |
| Migration | 없음(기존 `roleId`/`role` FK, `uk_users_username` 제약 재사용) |

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
- [x] 계정 발급(회원가입, 원장이 하위 직원 계정 생성) API — `POST /api/users` 추가로 완료(이메일 발송/`mustChangePw` 로그인 연동은 후속 작업)
- [x] 로그아웃 API — `POST /api/auth/logout` 추가, `TokenRevokerUseCase`로 refreshToken 삭제
- [x] 역할 생성 API — `POST /api/roles` 추가, DB 유니크 제약 백스톱 포함
- [x] 권한 카탈로그 조회 API — `GET /api/permissions` 추가
- [x] 역할 권한 조립 API — `PUT /api/roles/{roleId}/permissions` 추가(전체 교체 방식), 존재하지 않는 코드 400/역할 없음·다른 학원 404
- [x] 역할 목록/상세/수정/삭제 API — `GET /api/roles`, `GET /api/roles/{roleId}`, `PUT /api/roles/{roleId}`, `DELETE /api/roles/{roleId}` 추가로 역할 관리 API 7개 완성
- [x] `academy` 테이블 생성됨(다른 팀원, `V2.1.2`~`V2.1.4`) — `role.academy_id`, `users.academy_id` 모두 FK 연결 완료(`V4.1.3`)

## 📌 후속 문서

완료된 변경의 사용자 관점 요약은 [CHANGELOG.md](CHANGELOG.md)에서 확인할 수 있다.
