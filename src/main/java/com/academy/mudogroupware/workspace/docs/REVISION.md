# 🔄 워크스페이스 생성 이름 중복 정책 단순화

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
