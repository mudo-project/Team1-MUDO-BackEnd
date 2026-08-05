# 워크스페이스 생성 API 흐름

## 호출 흐름

```text
POST /api/workspaces
  → Security Filter
  → AuthUser
  → WorkspaceController
  → CreateWorkspaceRequest
  → CreateWorkspaceCommand
  → CreateWorkspaceUseCase
  → WorkspaceService
  → WorkspaceMemberDirectoryPort
  → WorkspaceMemberDirectoryAdapter
  → UserDirectoryUseCase
  → UserDirectoryService
  → UserRepository
  → UserRepositoryImpl
  → UserJpaRepository
  → WorkspaceRepository
  → WorkspacePersistenceAdapter
  → WorkspaceJpaRepository
```

## 1. 인증 정보 추출

`Security Filter`가 Access Token을 검증하고 `AuthUser`를 만든다. `WorkspaceController`는 `AuthUser`에서 `academyId`와 `userId`를 받아 요청 본문에는 없는 학원·생성자 정보를 결정한다.

## 2. 요청 검증과 Command 변환

`CreateWorkspaceRequest`는 이름의 필수 여부·최대 100자와 참여자 번호의 양수를 Bean Validation으로 검증한다. 검증을 통과하면 `CreateWorkspaceCommand`로 변환한다.

## 3. 참여자 확정과 검증

`WorkspaceService`는 추가 참여자 목록을 `LinkedHashSet`으로 정리하고 생성자를 추가한다. 빈 추가 참여자 목록도 허용한다.

`WorkspaceMemberDirectoryPort`는 workspace가 소유한 외부 조회 계약이다. 구현체인 `WorkspaceMemberDirectoryAdapter`는 users의 공개 `UserDirectoryUseCase`를 호출한다. users는 같은 학원이며 `ACTIVE` 상태인 사용자 ID만 반환한다.

요청한 ID 전체가 반환되지 않으면 `InvalidWorkspaceMemberException`을 발생시키고 `WORKSPACE_400_1`로 응답한다.

## 4. 이름 확인과 저장

`WorkspaceService`는 하나의 트랜잭션에서 활성 이름 사용 여부를 조회한다.

- 같은 학원에 이미 존재하는 활성 이름이면 `WorkspaceNameConflictException`을 발생시켜 `WORKSPACE_409_1`을 반환한다.
- 사용 가능한 이름이면 앞뒤 공백을 제거한 이름과 참여자 ID로 `Workspace` Domain Model을 만들고 `WorkspaceRepository`에 전달한다.

## 5. 영속화

`WorkspacePersistenceAdapter`는 MapStruct로 Domain Model을 `WorkspaceJpaEntity`로 변환하고, 각 참여자 ID를 `workspace_member` 엔티티로 추가한다. `saveAndFlush` 후 생성된 워크스페이스 ID를 포함한 Domain Model을 반환한다.

`workspace` 테이블의 `(academy_id, active_name)` unique 제약이 충돌하면 Adapter가 `WorkspaceNameConflictException`으로 변환한다.

## 6. 응답

성공하면 Controller가 `GlobalApiResponse.created`로 HTTP `201 Created`와 `workspaceId`를 반환한다.

## 워크스페이스 목록 조회 API 흐름

```text
GET /api/workspaces?scope=MINE|ALL
  → Security Filter
  → AuthUser / Authorities
  → WorkspaceController @PreAuthorize
  → WorkspaceQueryUseCase
  → WorkspaceQueryService
  → WorkspaceListQueryPort
  → WorkspaceListQueryAdapter
  → WorkspaceJpaRepository
  → WorkspaceListResponse
  → GlobalApiResponse
```

### 1. 인증과 ALL 범위 권한 검사

`WorkspaceController`는 `AuthUser`의 `academyId`, `userId`를 사용한다. `scope`를 생략하면 `MINE`으로 바인딩된다.

현재는 권한 모듈 연동 전이므로 Spring Method Security의 `@PreAuthorize`가 `scope=ALL` 요청을 차단한다. `WORKSPACE:READ_ALL` Authority 연동이 완료되면 해당 권한 보유자만 `ALL`을 조회하도록 확장한다.

### 2. 조회 범위 선택

`WorkspaceQueryService`는 `scope`에 따라 조회 Port를 선택한다.

- `MINE`: 같은 학원의 활성 워크스페이스 중 요청 사용자가 참여한 목록 조회
- `ALL`: 같은 학원의 전체 활성 워크스페이스 목록 조회

두 조회 모두 요청 사용자의 최근 접속 시각 내림차순으로 정렬한다. 최근 접속 기록이 없는 항목은 워크스페이스 생성 시각 내림차순으로 뒤에 배치한다.

### 3. 응답 변환

Controller는 `WorkspaceListItem`을 `workspaceId`, `name`, `memberCount` 필드의 `WorkspaceListResponse`로 변환한다. 조회 결과가 없으면 HTTP `200 OK`의 `data`에 빈 배열을 반환한다.

## 워크스페이스 최근 접속 API 흐름

```text
PUT /api/workspaces/{workspaceId}/recent-access
  → Security Filter
  → AuthUser / Authorities
  → WorkspaceController
  → RecordWorkspaceRecentAccessUseCase
  → WorkspaceRecentAccessService
  → WorkspaceListQueryPort.existsAccessible
  → WorkspaceListQueryAdapter
  → WorkspaceRecentAccessPort.upsert
  → WorkspaceRecentAccessAdapter
  → WorkspaceRecentAccessJpaRepository
```

### 1. 인증 정보와 조회 권한 전달

Controller는 `AuthUser`에서 `academyId`, `userId`를 추출한다. 인증 객체의 Authority에 `WORKSPACE:READ_ALL`이 있으면 `canReadAll=true`, 없으면 `false`로 계산해 UseCase에 전달한다.

### 2. 접근 가능 여부 확인

`WorkspaceRecentAccessService`는 같은 학원의 삭제되지 않은 워크스페이스인지 확인한다.

- `canReadAll=false`: 요청 사용자가 참여한 워크스페이스만 허용
- `canReadAll=true`: 같은 학원의 활성 워크스페이스면 허용

접근할 수 없으면 최근 접속 저장 없이 `COMMON_403_1`로 응답한다.

### 3. 최근 접속 시각 저장과 응답

접근 가능하면 서버의 `Clock` 기준 현재 시각을 저장한다. Repository는 MySQL의 `INSERT ... ON DUPLICATE KEY UPDATE` 단일 쿼리로 `(userId, workspaceId)` 기록을 생성하거나 갱신한다. 동일한 최초 요청이 동시에 들어와도 복합 PK 충돌로 실패하지 않으며, 갱신 시에는 더 최신 접속 시각을 유지한다. 성공 응답은 본문 없는 HTTP `204 No Content`이다.
