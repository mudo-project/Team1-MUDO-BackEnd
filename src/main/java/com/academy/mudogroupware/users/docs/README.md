# users 모듈

계정, 로그인, 역할, 권한, 사용자 상태를 담당한다.

## 책임과 범위

- `User`: 학원 소속 직원 계정. `academyId`, `name`, `roleId`, `status`를 가진다.
- `Role`: 학원별 역할.
- `Permission`: 시스템 권한 코드.
- `RolePermission`: 역할과 권한의 연결.
- `AcademyApplication`: 학원 신청서. 신청 접수 API는 아직 없고(파일 업로드 인프라 선행 필요), 현재는 SUPER ADMIN의 목록/상세 조회만 가능하다. 승인/반려는 후속 PR에서 추가된다.

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
- `ListAcademyApplicationsUseCase`
- `GetAcademyApplicationUseCase`

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
- `admin_scope=ACADEMY`(학원 관리자)는 컬럼만 존재하고 아직 미사용(학원 신청 승인 API가 추가되는 후속 PR에서 도입 예정).
- 학원 신청 목록/상세 조회(`GET /api/academy-applications`, `GET /api/academy-applications/{applicationId}`)는 이 코드베이스에서 처음으로 `@PreAuthorize` 대신 `SecurityConfig` 필터체인의 URL 매칭(`PLATFORM:SUPER_ADMIN` authority)으로 막는다 — SUPER ADMIN인지 아닌지 하나만 갈리고 그 안에서 세분화된 권한 차이가 없기 때문.

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
