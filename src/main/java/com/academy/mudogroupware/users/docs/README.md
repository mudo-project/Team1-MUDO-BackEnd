# users 모듈

계정, 로그인, 역할, 권한, 사용자 상태를 담당한다.

## 책임과 범위

- `User`: 직원 계정. `name`, `roleId`, `status`를 가진다(`academyId`는 Phase 2에서 제거했다 — 아래 "academyId 제거" 참고).
- `Role`: 시스템 전체 역할(더 이상 학원별로 구분하지 않는다).
- `Permission`: 시스템 권한 코드.
- `RolePermission`: 역할과 권한의 연결.

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
- `ChangeUserRoleUseCase`
- `SearchUsersUseCase`
- `CreateAccountUseCase`
- `PasswordSetupUseCase`

## 다른 모듈에 제공하는 Adapter

- `RolePermissionLookupAdapter`: global security의 `RolePermissionLookupPort`를 구현한다.
- `WorkspaceUserInfoAdapter`: workspace의 `WorkspaceUserInfoPort`를 구현한다.
- `LectureTeacherDirectoryAdapter`: lecture의 `TeacherDirectoryPort`를 구현한다. 강의 목록/상세와 student 수강 강의 목록에서 `teacherName`을 표시할 때 사용한다.
- `ApprovalApproverDirectoryAdapter`: approval의 `ApproverDirectoryPort`를 구현한다. 결재자/작성자 이름을 조회할 때 사용한다.

## 상태 정책

- `ACTIVE`: 로그인 가능.
- `RESIGNED`: 로그인 제한.
- `INACTIVE`: 로그인 제한.

강의의 기존 담당 선생님명 표시나 결재 이력 표시처럼 과거 데이터의 이름을 보여주는 조회는 상태와 관계없이 이름을 반환한다. 초대/신규 배정 단계에서 ACTIVE만 허용할지는 각 기능 정책에서 별도로 결정한다.

## 주의사항

