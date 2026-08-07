# users 모듈

계정, 로그인, 역할, 권한, 사용자 상태를 담당한다.

## 책임과 범위

- `User`: 학원 소속 직원 계정. `academyId`, `name`, `roleId`, `status`를 가진다.
- `Role`: 학원별 역할.
- `Permission`: 시스템 권한 코드.
- `RolePermission`: 역할과 권한의 연결.
- `AcademyApplication`: 학원 신청서. 신청 접수 API는 아직 없다(파일 업로드 인프라 선행 필요). SUPER ADMIN이 목록/상세 조회, 승인/반려까지 처리할 수 있다. 승인 시 `Academy`와 최초 관리자 `User`가 함께 발급된다.
- `Academy`: 학원. `attendance` 모듈도 Wi-Fi IP 기능 전용으로 좁게 매핑한 별도 엔티티(`AcademyJpaEntity`)를 갖고 있으나, 이름/사업자번호/상태를 포함한 전체 소유권은 `users`가 갖는다.

## 공개 UseCase

- `LoginUseCase`
- `RefreshUseCase`
- `LogoutUseCase`
- `CreateRoleUseCase`
- `AssignRolePermissionsUseCase`
- `PermissionQueryUseCase`
- `ListAcademyApplicationsUseCase`
- `GetAcademyApplicationUseCase`
- `ApproveAcademyApplicationUseCase`
- `RejectAcademyApplicationUseCase`

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

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
