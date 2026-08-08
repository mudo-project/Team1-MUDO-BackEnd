# 🔁 반복 업무 템플릿 API 호출 흐름

> `TASK_API_FLOW.md`가 업무 생성·수정·삭제 흐름을 문서화하듯, 이 문서는 반복 업무 템플릿 생성·목록 조회 API의 호출 흐름을 다룬다.

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

JPQL은 `order by t.createdAt desc, t.id desc`로 정렬한다. `createdAt`만으로는 짧은 간격으로 연달아 생성된 템플릿의 타임스탬프가 동일하게 저장될 수 있어(시스템 시계 해상도 문제) 정렬 순서가 조회마다 달라질 수 있다. `id`(고유하고 항상 생성 순서와 일치)를 2차 정렬 기준으로 추가해 정렬 결과를 항상 동일하게(결정적으로) 만든다 — `AttendanceCorrectionRequestRepositoryImpl`이 `requestedAt desc, id desc`로 쓰는 것과 같은 패턴이다.

전체 개수(count) 쿼리는 실행하지 않는다. `Slice.hasNext()`는 `size + 1`건을 조회해 다음 페이지 존재 여부만 판단하는 방식으로 동작하므로, 전체 개수가 필요 없는 이 조회에 적합하다(`docs/API_CONTRACT.md`의 페이지네이션 규칙 참고).

### 3. 응답 조립

`RecurringTaskTemplatePersistenceAdapter`가 `Slice`를 공용 `PageResult<RecurringTaskTemplate>`로 변환해 Service·UseCase 계층까지 Spring Data 타입이 노출되지 않도록 한다. Controller는 `SliceResponse.from(pageResult, RecurringTaskTemplateListResponse::from)`으로 응답 DTO 목록과 페이지 메타데이터(`content`, `page`, `size`, `hasNext`)를 조립한다. 이 패턴은 `notice` 도메인의 목록 조회와 동일하다.

성공하면 Controller가 `GlobalApiResponse.ok(WorkspaceResponseCode.RECURRING_TEMPLATE_LIST_RETRIEVED, ...)`로 HTTP `200 OK`를 반환한다.

## 📚 관련 문서

- [RECURRING_TASK_API.md](RECURRING_TASK_API.md) — 반복 업무 템플릿 API 명세
- [TASK_API_FLOW.md](TASK_API_FLOW.md) — 업무(Task) 생성·수정·삭제 API 호출 흐름
- `docs/API_CONTRACT.md` — 프로젝트 공통 페이지네이션·응답 형식 규칙
