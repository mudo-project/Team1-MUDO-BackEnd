# 🆕 워크스페이스 생성 API 흐름

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

`Security Filter`가 Access Token을 검증하고 `AuthUser`를 만든다. `WorkspaceController`는 `AuthUser`에서 `academyId`와 `userId`를 받는다. `academyId`는 워크스페이스 자체에는 더 이상 저장되지 않고(2026-08-10, `academy_id` 컬럼 삭제), 참여자가 같은 학원의 활성 사용자인지 확인하는 `users` 도메인 조회에만 쓰인다. `userId`는 요청 본문에는 없는 생성자 정보를 결정한다.

## 2. 요청 검증과 Command 변환

`CreateWorkspaceRequest`는 이름의 필수 여부·최대 100자와 참여자 번호의 양수를 Bean Validation으로 검증한다. 검증을 통과하면 `CreateWorkspaceCommand`로 변환한다.

## 3. 참여자 확정과 검증

`WorkspaceService`는 추가 참여자 목록을 `LinkedHashSet`으로 정리하고 생성자를 추가한다. 빈 추가 참여자 목록도 허용한다.

`WorkspaceMemberDirectoryPort`는 workspace가 소유한 외부 조회 계약이다. 구현체인 `WorkspaceMemberDirectoryAdapter`는 users의 공개 `UserDirectoryUseCase`를 호출한다. users는 같은 학원이며 `ACTIVE` 상태인 사용자 ID만 반환한다.

요청한 ID 전체가 반환되지 않으면 `InvalidWorkspaceMemberException`을 발생시키고 `WORKSPACE_400_1`로 응답한다.

## 4. 이름 확인과 저장

`WorkspaceService`는 하나의 트랜잭션에서 활성 이름 사용 여부를 조회한다.

- 이미 존재하는 활성 이름이면 `WorkspaceNameConflictException`을 발생시켜 `WORKSPACE_409_1`을 반환한다.
- 사용 가능한 이름이면 앞뒤 공백을 제거한 이름과 참여자 ID로 `Workspace` Domain Model을 만들고 `WorkspaceRepository`에 전달한다.

## 5. 영속화

`WorkspacePersistenceAdapter`는 MapStruct로 Domain Model을 `WorkspaceJpaEntity`로 변환하고, 각 참여자 ID를 `workspace_member` 엔티티로 추가한다. `saveAndFlush` 후 생성된 워크스페이스 ID를 포함한 Domain Model을 반환한다.

`workspace` 테이블의 `active_name` unique 제약이 충돌하면 Adapter가 `WorkspaceNameConflictException`으로 변환한다.

## 6. 응답

성공하면 Controller가 `GlobalApiResponse.created`로 HTTP `201 Created`와 `workspaceId`를 반환한다.

## 📋 워크스페이스 목록 조회 API 흐름

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

`WorkspaceController`는 `AuthUser`의 `userId`를 사용한다(`academyId` 검증은 하지 않는다 — 2026-08-10 정정). `scope`를 생략하면 `MINE`으로 바인딩된다.

Spring Method Security의 `@PreAuthorize`가 `scope=ALL` 요청 시 `WORKSPACE:READ_ALL` Authority를 확인한다(연동 완료, 2026-08-10) — 없으면 UseCase 호출 전에 `403`으로 차단된다.

### 2. 조회 범위 선택

`WorkspaceQueryService`는 `scope`에 따라 조회 Port를 선택한다.

- `MINE`: 활성 워크스페이스 중 요청 사용자가 참여한 목록 조회
- `ALL`: 전체 활성 워크스페이스 목록 조회

두 조회 모두 요청 사용자의 최근 접속 시각 내림차순으로 정렬한다. 최근 접속 기록이 없는 항목은 워크스페이스 생성 시각 내림차순으로 뒤에 배치한다.

### 3. 응답 변환

Controller는 `WorkspaceListItem`을 `workspaceId`, `name`, `memberCount` 필드의 `WorkspaceListResponse`로 변환한다. 조회 결과가 없으면 HTTP `200 OK`의 `data`에 빈 배열을 반환한다.