- 다른 모듈은 users Entity/Repository/JPA를 직접 참조하지 않는다.
- 필요한 조회는 소비 모듈이 Port를 정의하고 users infrastructure Adapter가 구현한다.
- notice에는 아직 users 직접 조회 shim이 남아 있을 수 있으며 별도 notice 범위에서 교체한다.
- `account_type=ADMIN`+`admin_scope=PLATFORM` 계정은 역할 없이도 모든 권한 카탈로그를 authority로 부여받는다(`PLATFORM:SUPER_ADMIN` 합성 authority도 추가로 받음).
- `admin_scope=ACADEMY`(학원 관리자) 계정을 만드는 API는 없다. 학원 신청/승인 플로우가 폐기됐고, `POST /api/users`(`CreateAccountService`)는 `AccountType.MEMBER`만 발급하며 그 호출 자체에도 `ACCOUNT:MANAGE` 권한(이미 관리자 계정이 있어야 함)이 필요하다. 따라서 새로 배포한 서버의 최초 학원 관리자 계정은 앱 API를 거치지 않고 DB에 직접 계정·역할·권한을 심는 수동 SQL 작업으로 만든다(배경은 `REVISION.md` 참고).
- 계정 발급 체계 3단계(학원 관리자의 직원 계정 발급, `POST /api/users`)까지 완료됐다. 임시 비밀번호 생성 로직을 담당하는 `AccountIssuer` 협력 객체가 `application/service/support`에 있다. `AccountIssuer`는 임시 비밀번호를 응답에 직접 노출하지 않고 `PasswordSetupLinkBuilder`로 만든 비밀번호 설정 링크(`POST /api/users/password-setup`)를 반환한다 — `mustChangePw`는 로그인 흐름에서 다른 API를 막는 강제 로직으로 쓰지 않고, 이 최초 설정 완료 여부만 나타내는 플래그로 쓴다(강제 로직을 안 만들기로 한 배경은 `REVISION.md` 참고). 이메일 발송 등은 아직 없다(후속 작업, 상세 배경은 `REVISION.md` 참고).
- `GET /api/users/members`(관리자용 구성원 목록 조회)는 기존 `GET /api/users`(학원 구성원 검색)와 완전히 별개의 엔드포인트다 — 후자는 워크스페이스/채팅방 멤버 선택용으로 권한 없이 호출 가능하도록 의도적으로 최소 필드만 반환하므로, 여기에 연락처·이메일 등을 추가하지 않는다. 오늘 근태 상태(`attendanceStatus`)는 `users`가 정의한 `TodayAttendanceStatusPort`를 `attendance` 도메인이 구현해 내려준다 — 주간/월간 근태 이력이 필요하면 여전히 `GET /api/attendance/employees/weekly`를 별도 호출해야 한다.
- `GET /api/users/me`, `PATCH /api/users/me`, `PATCH /api/users/me/password`는 로그인만 되어 있으면 누구나 자기 자신에 대해 호출 가능하다(권한 코드 불필요). `PATCH /api/users/me`는 `phone`/`email`만 수정 가능하고, 값을 보내지 않은 필드는 기존 값을 유지하는 부분 수정이다. 이 API들이 재사용하는 `GetUserDetailService`/`UpdateUserProfileService`는 관리자용 구성원 상세조회/수정에서도 그대로 재사용한다.
- `GET /api/users/{userId}`(관리자용 구성원 상세조회)는 `ACCOUNT:MANAGE` 권한이 필요하고, `GetUserDetailService`가 `GetMyProfileUseCase`/`GetMemberDetailUseCase` 둘 다 구현하는 방식으로 내 정보 조회와 응답 형태를 그대로 재사용한다. 대상이 없거나 학원 관리자 계정이면 사용자 역할 변경 API와 동일하게 `404 USER_404_1`로 응답해 존재 여부를 숨긴다.
- `PATCH /api/users/{userId}`(관리자용 구성원 정보 수정)는 같은 방식으로 `UpdateUserProfileService`가 `UpdateMyProfileUseCase`/`UpdateMemberProfileUseCase` 둘 다 구현한다. 본인 수정(`PATCH /api/users/me`)과 달리 `name`/`joinedAt`도 바꿀 수 있지만, `roleId`는 이 API로 바꿀 수 없다(역할 변경은 `PATCH /api/users/{userId}/role`을 씁니다) — 역할 변경에는 role 존재 검증이 별도로 필요해서 책임을 분리했다.
- `PATCH /api/users/{userId}/status`(관리자용 구성원 재직 상태 변경)는 `ACTIVE`/`RESIGNED`/`INACTIVE` 양방향 전환을 지원한다(퇴사 처리 후 복직도 가능). 대상 검증은 구성원 상세조회/정보수정과 동일한 패턴이다.
- `PATCH /api/users/me/password`는 `POST /api/users/password-setup`(최초 1회 설정 전용)과 별개다 — 이미 인증된 본인 요청이라 계정 존재 여부를 숨기지 않고, 현재 비밀번호가 틀리면 구체적인 오류 메시지(`USER_400_3`)를 반환한다.
- 계정 생성 시 `phone`/`email`은 선택 입력이다(`V4.1.7`). 비워두면 본인이 나중에 `PATCH /api/users/me`로 채워 넣을 수 있다.
- **academyId 스코핑 제거(Phase 2, `V4.1.8`)**: 실제 배포가 학원마다 별도 EC2·DB 스키마를 쓰는 구조라, 앱 코드 레벨의 academyId 필터링이 불필요해졌다. `User`/`Role` 도메인 모델, JWT 클레임(`JwtClaims`/`AuthUser`/`JwtTokenProvider`/`JwtAuthenticationConverter`), `users`/`role` 리포지토리 전체에서 academyId를 제거했다. `users.academy_id`/`role.academy_id` 컬럼은 DROP하지 않고 nullable로만 바꿨다 — messenger의 `ChatMemberInfoEntity`가 여전히 `users` 테이블의 `academy_id`에 매핑돼 있어서(shim), 지금 컬럼을 지우면 messenger가 깨진다. `UserRepository.findActiveUserIds(academyId, userIds)`는 messenger/workspace가 호출하는 크로스-BC 포트라 시그니처를 그대로 유지했고, 내부적으로는 academyId 파라미터를 무시한다. 상세 배경은 `REVISION.md` 참고.
- **`GET /api/users/members` 번호 기반 페이지네이션(2026-08-12)**: 응답을 공용 `SliceResponse`(`content`/`page`/`size`/`hasNext`)에서 users 도메인 전용 `MemberPageResponse`(`totalElements`/`totalPages`/`first`/`last`/`hasPrevious` 추가)로 바꿨다. 다른 9개 도메인이 같이 쓰는 공용 `PageResult`/`SliceResponse`는 건드리지 않고, `MemberPage`(application 결과)와 `MemberPageResponse`(presentation DTO)를 users 도메인 안에 새로 만들어서 이 엔드포인트에만 적용했다 — 크로스 도메인 컴파일 영향을 없애기 위함. `totalElements`는 이미 인메모리에 올라온 필터링된 리스트의 크기라 추가 DB 조회가 없다. 참고 프로젝트: 로컬 `module03-gymjjak`의 `PageResponse` 패턴(Spring Data `Page<T>` + DB 레벨 `Pageable`/`@Query(countQuery=...)`)을 필드 구성만 참고했고, DB 레벨 전환은 하지 않았다 — 인메모리 페이지네이션 유지 결정은 여전히 유효하다(`REVISION.md` 참고).

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
