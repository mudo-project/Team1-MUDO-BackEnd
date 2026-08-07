# ⏰ 업무 자동 지연 처리 스케줄러

> 업데이트: 2026-08-06 · 기한이 지난 미완료 업무를 매일 KST 00:00에 자동으로 `DELAYED`로 전환하는 배치 스케줄러입니다. REST API가 아니며, 서버가 자체적으로 매일 실행합니다.

## 🚀 호출 흐름

```text
KST 00:00 매일 실행
  → SchedulingConfig @EnableScheduling (global/infrastructure/config)
  → WorkspaceTaskDelayScheduler @Scheduled(cron="0 0 0 * * *", zone="Asia/Seoul")
  → DelayOverdueTasksUseCase (interface)
  → DelayOverdueTasksService @Service @Transactional
  → TaskJpaRepository.findOverdueRegularTasks
  → TaskJpaRepository.findOverdueRecurringTasks
  → TaskJpaEntity.markDelayed()
  → TaskStatusHistoryJpaRepository.save()
  → TaskStatusHistoryJpaEntity.systemChanged()
  → Logger.info(...) 처리 건수 기록
```

## 1. 스케줄 트리거

`SchedulingConfig` 클래스는 프로젝트 전체에 **한 번만** 선언된 `@EnableScheduling`을 담당합니다. 이는 Spring이 `@Scheduled` 어노테이션을 감지하고 처리하도록 활성화합니다.

`WorkspaceTaskDelayScheduler`는 매일 KST 00:00에 정확히 실행되는 `@Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")` 메서드를 선언합니다. 이 시점에 `DelayOverdueTasksUseCase` 인터페이스의 `delayOverdueTasks()` 메서드를 호출합니다.

## 2. 일반 업무(Regular Task) 지연 조회

`TaskJpaRepository.findOverdueRegularTasks(LocalDate today, TaskStatus completed, TaskStatus delayed)`는 다음 조건을 만족하는 모든 업무를 조회합니다.

- `recurringTemplate IS NULL` — 반복 템플릿이 없는 일반 업무
- `due_at < :today` — 기한이 오늘보다 이전 (즉, 지난 날짜)
- `status NOT IN (COMPLETED, DELAYED)` — 이미 완료되거나 지연 처리된 업무는 제외 (멱등성)
- `workspace.deleted_at IS NULL` — 소프트 삭제된 워크스페이스의 업무는 제외

## 3. 반복 업무(Recurring Task) 지연 조회

`TaskJpaRepository.findOverdueRecurringTasks(LocalDateTime startOfToday, TaskStatus completed, TaskStatus delayed)`는 다음 조건을 만족하는 모든 업무 발생(occurrence)을 조회합니다.

- `recurringTemplate IS NOT NULL` — 반복 템플릿을 가진 반복 업무
- `scheduledFor < :startOfToday` — 예정일이 오늘(KST) 00:00 이전
  - 예: 2026-08-05 09:00 또는 2026-08-04 23:59는 포함, 2026-08-05 00:00 정각이나 이후는 제외
- `status NOT IN (COMPLETED, DELAYED)` — 이미 완료되거나 지연 처리된 업무는 제외
- `workspace.deleted_at IS NULL` — 소프트 삭제된 워크스페이스의 업무는 제외

## 4. 상태 전환

조회된 각 업무에 대해 `DelayOverdueTasksService`는:

1. 현재 상태를 `previousStatus` 변수에 기록 (전환 전에 **반드시** 수행)
2. `TaskJpaEntity.markDelayed()` 도메인 메서드를 호출해 상태를 `DELAYED`로 변경
3. `TaskStatusHistoryJpaEntity.systemChanged()` 팩토리 메서드로 이력 엔티티를 생성
   - `previousStatus`: 전환 전 상태 (예: `IN_PROGRESS`, `WAITING`)
   - `currentStatus`: `DELAYED`
   - `changedBy`: `NULL` (시스템이 자동으로 생성했음을 의미)
4. `TaskStatusHistoryJpaRepository.save()`로 이력을 저장

**핵심**: `previousStatus`를 `task.getStatus()` 호출로 미리 캡처해야만, `markDelayed()` 후에도 올바른 이전 상태를 기록할 수 있습니다. 이 순서가 뒤바뀌면 이력이 잘못 기록됩니다.

## 5. 멱등성 보장

두 가지 메커니즘이 함께 작동해 재실행 시에도 이력이 중복되지 않도록 합니다.

### 쿼리 기반 멱등성
`findOverdueRegularTasks`와 `findOverdueRecurringTasks` 모두 조회 조건에 `status NOT IN (COMPLETED, DELAYED)`를 포함합니다. 따라서 한 번 `DELAYED`로 전환된 업무는 다음 실행에서 조회 대상이 아니게 됩니다.

### 순차 실행 멱등성
같은 날 여러 번 스케줄러를 수동으로 트리거하거나, 순차적인 배치를 재실행해도 각각 안전하게 처리됩니다.