## 🕒 워크스페이스 최근 접속 API 흐름

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

Controller는 `AuthUser`에서 `userId`를 추출한다. 인증 객체의 Authority에 `WORKSPACE:READ_ALL`이 있으면 `canReadAll=true`, 없으면 `false`로 계산해 UseCase에 전달한다.

### 2. 접근 가능 여부 확인

`WorkspaceRecentAccessService`는 삭제되지 않은 워크스페이스인지 확인한다. `academyId` 검증은 하지 않는다(2026-08-10 정정 — 실제 배포는 학원마다 별도 EC2·DB 스키마를 쓰는 구조라 앱 레벨의 academyId 검증이 불필요하다).

- `canReadAll=false`: 요청 사용자가 참여한 워크스페이스만 허용
- `canReadAll=true`: 활성 워크스페이스면 허용

접근할 수 없으면 최근 접속 저장 없이 `COMMON_403_1`로 응답한다.

### 3. 최근 접속 시각 저장과 응답

접근 가능하면 서버의 `Clock` 기준 현재 시각을 저장한다. Repository는 MySQL의 `INSERT ... ON DUPLICATE KEY UPDATE` 단일 쿼리로 `(userId, workspaceId)` 기록을 생성하거나 갱신한다. 동일한 최초 요청이 동시에 들어와도 복합 PK 충돌로 실패하지 않으며, 갱신 시에는 더 최신 접속 시각을 유지한다. 성공 응답은 본문 없는 HTTP `204 No Content`이다.

## 🔍 워크스페이스 상세 조회 API 흐름

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

Controller는 `AuthUser`에서 `userId`를 추출하고, 인증 객체의 Authority에 `WORKSPACE:READ_ALL`이 있으면 `canReadAll=true`로 계산한다. `date` 쿼리 파라미터가 없으면 서버의 `Clock` 기준 오늘 날짜를 기준일로 사용한다.

### 2. 존재 확인과 접근 제어

`WorkspaceDetailQueryService`는 `findActiveWorkspaceName`으로 삭제되지 않은 워크스페이스인지 먼저 확인한다. 없으면 `WorkspaceNotFoundException`을 발생시켜 `WORKSPACE_404_1`로 응답한다.

존재하면 `WorkspaceListQueryPort.existsAccessible`로 접근 가능 여부를 확인한다. `canReadAll=false`면 요청 사용자가 참여한 워크스페이스만, `true`면 활성 워크스페이스면 허용한다(`academyId` 검증은 하지 않는다 — 2026-08-10 정정). 접근할 수 없으면 `WorkspaceAccessDeniedException`을 발생시켜 `WORKSPACE_403_1`로 응답한다.

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

## ✏️ 워크스페이스 이름 수정 API 흐름

```text
PATCH /api/workspaces/{workspaceId}
  → Security Filter
  → AuthUser
  → WorkspaceController
  → RenameWorkspaceRequest
  → RenameWorkspaceCommand
  → RenameWorkspaceUseCase
  → RenameWorkspaceService
  → WorkspaceRepository.findByIdForUpdate
  → WorkspacePersistenceAdapter
  → WorkspaceJpaRepository
  → Workspace.rename
  → WorkspaceRepository.rename
  → WorkspacePersistenceAdapter
  → WorkspaceRenameResponse
  → GlobalApiResponse
```

### 1. 요청 검증과 trim

`RenameWorkspaceRequest`의 compact 생성자가 Bean Validation이 실행되기 전에 이름을 먼저 `trim()`한다. `@NotBlank`/`@Size(max = 100)`은 trim된 값을 검증하므로, 앞뒤 공백을 포함해 100자를 넘는 이름도 trim 후 100자 이하면 통과한다.

### 2. 잠금 조회와 참여자 검증

`RenameWorkspaceService`는 `WorkspaceRepository.findByIdForUpdate`(비관적 락)로 활성 워크스페이스를 조회한다. 없으면 `WorkspaceNotFoundException`을 발생시켜 `WORKSPACE_404_1`로 응답한다. 요청자가 현재 참여자가 아니면 `WorkspaceAccessDeniedException`을 발생시켜 `WORKSPACE_403_1`로 응답한다.

