# users 모듈

계정, 로그인, 역할, 권한, 사용자 상태를 담당한다.

## 책임과 범위

- `User`: 학원 소속 직원 계정. `academyId`, `name`, `roleId`, `status`를 가진다.
- `Role`: 학원별 역할.
- `Permission`: 시스템 권한 코드.
- `RolePermission`: 역할과 권한의 연결.
- `AcademyApplication`: 학원 신청서. 접수(`POST`, 공개 API)는 학원명·대표자 정보·플랜만 받는 최소 스코프이고(사업자등록증 검증 등은 아직 없음), SUPER ADMIN이 목록/상세 조회, 승인/반려까지 처리할 수 있다. 승인 시 `Academy`와 최초 관리자 `User`가 함께 발급된다.
- `Academy`: 학원. `attendance` 모듈도 Wi-Fi IP 기능 전용으로 좁게 매핑한 별도 엔티티(`AcademyJpaEntity`)를 갖고 있으나, 이름/사업자번호/상태를 포함한 전체 소유권은 `users`가 갖는다.

## 공개 UseCase

- `LoginUseCase`
- `RefreshUseCase`
- `LogoutUseCase`
- `CreateRoleUseCase`
- `AssignRolePermissionsUseCase`
- `PermissionQueryUseCase`
- `ListRolesUseCase`
- `GetRoleUseCase`
- `UpdateRoleUseCase`
- `DeleteRoleUseCase`
- `SubmitAcademyApplicationUseCase`
- `ListAcademyApplicationsUseCase`
- `GetAcademyApplicationUseCase`
- `ApproveAcademyApplicationUseCase`
- `RejectAcademyApplicationUseCase`
- `ChangeUserRoleUseCase`
- `SearchUsersUseCase`
- `CreateAccountUseCase`
- `PasswordSetupUseCase`

## 다른 모듈에 제공하는 Adapter

- `RolePermissionLookupAdapter`: global security의 `RolePermissionLookupPort`를 구현한다.
- `WorkspaceUserInfoAdapter`: workspace의 `WorkspaceUserInfoPort`를 구현한다.
- `LectureTeacherDirectoryAdapter`: lecture의 `TeacherDirectoryPort`를 구현한다. 강의 목록/상세와 student 수강 강의 목록에서 `teacherName`을 표시할 때 사용한다.
- `ApprovalApproverDirectoryAdapter`: approval의 `ApproverDirectoryPort`를 구현한다. 결재자/작성자 이름과 academyId를 조회할 때 사용한다.

## 상태 정책

- `ACTIVE`: 로그인 가능.
- `RESIGNED`: 로그인 제한.
- `INACTIVE`: 로그인 제한.

강의의 기존 담당 선생님명 표시나 결재 이력 표시처럼 과거 데이터의 이름을 보여주는 조회는 상태와 관계없이 academyId 범위 안에서 이름을 반환한다. 초대/신규 배정 단계에서 ACTIVE만 허용할지는 각 기능 정책에서 별도로 결정한다.

## 주의사항

- 다른 모듈은 users Entity/Repository/JPA를 직접 참조하지 않는다.
- 필요한 조회는 소비 모듈이 Port를 정의하고 users infrastructure Adapter가 구현한다.
- notice에는 아직 users 직접 조회 shim이 남아 있을 수 있으며 별도 notice 범위에서 교체한다.
- `account_type=ADMIN`+`admin_scope=PLATFORM` 계정은 역할 없이도 모든 권한 카탈로그를 authority로 부여받는다(`PLATFORM:SUPER_ADMIN` 합성 authority도 추가로 받음).
- `admin_scope=ACADEMY`(학원 관리자)는 학원 신청 승인 시점에 실제로 발급된다. 승인 시점에 `AccountType.ADMIN`+`AdminScope.ACADEMY`로 생성되며, 세부 권한은 여전히 기존 역할/권한 카탈로그로 체크한다.
- 학원 신청 목록/상세 조회·승인·반려(`/api/academy-applications`의 GET·approve·reject)는 이 코드베이스에서 처음으로 `@PreAuthorize` 대신 `SecurityConfig` 필터체인의 URL 매칭(`PLATFORM:SUPER_ADMIN` authority)으로 막는다 — SUPER ADMIN인지 아닌지 하나만 갈리고 그 안에서 세분화된 권한 차이가 없기 때문.
- 계정 발급 체계 3단계(학원 관리자의 직원 계정 발급, `POST /api/users`)까지 완료됐다. 원장 계정 발급(`ApproveAcademyApplicationService`)과 임시 비밀번호 생성 로직을 공유하는 `AccountIssuer` 협력 객체가 `application/service/support`에 있다. `AccountIssuer`는 임시 비밀번호를 응답에 직접 노출하지 않고 `PasswordSetupLinkBuilder`로 만든 비밀번호 설정 링크(`POST /api/users/password-setup`)를 반환한다 — `mustChangePw`는 로그인 흐름에서 다른 API를 막는 강제 로직으로 쓰지 않고, 이 최초 설정 완료 여부만 나타내는 플래그로 쓴다(강제 로직을 안 만들기로 한 배경은 `REVISION.md` 참고). 원장 계정은 승인 시 자동 생성되는 "원장" 역할(그 시점 권한 카탈로그 전체 보유)로 발급되어 로그인 즉시 모든 기능을 쓸 수 있다. 이메일 발송, 사업자등록증 검증(OCR·국세청 진위확인 API)·악의적 공격 방어(rate limit)는 아직 없다(후속 작업, 상세 배경은 `REVISION.md` 참고).
- `POST /api/academy-applications`(신청 접수)는 계정 없는 학원이 호출하므로 `SecurityConfig`에서 `permitAll`이다 — 같은 리소스의 나머지 4개 엔드포인트(GET 목록/상세·approve·reject)는 여전히 `PLATFORM:SUPER_ADMIN` 필요.
- 학원 신청 접수는 `requestedLoginId`를 접수 시점에 중복확인한다(발급된 계정 + 대기중/승인된 다른 신청서 대상, 반려된 신청서는 제외). DB에는 상태별 조건부 유니크 제약(생성 컬럼 `requested_login_id_active` + `uk_academy_application_requested_login_id_active`)을 함께 걸어 동시 접수 레이스를 막는다.
- `GET /api/users/members`(관리자용 구성원 목록 조회)는 기존 `GET /api/users`(학원 구성원 검색)와 완전히 별개의 엔드포인트다 — 후자는 워크스페이스/채팅방 멤버 선택용으로 권한 없이 호출 가능하도록 의도적으로 최소 필드만 반환하므로, 여기에 연락처·이메일 등을 추가하지 않는다. 오늘 근태 상태(`attendanceStatus`)는 `users`가 정의한 `TodayAttendanceStatusPort`를 `attendance` 도메인이 구현해 내려준다 — 주간/월간 근태 이력이 필요하면 여전히 `GET /api/attendance/employees/weekly`를 별도 호출해야 한다.

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