**주의**: 다중 인스턴스 동시 실행 시(예: ECS 오토스케일링) 쿼리 기반 멱등성만으로는 동시 조회 후 중복 전환을 방지할 수 없습니다. 이 경우 ShedLock 같은 분산 락 도입이 필요하며, [GitHub issue #138](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/138)에서 추적됩니다.

## 6. 로깅

`DelayOverdueTasksService`는 모든 업무 처리를 마친 후 한 줄의 INFO 로그를 남깁니다.

```
업무 자동 지연 처리 완료: 일반 {count1}건, 반복 {count2}건
```

이 로그는 해당 실행일의 처리 결과를 빠르게 추적할 수 있게 하며, 모니터링 시스템(CloudWatch, Prometheus, Loki 등)에서 수집 가능합니다.

## 📋 데이터 흐름 요약

| 단계 | 책임 계층 | 역할 |
|---|---|---|
| 1. 스케줄 트리거 | Infrastructure (`SchedulingConfig`, `WorkspaceTaskDelayScheduler`) | 매일 KST 00:00에 작업 시작 신호 |
| 2. 조회 후보 발굴 | Application (`DelayOverdueTasksService`) | 일반·반복 업무별로 기한 초과 대상 검색 |
| 3. 쿼리 실행 | Infrastructure (`TaskJpaRepository`) | 데이터베이스에서 조건을 만족하는 업무 조회 |
| 4. 상태 변경 | Domain (`TaskJpaEntity.markDelayed()`) | 업무 엔티티의 상태를 DELAYED로 변경 |
| 5. 이력 기록 | Infrastructure (`TaskStatusHistoryJpaRepository`) | 상태 변경 사실과 전환 정보를 영속화 |
| 6. 관찰 | Infrastructure (`Logger`) | 처리 결과를 로그로 남겨 추적 가능하게 함 |

## ⚠️ 주의 사항

- **단일 인스턴스 전제**: 현재 코드는 서버 인스턴스가 1개일 때만 완전히 안전합니다. 다중 인스턴스 환경에서는 분산 락(ShedLock)을 도입해야 합니다 ([#138](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/138)).
- **청크 처리 미지원**: 현재 모든 대상을 한 번에 조회·전환합니다. 대량 데이터 환경에서는 페이징 처리가 필요할 수 있습니다.
- **삭제 기간 없음**: 스케줄러는 기한 초과 여부만 판단하며, "삭제 후 몇 주 뒤 자동 정리" 같은 보관 기간 정책은 별도 입니다.

## 📚 관련 문서

- [BUSINESS_RULES.md](BUSINESS_RULES.md) — 자동 지연 조건의 비즈니스 규칙 상세
- [REVISION.md](REVISION.md) — 구현 히스토리 및 최종 코드 리뷰 반영 사항
- [CHANGELOG.md](CHANGELOG.md) — 기능 추가 알림
- GitHub [PR #136](https://github.com/mudo-project/Team1-MUDO-BackEnd/pull/136) — 전체 구현 코드
- GitHub [Issue #138](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/138) — 다중 인스턴스 동시 실행 대응 (미해결)

## 🆕 업무 생성 API 흐름

```text
POST /api/workspaces/{workspaceId}/tasks
  → Security Filter
  → AuthUser
  → WorkspaceTaskController
  → CreateTaskRequest
  → CreateTaskCommand
  → CreateTaskUseCase
  → CreateTaskService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → Task.create (Domain Model)
  → TaskRepository.save
  → TaskPersistenceAdapter
  → TaskJpaRepository
  → TaskStatusHistoryRepository.append
  → TaskStatusHistoryPersistenceAdapter
  → TaskStatusHistoryJpaRepository
```

### 1. 인증 정보 추출

`Security Filter`가 Access Token을 검증하고 `AuthUser`를 만든다. `WorkspaceTaskController`는 경로의 `workspaceId`와 `AuthUser`의 `userId`를 받아 요청 본문에는 없는 워크스페이스·생성자 정보를 결정한다.

### 2. 요청 검증과 Command 변환

`CreateTaskRequest`는 제목의 필수 여부와 trim 후 최대 200자, 마감일의 필수 여부를 Bean Validation으로 검증한다. Compact Constructor에서 검증 이전에 미리 `title`을 trim해, `@Size`가 trim 전 원본 길이를 검증하는 것을 방지한다("trim 후 최대 200자" 계약 준수). 검증을 통과하면 `CreateTaskCommand`로 변환한다.

### 3. 워크스페이스 존재 확인과 참여자 검증

`CreateTaskService`는 존재 확인을 권한 확인보다 먼저 한다(기존 워크스페이스 API와 동일한 순서).

- `WorkspaceRepository.findById`(락 없음, 활성 워크스페이스만)로 조회한다. 없으면 `WorkspaceNotFoundException` → `WORKSPACE_404_1`.
- 요청자가 `workspace.getMemberIds()`에 포함되지 않으면 `WorkspaceAccessDeniedException` → `WORKSPACE_403_1`.
- `WORKSPACE:CREATE` 권한 검사는 `permission` 테이블에 코드가 아직 시드되지 않아 TODO 주석으로 남겨져 있다.

### 4. 초기 상태 결정과 저장

`Task.create(workspaceId, title, dueAt, requesterId, today)`가 마감일과 오늘 날짜를 비교해 초기 상태를 결정한다. `dueAt`이 `today`보다 이전이면 `DELAYED`, 그 외(오늘 포함)에는 `WAITING`이다. `today`는 `CreateTaskService`가 소유한 `Clock`으로 계산해 도메인 정적 팩토리에 파라미터로 넘긴다 — 도메인은 `Clock`을 직접 주입받지 않는다.

`TaskRepository.save`는 `TaskPersistenceAdapter`가 구현한다. `id`가 없는 신규 업무이므로 `WorkspaceJpaRepository.getReferenceById`로 워크스페이스 참조를 해결한 뒤 `TaskPersistenceMapper.toEntity`로 엔티티를 조립하고 `saveAndFlush`로 저장해 생성된 `taskId`를 확보한다.

### 5. 상태 이력 기록

저장 직후 `TaskStatusHistoryRepository.append(TaskStatusHistory.userChanged(taskId, null, status, requesterId))`를 호출해 최초 이력을 남긴다. `previousStatus = null`, `changedBy = 요청자 ID`.

### 6. 응답

성공하면 Controller가 `GlobalApiResponse.created(WorkspaceResponseCode.TASK_CREATED, ...)`로 HTTP `201 Created`와 `taskId`를 반환한다.