`WORKSPACE:CREATE` 권한은 아직 시드되지 않아, 이번 라운드는 참여자 여부만 검증하고 `@PreAuthorize` 자리에 TODO 주석만 남긴다.

### 3. 이름 반영과 저장

`Workspace.rename(newName)` 도메인 메서드가 새 이름을 반영한 새 인스턴스를 반환한다. `WorkspaceRepository.rename`이 실제 저장을 담당하며, DB의 `active_name` unique 제약이 충돌하면 Adapter가 `WorkspaceNameConflictException`으로 변환해 `WORKSPACE_409_1`로 응답한다. 사전 중복 조회 없이 이 예외 변환에만 의존하므로, 동시에 같은 이름으로 변경을 시도해도 정확히 하나만 성공한다.

### 4. 응답

Controller가 `WorkspaceRenameResponse.from(workspaceId, name)`을 `GlobalApiResponse.ok`로 감싸 HTTP `200 OK`를 반환한다.

## 🗑️ 워크스페이스 삭제 API 흐름

```text
DELETE /api/workspaces/{workspaceId}
  → Security Filter
  → AuthUser
  → WorkspaceController
  → DeleteWorkspaceCommand
  → DeleteWorkspaceUseCase
  → DeleteWorkspaceService
  → WorkspaceRepository.findByIdForUpdate
  → WorkspacePersistenceAdapter
  → WorkspaceJpaRepository
  → WorkspaceRepository.delete
  → WorkspacePersistenceAdapter
```

### 1. 잠금 조회와 참여자 검증

`DeleteWorkspaceService`는 `findByIdForUpdate`로 활성 워크스페이스를 조회한다. 없으면 `WorkspaceNotFoundException`(`WORKSPACE_404_1`), 요청자가 참여자가 아니면 `WorkspaceAccessDeniedException`(`WORKSPACE_403_1`)으로 응답한다.

`WORKSPACE:DELETE` 권한 시드 전까지는 참여자 수와 무관하게 참여자 여부만 검증한다. 본인이 유일한 참여자인 상태의 삭제는 자진 탈퇴의 대체 행위이므로, 권한 연동 이후에도 계속 권한 없이 허용할 예정이다.

### 2. 소프트 삭제

서버에 주입된 `Clock` 기준 현재 시각으로 `WorkspaceRepository.delete(workspaceId, deletedAt)`을 호출한다. Adapter는 `SoftDeleteTimeEntity.markDeleted(deletedAt)`으로 `deleted_at`을 세팅하고 `saveAndFlush`한다. 물리 삭제는 하지 않는다.

### 3. 응답

성공하면 Controller가 `GlobalApiResponse.ok(WorkspaceResponseCode.WORKSPACE_DELETED)`로 `200 OK`를 반환한다.

## ➕ 워크스페이스 참여자 추가 API 흐름

```text
POST /api/workspaces/{workspaceId}/members
  → Security Filter
  → AuthUser
  → WorkspaceController
  → AddWorkspaceMembersRequest
  → AddWorkspaceMembersCommand
  → AddWorkspaceMembersUseCase
  → AddWorkspaceMembersService
  → WorkspaceRepository.findByIdForUpdate
  → WorkspacePersistenceAdapter
  → Workspace.newlyAddedMemberIds / addMembers
  → WorkspaceMemberDirectoryPort
  → WorkspaceMemberDirectoryAdapter
  → UserDirectoryUseCase
  → WorkspaceRepository.updateMembers
  → WorkspacePersistenceAdapter
  → WorkspaceMemberAddResponse
  → GlobalApiResponse
```

### 1. 요청 검증

`AddWorkspaceMembersRequest.memberIds`는 `@NotEmpty`(목록 자체)와 각 원소의 `@NotNull`/`@Positive`로 검증한다. 빈 목록·null·양수가 아닌 값은 `COMMON_400_1`로 거부된다.

### 2. 잠금 조회와 참여자 검증

