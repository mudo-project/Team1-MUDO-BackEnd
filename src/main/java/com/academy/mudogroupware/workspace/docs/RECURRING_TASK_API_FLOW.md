# 🔁 반복 업무 템플릿 API 호출 흐름

> `TASK_API_FLOW.md`가 업무 생성·수정·삭제 흐름을 문서화하듯, 이 문서는 반복 업무 템플릿 생성·목록 조회·수정 API의 호출 흐름을 다룬다.

## 🆕 반복 업무 템플릿 생성 API 흐름

```text
POST /api/workspaces/{workspaceId}/recurring-templates
  → Security Filter
  → AuthUser
  → WorkspaceRecurringTaskTemplateController
  → CreateRecurringTaskTemplateRequest
  → CreateRecurringTaskTemplateCommand
  → CreateRecurringTaskTemplateUseCase
  → CreateRecurringTaskTemplateService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → RecurringTaskTemplate.create (Domain Model, 반복 규칙 검증)
  → RecurringTaskTemplateRepository.save
  → RecurringTaskTemplatePersistenceAdapter
  → RecurringTaskTemplateJpaRepository
```

### 1. 요청 검증과 Command 변환

`CreateRecurringTaskTemplateRequest`는 제목의 필수 여부와 trim 후 최대 200자, `recurrenceType`의 필수 여부를 Bean Validation으로 검증한다. Compact Constructor에서 검증 이전에 미리 `title`을 trim한다(`CreateTaskRequest`와 동일한 방식). 검증을 통과하면 `toCommand(authUser, workspaceId)`가 `CreateRecurringTaskTemplateCommand`로 변환한다.

### 2. 워크스페이스 존재 확인과 참여자 검증

`CreateRecurringTaskTemplateService`는 업무 생성 API와 동일한 순서를 따른다 — `WorkspaceRepository.findById`(락 없음)로 조회 후 없으면 `WorkspaceNotFoundException`(`404_1`), 요청자가 `workspace.getMemberIds()`에 포함되지 않으면 `WorkspaceAccessDeniedException`(`403_1`).

### 3. 반복 규칙 검증과 저장

`RecurringTaskTemplate.create(...)`가 생성자에서 `validateRule(recurrenceType, recurrenceRule)`을 호출해 주기 타입에 맞는 규칙인지 검증한다.

- `WEEKLY`: `recurrenceRule.daysOfWeek`가 1~7 사이의 정수 목록이어야 하며 최소 1개 필요.
- `MONTHLY`: `recurrenceRule.dayOfMonth`가 정수 `1`이어야 한다(제품 요구사항으로 매달 1일만 허용, 2026-08-08 결정).

정수 여부는 `Number.doubleValue()`가 `Math.floor()`와 같은지 비교해 판별한다 — `intValue()`만으로는 `1.5` 같은 소수 값이 잘림으로 인해 범위 검증을 우회할 수 있기 때문이다. 검증에 실패하면 `InvalidRecurrenceRuleException`(`400_7`)이 발생한다.

생성자는 호출자가 넘긴 `recurrenceRule` Map을 그대로 참조하지 않고 깊은 불변 복사본을 만들어(`copyOf`) 저장한다 — 저장 이후 호출자가 원본 Map을 변경해도 도메인 객체 내부 상태에 영향을 주지 않는다.

`RecurringTaskTemplateRepository.save`는 `RecurringTaskTemplatePersistenceAdapter`가 구현한다. `id`가 없는 신규 템플릿이므로 `WorkspaceJpaRepository.getReferenceById`로 워크스페이스 참조를 해결한 뒤 엔티티를 생성해 저장하고, 생성된 `templateId`를 확보한다.

### 4. 응답

성공하면 Controller가 `GlobalApiResponse.created(WorkspaceResponseCode.RECURRING_TEMPLATE_CREATED, ...)`로 HTTP `201 Created`와 `templateId`를 반환한다.

---

## 📋 반복 업무 템플릿 목록 조회 API 흐름

```text
GET /api/workspaces/{workspaceId}/recurring-templates?page=0&size=20
  → Security Filter
  → AuthUser
  → WorkspaceRecurringTaskTemplateController
  → GetRecurringTaskTemplatesUseCase
  → GetRecurringTaskTemplatesService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → RecurringTaskTemplateRepository.findAllByWorkspaceId(workspaceId, page, size)
  → RecurringTaskTemplatePersistenceAdapter
  → RecurringTaskTemplateJpaRepository (Slice 조회)
  → SliceResponse.from(...) (Presentation)
```

이 API는 생성과 달리 Command DTO가 없다 — 경로 변수(`workspaceId`)와 쿼리 파라미터(`page`, `size`)만으로 조회 조건이 결정된다.

### 1. 워크스페이스 존재 확인과 참여자 검증

