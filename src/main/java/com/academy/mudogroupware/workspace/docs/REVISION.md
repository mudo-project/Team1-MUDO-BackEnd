# 🔄 워크스페이스 생성 이름 중복 정책 단순화

## ✅ 2026-08-06 · 업무 자동 지연 스케줄러 최종 리뷰 반영

### 변경 목적

업무 자동 지연 처리 스케줄러(`WorkspaceTaskDelayScheduler`) 구현 완료 후 진행한 전체 브랜치 코드 리뷰에서 나온 지적사항 중, 정확성·성능·테스트 커버리지에 영향을 주는 항목을 반영합니다. Application 계층이 JPA Repository/Entity에 직접 의존하는 계층 위반은 이번 범위에서 제외했습니다 — `Task`는 아직 도메인 모델·Repository 인터페이스가 없어(다른 워크스페이스 하위 Aggregate와 달리 JPA Entity로만 존재), 지금 Port 하나만 임시로 만들면 향후 Task 도메인 모델을 정식으로 만들 때 다시 갈아엎어야 합니다. Task 도메인을 만드는 라운드(예: 업무 CRUD 구현)로 미룹니다.

### 구현 변경

- `TaskJpaRepository.findOverdueRecurringTasks`의 `function('DATE', t.scheduledFor) < :today` 술어를 `t.scheduledFor < :startOfToday`(`LocalDateTime`) 비교로 변경했습니다. 컬럼에 함수를 적용하지 않아 인덱스를 탈 수 있고, MySQL 전용 `DATE()` 함수 의존이 사라져 방언 이식성 문제도 함께 해소됩니다. `DelayOverdueTasksService`는 `today.atStartOfDay()`를 계산해 넘깁니다.
- `findOverdueRegularTasks`/`findOverdueRecurringTasks` 두 쿼리 모두에 `t.workspace.deletedAt is null` 조건을 추가했습니다. 소프트 삭제된 워크스페이스의 업무는 자동 지연 대상에서 제외합니다(정책 확정: 삭제 기간 중 지연 이력이 쌓인 채로 워크스페이스가 복구되는 시나리오를 방지).
- `DelayOverdueTasksService`에 `@Slf4j`를 추가하고, 지연 전환 완료 후 처리 건수(일반/반복)를 INFO 레벨로 한 줄 로그합니다. 이 프로젝트의 첫 `@Scheduled` 배치 잡이라 실패해도 아무도 모르는 상태를 막기 위한 최소 관측성입니다. 청크 처리·분산 락 등 운영 성숙도 항목은 실제 데이터량·인스턴스 수가 확인될 때로 미뤘습니다.
- `DelayOverdueTasksService`에 Task 도메인 모델 부재로 JPA Repository에 직접 의존한다는 사실과, Task 도메인 모델을 만들 때 Port/Adapter로 전환한다는 `TODO` 주석을 남겼습니다.
- `TaskJpaRepositoryOverdueQueryDataJpaTest`에서 DATE() 함수 제거에 따라 불필요해진 `@AutoConfigureTestDatabase(replace = Replace.NONE)`(H2를 MySQL 호환 모드로 유지하던 임시 조치)를 제거했습니다. 기본 `@DataJpaTest` 격리 H2로 돌아가, 다른 `@SpringBootTest`와 이름 있는 인메모리 DB를 공유하던 잠재적 테스트 오염 위험도 함께 해소됩니다.

### 검증

- `TaskJpaRepositoryOverdueQueryDataJpaTest`에 소프트 삭제된 워크스페이스의 업무가 일반/반복 쿼리 각각에서 제외되는지 검증하는 테스트를 추가했습니다.
- `DelayOverdueTasksServiceTest`의 고정 `Clock`을 KST 날짜 경계를 실제로 넘는 시각(UTC 전날 15:00)으로 바꾸고, `LocalDate.now(clock)`이 의도한 KST 날짜로 계산되는지 단언을 추가했습니다. 기존 값은 UTC와 KST 날짜가 우연히 같아 시간대 버그가 있어도 테스트를 통과시켰습니다.
- 같은 테스트에서 `ArgumentCaptor`로 저장된 이력 행을 캡처해 `previousStatus`(전환 전 상태)·`currentStatus`(`DELAYED`)·`changedBy`(`NULL`)를 직접 단언하도록 강화했습니다. 기존에는 `save()` 호출 횟수만 검증해 이력 내용 자체는 검증하지 못했습니다.
- 전체 `./gradlew test`를 통과했습니다(389/389).

> 자동 지연 조건에 워크스페이스 소프트 삭제 제외 규칙을 추가한 내용은 [BUSINESS_RULES.md](BUSINESS_RULES.md)에도 반영했습니다. 📚

## ✅ 2026-08-05 · 워크스페이스 상세 조회 코드 리뷰 반영

### 변경 목적

워크스페이스 상세 조회 API에 대한 코드 리뷰(CodeRabbit) 지적사항 중 재현 가능성·정보 일관성에 영향을 주는 항목을 반영합니다. 성능 최적화 방향은 맞지만 지금 데이터량에서 급하지 않은 항목(중첩 서브쿼리 재작성, projection 도입)과, 이미 의도적으로 분리한 설계(`findActiveWorkspaceName`의 존재 확인·접근 권한 확인 분리)는 이번 범위에서 제외했습니다.