`findByIdForUpdate`로 활성 워크스페이스를 조회하고, 요청자가 현재 참여자인지 확인한다(아니면 `WORKSPACE_403_1`).

### 3. 신규 후보만 골라 학원 소속 검증

`Workspace.newlyAddedMemberIds`로 이미 참여 중인 사용자를 제외한 신규 후보만 추린다. 신규 후보가 없으면(전원 이미 참여 중) `WorkspaceMemberDirectoryPort` 호출과 저장소 갱신을 모두 건너뛰고 빈 목록을 반환한다 — 멱등 처리.

신규 후보가 있으면 `WorkspaceMemberDirectoryPort.findActiveUserIds(academyId, candidateIds)`로 같은 학원의 `ACTIVE` 사용자인지 확인한다(내부적으로 users의 `UserDirectoryUseCase` 호출). 하나라도 아니면 `InvalidWorkspaceMemberException`을 발생시켜 `WORKSPACE_400_1`로 응답하고, 저장소는 갱신하지 않는다.

### 4. 참여자 갱신과 응답

`Workspace.addMembers(newIds)`로 참여자 집합을 갱신한 뒤 `WorkspaceRepository.updateMembers`로 반영한다. Controller는 신규로 추가된 사용자 ID만(이미 참여 중이던 사용자는 제외) `WorkspaceMemberAddResponse`에 담아 HTTP `200 OK`로 반환한다.

## ➖ 워크스페이스 참여자 제거(자진 탈퇴 겸용) API 흐름

```text
DELETE /api/workspaces/{workspaceId}/members/{userId}
  → Security Filter
  → AuthUser
  → WorkspaceController
  → RemoveWorkspaceMemberCommand
  → RemoveWorkspaceMemberUseCase
  → RemoveWorkspaceMemberService
  → WorkspaceRepository.findByIdForUpdate
  → WorkspacePersistenceAdapter
  → Workspace.removeMember
  → WorkspaceRepository.updateMembers
  → WorkspacePersistenceAdapter
```

### 1. 잠금 조회와 참여자 검증

`findByIdForUpdate`로 활성 워크스페이스를 조회한다. 요청자(`authUser.userId()`)가 현재 참여자가 아니면 `WorkspaceAccessDeniedException`(`WORKSPACE_403_1`)으로 응답한다 — 대상 `userId`가 요청자 자신이면 자진 탈퇴이므로 이 체크만 통과하면 항상 허용된다. 타인 제거는 `WORKSPACE:CREATE` 권한 시드 후 이 체크에 조건이 추가될 예정이다.

### 2. 도메인 모델의 불변식 판정

실제 제거 판단은 `RemoveWorkspaceMemberService`가 아니라 `Workspace.removeMember(targetUserId)` 도메인 메서드가 한다:

1. 대상이 참여자가 아니면 `WorkspaceMemberNotFoundException`(`WORKSPACE_404_2`) — 먼저 판정.
2. 제거 후 참여자가 0명이 되면(마지막 1인) `WorkspaceLastMemberException`(`WORKSPACE_400_2`) — 그다음 판정.

순서가 고정되어 있어, 참여자가 1명뿐인 워크스페이스에서 참여자가 아닌 대상을 지정하면 400이 아니라 404가 반환된다.

### 3. 참여자 갱신과 동시성

통과하면 `WorkspaceRepository.updateMembers`로 참여자 집합을 갱신한다. `findByIdForUpdate`의 비관적 락 덕분에, 마지막 두 참여자가 동시에 자진 탈퇴를 시도해도 정확히 한 명만 성공한다 — Testcontainers MySQL 기반 동시성 테스트로 실제 검증됨(락 제거 시 재현되는 실패, 복원 시 정상 동작 확인).

### 4. 응답

성공하면 Controller가 `GlobalApiResponse.ok(WorkspaceResponseCode.WORKSPACE_MEMBER_REMOVED)`로 `200 OK`를 반환한다.

## ♻️ 워크스페이스 복구 API 흐름