`GetRecurringTaskTemplatesService`는 생성 API와 동일한 순서를 따른다 — 존재 확인(`404_1`) → 참여자 확인(`403_1`). 조회 전용이므로 클래스에 `@Transactional(readOnly = true)`를 적용한다.

### 2. 페이지 조회

검증을 통과하면 `RecurringTaskTemplateRepository.findAllByWorkspaceId(workspaceId, page, size)`에 위임한다. `RecurringTaskTemplatePersistenceAdapter`는 `PageRequest.of(page, size)`로 `Pageable`을 만들어 `RecurringTaskTemplateJpaRepository`의 `Slice` 반환 메서드를 호출한다.

JPQL은 `order by t.createdAt desc, t.id desc`로 정렬한다. `createdAt`만으로는 짧은 간격으로 연달아 생성된 템플릿의 타임스탬프가 동일하게 저장될 수 있어(시스템 시계 해상도 문제) 정렬 순서가 조회마다 달라질 수 있다. `id`는 생성·커밋 순서를 보장하지는 않지만 항상 고유한 값이므로, 이를 2차 정렬 기준으로 추가하면 `createdAt`이 같은 행들 사이에서도 정렬 결과가 항상 동일하게(결정적으로) 유지된다 — `AttendanceCorrectionRequestRepositoryImpl`이 `requestedAt desc, id desc`로 쓰는 것과 같은 패턴이다.

전체 개수(count) 쿼리는 실행하지 않는다. `Slice.hasNext()`는 `size + 1`건을 조회해 다음 페이지 존재 여부만 판단하는 방식으로 동작하므로, 전체 개수가 필요 없는 이 조회에 적합하다(`docs/API_CONTRACT.md`의 페이지네이션 규칙 참고).

### 3. 응답 조립

`RecurringTaskTemplatePersistenceAdapter`가 `Slice`를 공용 `PageResult<RecurringTaskTemplate>`로 변환해 Service·UseCase 계층까지 Spring Data 타입이 노출되지 않도록 한다. Controller는 `SliceResponse.from(pageResult, RecurringTaskTemplateListResponse::from)`으로 응답 DTO 목록과 페이지 메타데이터(`content`, `page`, `size`, `hasNext`)를 조립한다. 이 패턴은 `notice` 도메인의 목록 조회와 동일하다.

성공하면 Controller가 `GlobalApiResponse.ok(WorkspaceResponseCode.RECURRING_TEMPLATE_LIST_RETRIEVED, ...)`로 HTTP `200 OK`를 반환한다.

---

## ✏️ 반복 업무 템플릿 수정 API 흐름

```text
PATCH /api/workspaces/{workspaceId}/recurring-templates/{templateId}
  → Security Filter
  → AuthUser
  → WorkspaceRecurringTaskTemplateController
  → UpdateRecurringTaskTemplateRequest
  → UpdateRecurringTaskTemplateCommand
  → UpdateRecurringTaskTemplateUseCase
  → UpdateRecurringTaskTemplateService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → RecurringTaskTemplateRepository.findByWorkspaceIdAndId (락 없음)
  → RecurringTaskTemplate.changeRecurrence (Domain Model, 반복 규칙 재검증)
  → RecurringTaskTemplateRepository.save
  → RecurringTaskTemplatePersistenceAdapter
  → RecurringTaskTemplateJpaRepository
```

### 1. 요청 검증과 Command 변환

`UpdateRecurringTaskTemplateRequest`는 두 가지를 `@AssertTrue`로 검증한다.

- `isAtLeastOneFieldPresent()`: `title`·`recurrenceType`·`recurrenceRule`을 모두 생략하면 `400`.
- `isRecurrencePairComplete()`: `recurrenceType`과 `recurrenceRule`은 항상 함께 오거나 함께 없어야 한다 — 하나만 보내면 `400`. `recurrenceType`이 바뀌면 `recurrenceRule`의 유효한 모양도 함께 바뀌므로(예: `WEEKLY`→`MONTHLY`), 두 값을 독립적으로 부분 수정하게 두지 않는다.

Compact Constructor에서 `title`이 있으면 미리 trim한다. 검증을 통과하면 `toCommand(authUser, workspaceId, templateId)`가 `UpdateRecurringTaskTemplateCommand`로 변환한다.

### 2. 워크스페이스 존재 확인과 참여자 검증

`UpdateRecurringTaskTemplateService`는 생성·목록 조회 API와 동일한 순서를 따른다 — `WorkspaceRepository.findById`(락 없음)로 조회 후 없으면 `WorkspaceNotFoundException`(`404_1`), 참여자가 아니면 `WorkspaceAccessDeniedException`(`403_1`).

### 3. 템플릿 조회와 값 병합

