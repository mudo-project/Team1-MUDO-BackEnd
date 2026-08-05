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

## 워크스페이스 상세 조회 API 흐름

```text
GET /api/workspaces/{workspaceId}?date=yyyy-MM-dd
  → Security Filter
  → AuthUser / Authorities
  → WorkspaceController
  → WorkspaceDetailQueryUseCase
  → WorkspaceDetailQueryService
  → WorkspaceDetailQueryPort (이름 · 참여자 · 업무 후보 · 코멘트 요약)
  → WorkspaceDetailQueryAdapter
  → WorkspaceListQueryPort.existsAccessible
  → WorkspaceListQueryAdapter
  → WorkspaceUserInfoPort
  → WorkspaceUserInfoAdapter
  → WorkspaceDetailResponse
  → GlobalApiResponse
```

### 1. 인증 정보와 기준일 결정

Controller는 `AuthUser`에서 `academyId`, `userId`를 추출하고, 인증 객체의 Authority에 `WORKSPACE:READ_ALL`이 있으면 `canReadAll=true`로 계산한다. `date` 쿼리 파라미터가 없으면 서버의 `Clock` 기준 오늘 날짜를 기준일로 사용한다.

### 2. 존재 확인과 접근 제어

`WorkspaceDetailQueryService`는 `findActiveWorkspaceName`으로 삭제되지 않은 워크스페이스인지 먼저 확인한다. 없으면 `WorkspaceNotFoundException`을 발생시켜 `WORKSPACE_404_1`로 응답한다.

존재하면 `WorkspaceListQueryPort.existsAccessible`로 접근 가능 여부를 확인한다. `canReadAll=false`면 요청 사용자가 참여한 워크스페이스만, `true`면 같은 학원의 활성 워크스페이스면 허용한다. 접근할 수 없으면 `WorkspaceAccessDeniedException`을 발생시켜 `WORKSPACE_403_1`로 응답한다.

### 3. 참여자·업무 후보 조회

`findMemberIds`로 참여자 ID 목록을(사용자 번호 오름차순), `findVisibleTasks(workspaceId, date)`로 표시 대상 업무 후보를 조회한다.

- 일반 업무: 상태가 `COMPLETED`가 아니면 항상 포함한다. `COMPLETED`면 업무 상태 이력 중 가장 최근에 `COMPLETED`로 바뀐 날짜가 기준일과 같은 경우에만 포함한다. 이 날짜 비교는 `createdAt`에 `cast(... as date)`를 적용하지 않고, 기준일의 00:00~다음 날 00:00 범위로 비교해 인덱스를 탈 수 있게 한다.
- 반복 업무: 생성 일시(`scheduledFor`)가 기준일의 00:00~다음 날 00:00 범위에 있는 경우에만 포함한다. 날짜 범위 비교 방식은 일반 업무와 동일하다.

### 4. 표시 이름 일괄 조회

참여자 ID와 업무 후보들의 생성자 ID를 하나의 집합으로 합쳐 `WorkspaceUserInfoPort.findUserInfo`를 한 번만 호출한다. `WorkspaceUserInfoAdapter`는 users의 공개 조회 계약을 통해 사용자별 표시 이름을 가져온다. 참여자 목록과 업무 카드의 생성자 정보를 조회할 때마다 users를 호출하지 않고, 이 맵을 재사용한다.

대상 사용자 ID가 조회 결과에 없으면(데이터 정합성이 깨진 예외적 상황) 표시 이름을 `"알 수 없음"`으로 대체한다. 참여자·생성자를 목록에서 제외하지 않으므로, `memberCount`/`taskCount`와 실제 배열 길이는 항상 일치한다.

### 5. 코멘트 요약과 정렬

`findCommentSummaries`로 후보 업무들의 완료/전체 코멘트 수를 일괄 조회한다. 후보들을 상태 → 기한(`null`은 마지막) → 생성 시각 → 업무 번호 순으로 정렬한 뒤 `WorkspaceTaskItem`으로 변환한다. 업무 번호는 앞선 세 기준이 모두 같을 때의 동점 처리(tie-break)이며, 이 기준이 없으면 같은 요청이 매번 다른 순서를 반환할 수 있다. 코멘트 요약이 없는 업무는 완료/전체 코멘트 수를 `null`로 둔다.

### 6. 응답 변환

Controller는 `WorkspaceDetail`을 `WorkspaceDetailResponse`로 변환해 `GlobalApiResponse.ok`로 HTTP `200 OK`를 반환한다. `completedCommentCount`, `commentCount`가 `null`인 업무는 `@JsonInclude(NON_NULL)`에 의해 응답 JSON에서 해당 필드 자체가 생략된다.
