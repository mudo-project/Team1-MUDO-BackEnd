# 🔄 워크스페이스 생성 이름 중복 정책 단순화

## ✅ 2026-08-05 · 도메인 생성·복원 경로 분리

### 변경 목적

워크스페이스 신규 생성 규칙과 DB 복원 규칙을 같은 Builder 경로로 처리하면, DB 복원 시에도 생성자 자동 참여 규칙이 적용될 수 있습니다. 신규 생성과 복원의 책임을 Domain Model에 명확히 분리합니다.

### 구현 변경

- `Workspace.create(...)`를 추가해 신규 워크스페이스 생성만 생성자 자동 참여 규칙을 적용하도록 했습니다.
- `Workspace.restore(...)`를 추가해 DB에 저장된 ID·생성자·참여자 상태를 변경 없이 복원하도록 했습니다.
- `Workspace`의 Builder를 제거해 외부 생성 경로를 `create`와 `restore`로 제한했습니다.
- `WorkspaceService`는 Builder 대신 `Workspace.create(...)`를 호출하고, 기존 참여자 검증과 이름 중복 확인 흐름을 유지합니다.
- `WorkspacePersistenceMapper.toDomain(...)`은 default 메서드에서 `Workspace.restore(...)`를 호출하도록 변경했습니다.
- MapStruct 생성 구현체는 `toEntity(...)`만 생성하며, `toDomain(...)`은 Mapper의 default 복원 로직을 사용합니다.

### 규칙

- `create(...)`는 전달받은 추가 참여자를 방어적으로 복사하고 생성자를 추가합니다.
- 생성자가 추가 참여자 목록에 포함되어도 Set으로 한 번만 보관합니다.
- `restore(...)`는 생성자를 참여자 목록에 다시 추가하지 않습니다.
- 별도 Policy를 만들지 않고 `WorkspaceService`가 `WorkspaceMemberDirectoryPort`를 직접 호출합니다.

### 검증

- `Workspace.create()`의 생성자 자동 참여, 생성자 중복 제거, 방어적 복사를 검증했습니다.
- `Workspace.restore()`가 저장된 참여자 목록을 그대로 복원하는지 검증했습니다.
- `WorkspacePersistenceMapper.toDomain()`이 복원 과정에서 생성자를 추가하지 않는지 검증했습니다.
- `clean compileJava` 후 생성된 `WorkspacePersistenceMapperImpl`이 `toEntity()`만 구현하는 것을 확인했습니다.
- `WorkspaceTest`, `WorkspaceServiceTest`, `WorkspacePersistenceMapperTest`, `WorkspacePersistenceAdapterTest`, `WorkspacePersistenceAdapterDataJpaTest`, `CreateWorkspaceRequestTest`를 통과했습니다.

> 외부 API와 사용자 정책은 변경되지 않아 CHANGELOG에는 별도 항목을 추가하지 않았습니다.

## ✅ 2026-08-05 · 생성 시 자동 접미사 제거

### 변경 목적

초기 워크스페이스 생성에서 중복 이름을 자동으로 변경하면 사용자가 의도하지 않은 이름으로 생성될 수 있습니다. 생성과 이름 수정의 중복 정책을 동일하게 맞추고, 이름 결정·재시도 로직을 단순화합니다.

### 정책 변경

- 같은 학원의 활성 워크스페이스 이름은 고유합니다.
- 생성 또는 이름 수정 시 동일한 활성 이름이 존재하면 자동으로 `(1)`을 붙이지 않고 `409 Conflict`를 반환합니다.
- 이름 비교 전 앞뒤 공백을 제거합니다.
- 자동 번호 부여는 추후 워크스페이스 복사 기능에만 적용합니다.
- 삭제된 워크스페이스 복구 시의 자동 번호 부여 정책은 유지합니다.

### 구현 변경

- `WorkspaceCreationTransaction`을 제거했습니다.
- `WorkspaceService`가 단일 트랜잭션에서 참여자 검증, 활성 이름 확인, 워크스페이스 저장을 처리합니다.
- 활성 이름이 존재하면 저장 전에 `WorkspaceNameConflictException`을 발생시킵니다.
- 동시 생성으로 사전 확인 이후 DB unique 제약이 충돌해도 `WorkspacePersistenceAdapter`가 `WorkspaceNameConflictException`으로 변환합니다.
- `WORKSPACE_409_1`은 사전 중복 확인과 DB unique 제약 충돌에 동일하게 사용합니다.

### 영향 범위

| 구분 | 변경 내용 |
| --- | --- |
| Application | 자동 접미사 탐색 및 재시도 제거, 생성 트랜잭션을 `WorkspaceService`로 통합 |
| Domain Exception | 기존 `WorkspaceNameConflictException`을 생성 중복 응답으로 사용 |
| Persistence | `(academy_id, active_name)` unique 제약 충돌의 예외 변환 유지 |
| Presentation | Swagger의 `409` 설명을 동일 활성 이름 충돌로 갱신 |
| Documentation | 비즈니스 정책, API 명세, 처리 흐름을 새 정책과 동기화 |

### 검증

- 중복된 활성 이름으로 생성 요청하면 `WorkspaceNameConflictException`이 발생하고 저장하지 않는 서비스 테스트를 추가했습니다.
- `WorkspaceServiceTest`, `WorkspacePersistenceAdapterTest`, `WorkspacePersistenceAdapterDataJpaTest`를 통과했습니다.
- `compileJava`와 `git diff --check`를 통과했습니다.

> 사용자 관점의 변경 이력은 [CHANGELOG.md](CHANGELOG.md)를 참고해주세요. 📚