`RecurringTaskTemplateRepository.findByWorkspaceIdAndId(workspaceId, templateId)`로 조회한다(락 없음 — 삭제 API가 아직 없어 삭제와의 동시 경합이 존재하지 않는다). 없으면 `RecurringTaskTemplateNotFoundException`(`404_5`).

`command.title()`이 `null`이면 기존 제목을 유지하고, `command.recurrenceType()`이 `null`이면 기존 `recurrenceType`·`recurrenceRule`을 그대로 다시 넘긴다(Request 검증으로 인해 `recurrenceType`이 `null`이면 `recurrenceRule`도 항상 `null`이다). 병합된 값으로 `template.changeRecurrence(newTitle, newType, newRule)`을 호출한다 — 이 메서드는 내부적으로 새 `RecurringTaskTemplate` 인스턴스를 만들며 생성자와 동일한 `validateRule`을 다시 실행하므로, 값이 바뀌지 않은 경우에도 기존 규칙이 여전히 유효한지 재검증된다.

### 4. 저장과 응답

`RecurringTaskTemplateRepository.save`는 `id`가 있으므로 `RecurringTaskTemplatePersistenceAdapter`가 기존 엔티티를 조회해 `changeRecurrence`로 갱신한다(생성과 같은 `save` 메서드, 분기만 다름). 성공하면 Controller가 `GlobalApiResponse.ok(WorkspaceResponseCode.RECURRING_TEMPLATE_UPDATED, ...)`로 HTTP `200 OK`와 반영된 `templateId`·`title`·`recurrenceType`·`recurrenceRule`을 반환한다.

## 🗑️ 반복 업무 템플릿 삭제 API 흐름

```text
DELETE /api/workspaces/{workspaceId}/recurring-templates/{templateId}
  → Security Filter
  → AuthUser
  → WorkspaceRecurringTaskTemplateController
  → DeleteRecurringTaskTemplateCommand
  → DeleteRecurringTaskTemplateUseCase
  → DeleteRecurringTaskTemplateService
  → WorkspaceRepository.findById (락 없음)
  → WorkspacePersistenceAdapter
  → RecurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate (락 없는 소속 확인 → 비관적 락, 2단계)
  → RecurringTaskTemplateRepository.delete
  → RecurringTaskTemplatePersistenceAdapter
  → RecurringTaskTemplateJpaRepository / RecurringTaskSkipJpaRepository
```

### 1. 존재 확인과 참여자 검증

`DeleteRecurringTaskTemplateService`는 다른 반복 업무 템플릿 API와 동일한 순서를 따른다 — `WorkspaceRepository.findById`(락 없음)로 조회 후 없으면 `WorkspaceNotFoundException`(`404_1`), 참여자가 아니면 `WorkspaceAccessDeniedException`(`403_1`).

### 2. 비관적 락 조회

`RecurringTaskTemplateRepository.findByWorkspaceIdAndIdForUpdate(workspaceId, templateId)`는 `TaskRepository.findByIdForUpdate`와 동일한 2단계로 동작한다. ① `existsByIdAndWorkspaceId`로 락 없이 워크스페이스 소속을 먼저 확인하고, 소속이 아니면 즉시 `RecurringTaskTemplateNotFoundException`(`404_5`)이 발생한다. ② 소속이 확인된 templateId에 대해서만 비관적 락을 건다. 수정 API의 `findByWorkspaceIdAndIdForUpdate`와 같은 락을 공유하므로, 수정과 삭제가 동시에 들어오면(같은 워크스페이스·같은 템플릿) 뒤에 도착한 트랜잭션이 먼저 완료된 트랜잭션의 결과를 보게 된다.

### 3. 삭제 실행과 이미 생성된 업무 처리

`RecurringTaskTemplateRepository.delete(templateId)`는 자식(`recurring_task_skip`) → 부모(`recurring_task_template`) 순서로 하드 삭제한다. 템플릿으로 이미 생성된 `Task` 행은 삭제되지 않는다 — `task.recurring_template_id`가 운영 마이그레이션에서 `ON DELETE SET NULL`로 정의되어 있어 `NULL`로만 바뀌고 "일반 업무"로 남는다. `TaskJpaEntity.recurringTemplate`의 `@OnDelete(action = OnDeleteAction.SET_NULL)`이 이 동작을 `@DataJpaTest`(H2)에도 동일하게 재현한다.

### 4. 응답

성공하면 Controller가 `GlobalApiResponse.ok(WorkspaceResponseCode.RECURRING_TEMPLATE_DELETED)`로 `200 OK`를 반환한다.

## 📚 관련 문서

- [RECURRING_TASK_API.md](RECURRING_TASK_API.md) — 반복 업무 템플릿 API 명세
- [TASK_API_FLOW.md](TASK_API_FLOW.md) — 업무(Task) 생성·수정·삭제 API 호출 흐름
- `docs/API_CONTRACT.md` — 프로젝트 공통 페이지네이션·응답 형식 규칙