### 구현 변경

- `WorkspaceDetailQueryService.TASK_ORDER`에 업무 번호(`taskId`)를 마지막 tie-break로 추가했습니다. 상태·기한·생성 시각이 모두 같은 업무가 있어도 항상 같은 순서를 반환합니다.
- 참여자·업무 생성자의 표시 이름을 조회하지 못하면 `null` 대신 `"알 수 없음"`으로 대체하도록 `resolveName`을 추가했습니다. 목록에서 제외하지 않으므로 `memberCount`/`taskCount`와 실제 배열 길이가 항상 일치합니다.
- `TaskJpaRepository.findVisibleRegularTasks`/`findVisibleRecurringTasks`의 날짜 비교를 `cast(... as date) = :date`에서 `>= startOfDay and < endOfDay` 범위 비교로 변경했습니다. 컬럼에 함수를 적용하지 않아 인덱스를 탈 수 있습니다. 두 메서드는 `LocalDate` 대신 `LocalDateTime` 시작·끝 두 개를 파라미터로 받도록 시그니처를 바꿨고, `LocalDate` → 시작/끝 시각 변환은 `WorkspaceDetailQueryAdapter`가 담당합니다.
- `WorkspaceJpaRepository.findMemberUserIds`에 `order by member.id.userId asc`를 추가해 참여자 목록 순서를 고정했습니다.

### 검증

- `WorkspaceDetailQueryServiceTest`에 tie-break 정렬, 이름 조회 실패 fallback 테스트를 추가했습니다.
- `TaskJpaRepositoryDataJpaTest`를 새 시그니처(`LocalDateTime` 시작/끝)에 맞게 갱신했고, 기존 표시 규칙 검증(당일 완료 업무만 노출, 재완료 시 최신 이력만 사용, 반복 업무 당일 회차만 노출)이 그대로 통과하는 것으로 회귀가 없음을 확인했습니다.
- `WorkspaceJpaRepositoryDetailQueryDataJpaTest`의 참여자 조회 검증을 `containsExactlyInAnyOrder`에서 `containsExactly`로 강화했습니다.
- 전체 `./gradlew test`를 통과했습니다.

> 외부 API 계약에 영향을 주는 변경(참여자 표시 이름의 `"알 수 없음"` fallback, 업무 카드 정렬의 `taskId` tie-break)은 [WORKSPACE_API.md](WORKSPACE_API.md), [WORKSPACE_API_FLOW.md](WORKSPACE_API_FLOW.md)에도 반영했습니다. 📚

## ✅ 2026-08-05 · 워크스페이스 목록·최근 접속 조회 추가

### 변경 목적

워크스페이스 페이지 진입 시 사용자가 참여 중인 공간을 바로 확인하고, 공용 공간에서도 각 사용자가 최근에 확인한 워크스페이스를 빠르게 다시 열 수 있도록 목록 조회와 사용자별 최근 접속 기록을 추가했습니다.

### 구현 변경

- `GET /api/workspaces?scope=MINE|ALL` 목록 조회 API를 추가했습니다.
- 기본 조회 범위는 요청 사용자가 참여한 같은 학원의 활성 워크스페이스(`MINE`)입니다.
- `WORKSPACE:READ_ALL` 권한이 있는 사용자는 같은 학원의 전체 활성 워크스페이스(`ALL`)를 조회할 수 있습니다.
- 목록 응답은 `workspaceId`, `name`, `memberCount`만 제공하며, 요청 사용자 기준 최근 접속 시각 내림차순으로 정렬합니다. 미접속 워크스페이스는 생성 시각 내림차순으로 뒤에 배치합니다.
- `PUT /api/workspaces/{workspaceId}/recent-access` 최근 접속 기록 API를 추가했습니다. 상세 화면을 정상적으로 연 뒤 호출하며, 요청 본문 없이 `204 No Content`를 반환합니다.
- `workspace_recent_access`는 `(user_id, workspace_id)` 복합 PK를 사용해 사용자별·워크스페이스별 한 행만 유지하고, 재접속 시 `last_accessed_at`을 갱신합니다.
- 최근 접속 기록은 MySQL 단일 upsert로 처리해 같은 최초 접속 요청이 동시에 들어와도 중복 키 오류 없이 생성 또는 갱신합니다.

### 검증

- 목록 조회의 `MINE`·`ALL` 범위, 같은 학원 제한, `WORKSPACE:READ_ALL` 권한 경계를 검증했습니다.
- 최근 접속 기록의 생성·갱신, 사용자별 정렬, 미접속 항목의 후순위 정렬을 검증했습니다.
- Testcontainers MySQL에서 동일한 `(user_id, workspace_id)`에 대한 동시 최초 upsert 요청이 한 행으로 저장되고 모두 성공하는지 검증했습니다.
- 더 최신 접속 시각을 저장한 뒤 더 과거의 요청이 도착해도 최신 시각이 유지되는지 검증했습니다.
- 전체 Gradle 테스트와 `git diff --check`를 통과했습니다.

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