```text
POST /api/workspaces/{workspaceId}/recover
  → Security Filter
  → AuthUser
  → WorkspaceController
  → RecoverWorkspaceCommand
  → RecoverWorkspaceUseCase
  → RecoverWorkspaceService
  → WorkspaceRepository.findDeletedByIdForUpdate
  → WorkspacePersistenceAdapter
  → WorkspaceJpaRepository
  → WorkspaceRepository.existsByAcademyIdAndName
  → Workspace.recover
  → WorkspaceRepository.recover
  → WorkspacePersistenceAdapter
  → WorkspaceRecoverResponse
  → GlobalApiResponse
```

### 1. 삭제된 워크스페이스 잠금 조회 (세 갈래 분기)

`WorkspaceRepository.findDeletedByIdForUpdate`는 다른 워크스페이스 API들의 `findByIdForUpdate`(활성 워크스페이스만 대상)와 반대로 **삭제된** 워크스페이스만 비관적 락으로 조회한다.

- 워크스페이스가 아예 없으면 `Optional.empty()` → `WorkspaceNotFoundException`(`WORKSPACE_404_1`)
- 워크스페이스는 있는데 삭제된 적 없이 활성 상태면 Adapter가 `existsById`로 이를 감지해 `WorkspaceAlreadyActiveException`(`WORKSPACE_409_2`)을 즉시 던진다 — **이 판정은 참여자 여부 확인보다 먼저 일어난다.**
- 삭제된 상태면 도메인 객체를 반환한다.

### 2. 삭제 당시 참여자 검증

소프트 삭제는 `deleted_at`만 세팅하고 `workspace_member` 테이블은 건드리지 않으므로, 삭제된 워크스페이스에 대해서도 "삭제 당시 참여자였는가"를 그대로 조회할 수 있다. `RecoverWorkspaceService`는 요청자가 이 멤버 목록에 없으면 `WorkspaceAccessDeniedException`(`WORKSPACE_403_1`)으로 응답한다.

### 3. 이름 충돌 확인과 타임스탬프 접미사

`WorkspaceRepository.existsByAcademyIdAndName`(기존 생성 API가 쓰는 것과 동일한 포트)으로 원래 이름이 이미 다른 활성 워크스페이스와 충돌하는지 **한 번만** 확인한다. 충돌하면 서버 `Clock` 기준 초 단위 시각으로 `"이름(yyyyMMddHHmmss)"` 접미사를 붙인다. 접미사는 항상 16자로 고정이라 원본 이름을 84자로 잘라 100자 제한을 항상 보장한다. 순번(`(1)`, `(2)`...) 방식은 검토했으나, 몇 번까지 재시도할지 몰라 같은 트랜잭션 안에서 쓰기를 반복해야 하는 위험 때문에 채택하지 않았다 — 타임스탬프는 재시도 없이 존재 확인 1회 + 쓰기 1회로 끝난다.

### 4. 복구와 저장

`Workspace.recover(finalName)` 도메인 메서드로 새 인스턴스를 만든 뒤 `WorkspaceRepository.recover(workspaceId, finalName)`을 호출한다. Adapter는 `SoftDeleteTimeEntity.clearDeletedAt()`(`markDeleted()`의 반대 동작)과 `rename()`을 함께 반영해 저장한다.

`findDeletedByIdForUpdate`와 `recover` 호출은 **하나의 `@Transactional` 메서드 안에서** 실행되어야 한다 — `recover()`의 내부 구현이 잠금 없는 `findById` 재조회에 의존하는데, 같은 트랜잭션이면 Hibernate 1차 캐시가 이미 잠긴 엔티티 인스턴스를 그대로 돌려주므로 안전하다. 극히 드물게 존재 확인과 실제 쓰기 사이에 다른 요청이 같은 이름을 선점하면 DB unique 제약 위반이 `WorkspaceNameConflictException`(`WORKSPACE_409_1`)으로 변환되어 재시도 없이 그대로 반환된다.

### 5. 응답

Controller가 `WorkspaceRecoverResponse.from(workspaceId, finalName)`을 `GlobalApiResponse.ok`로 감싸 HTTP `200 OK`를 반환한다.
