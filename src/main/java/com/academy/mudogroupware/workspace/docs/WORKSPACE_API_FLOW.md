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
