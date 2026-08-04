> 작성일: 2026-08-01
> 상태: 🚧 스키마 팀 컨벤션 정합화 완료 · role 기반 결재자 지정 설계 예정

## 🎯 변경 목적

학원 그룹웨어에 전자결재(템플릿 기반 다단계 순차 결재) 기능을 추가한다. 노션 기능명세서와 팀 ERD(erdcloud) 리뷰 결과를 반영해, 다른 도메인과 스키마 컨벤션(멀티테넌시, 공유 테이블, 다중 파일 첨부)을 맞춘다.

---

## ✅ 2026-08-04 · 코드 리뷰 반영 (템플릿 스코프, 결재자 검증, 트랜잭션 롤백)

### 배경

코드 리뷰에서 P1 2건, P2 1건이 발견됐다.

### 확정된 정책

- **템플릿 단건 조회/수정/삭제**가 요청자 정보 없이 ID만으로 동작해, 공유 `template` 테이블(다른 기능도 같은 테이블을 `type` 컬럼으로 구분해 쓰는 구조)에서 다른 학원 또는 다른 기능(type != 'APPROVAL')의 템플릿을 조회·수정·삭제할 수 있는 상태였다. `ApprovalTemplateJpaRepository.findByIdAndType`으로 type을 always 필터링하고, 상세/수정/삭제 3개 API에 요청자 인증을 추가해 학원 일치까지 검증하도록 고쳤다.
- **결재자(approverId) 목록**이 실제 존재 여부·소속 학원 확인 없이 그대로 저장되고 있었다(템플릿 생성/수정, 문서 생성, 결재선 수정 4곳). `ApproverValidator`(공용 검증 컴포넌트)를 추가해 4곳 모두에서 결재자 지정 시 검증하도록 통일했다.
- `SummarizeApprovalAttachmentService`가 요약 실패 시 `markSummaryFailed()`를 저장한 뒤 예외를 던지는데, `@Transactional` 기본 롤백 규칙(RuntimeException 전체 롤백) 때문에 그 저장까지 함께 롤백되는 문제가 있었다. `@Transactional(noRollbackFor = ApprovalException.class)`로 고쳤다.

### 완료 기준

- [x] `./gradlew test` 통과 (신규 유닛 테스트 포함).

---

## ✅ 2026-08-04 · 첨부파일 AI 요약(Gemini) 실제 연동

### 배경

`approval_attachment.ai_summary`/`summary_status`/`summarized_at` 컬럼은 스키마 설계 때부터 있었지만 실제 요약 로직은 없었다. 이번에 Google Gemini API를 실제로 붙였다. 단, `file` 모듈이 아직 `fileId → 실제 파일 내용(objectKey 등)` 조회를 제공하지 않아(파일 메타데이터 테이블 자체가 없음), 실제 파일 내용을 읽어 요약하는 것은 불가능한 상태다.

### 확정된 정책

- 요약 트리거는 업로드 시 자동이 아니라, 클라이언트가 명시적으로 호출하는 동기 API(`POST .../attachments/{fileId}/summarize`)로 한다. 비동기 큐를 두지 않았다 (Gemini 호출이 수 초 내 끝나는 것을 전제로 한 단순화 — 트래픽이 커지면 재검토 필요).
- **실제 파일 내용이 아니라 placeholder 텍스트를 Gemini에 보낸다.** `file` 모듈이 조회 기능을 제공하기 전까지 진짜 요약이 아니라는 점을 README/API.md에 경고 문구로 명시했다.
- Gemini 연동은 `application.port.AttachmentSummarizerPort`(추상화) + `infrastructure/external/gemini`의 `GeminiSummarizerAdapter`(구현)로 분리했다. 나중에 다른 제공자(OpenAI 등)로 바꾸거나 file 모듈 연동이 준비되면, 인터페이스는 그대로 두고 어댑터/서비스 내부 프롬프트 생성부만 바꾸면 된다.
- API 키는 `GEMINI_API_KEY` 환경변수로만 받는다(코드/설정 파일에 값이 들어가지 않도록). `GEMINI_MODEL`(기본값 `gemini-2.0-flash`)도 환경변수로 바꿀 수 있게 했다. `file` 모듈의 `S3Properties`와 동일한 `@Value` 기반 패턴을 따랐다.
- 요약 실패 시 `summaryStatus`를 `FAILED`로 남기고 `502`(`APPROVAL_502_1`)를 반환한다.

### 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `ApprovalAttachment.applySummary/markSummaryFailed`, `ApprovalDocument.findAttachmentByFileId` 추가, `ApprovalErrorCode`에 `ATTACHMENT_NOT_FOUND`/`SUMMARY_GENERATION_FAILED` 추가 |
| Application | `AttachmentSummarizerPort`(+ `AttachmentSummarizationException`), `SummarizeApprovalAttachmentUseCase`/`Command`/`View`/`Service` 추가 |
| Infrastructure | `infrastructure/external/gemini` 패키지 신설: `GeminiProperties`, `GeminiConfig`(RestClient 빈), `GeminiSummarizerAdapter`, Gemini 요청/응답 DTO |
| Presentation | `ApprovalController`에 `POST /{documentId}/attachments/{fileId}/summarize` 추가 |

### 완료 기준

- [x] `GEMINI_API_KEY`가 설정되면 Gemini API를 호출해 요약을 받아 `aiSummary`/`summaryStatus`/`summarizedAt`에 반영한다.
- [x] Gemini 호출 실패 시 `summaryStatus`가 `FAILED`로 저장되고 `502`를 반환한다.
- [x] `./gradlew compileJava` / `./gradlew test`(전체 Spring 컨텍스트 로딩 포함) 통과 — Gemini 관련 빈 배선에 문제가 없음을 확인.

---

## ✅ 2026-08-04 · Web Push 백엔드 준비 (이벤트 발행 + 구독 저장)

### 배경

결재 차례가 돌아왔을 때 Web Push로 알려주는 기능은 초기 설계 때부터 필요성이 언급됐지만 계속 미착수 상태였다. 이번에 실제 푸시 발송(VAPID, 프론트 서비스워커)은 아직 준비되지 않았다는 전제 하에, 백엔드에서 먼저 만들 수 있는 두 조각만 구현했다.

### 확정된 정책

- `ApprovalDocument`에 `currentPendingApproverId()`를 공개 메서드로 추가했다.
- `DecideApprovalLineService`가 승인 처리로 다음 결재자 라인이 활성화되면(문서가 여전히 `IN_PROGRESS`) `ApprovalLineActivatedEvent`(documentId, documentTitle, approverId, activatedAt)를 Spring `ApplicationEventPublisher`로 발행한다. 이 이벤트를 소비하는 리스너는 아직 만들지 않았다 — 만들면 안 되는 게 아니라, 실제로 보낼 방법(VAPID/web-push 라이브러리)이 없어서 만들 수 없었다.
- 브라우저 푸시 구독 정보(`endpoint`/`p256dh`/`auth`)를 저장하는 `PushSubscription` 도메인과 `/api/approvals/push-subscriptions` 등록/해지 API를 추가했다. 같은 사용자·`endpoint`로 재등록하면 새 행을 만들지 않고 키만 갱신한다 (브라우저가 구독을 주기적으로 재발급하는 경우 대비).
- **임시 배치 결정**: Web Push 구독은 approval 전용 개념이 아니라 여러 기능이 함께 쓸 수 있는 범용 기능이지만, 지금은 이 기능을 필요로 하는 곳이 approval뿐이라 별도 `notification` 모듈을 새로 만들지 않고 approval 패키지 안에 두었다. 다른 기능(예: 공지사항 알림)이 Web Push를 쓰게 되면, 그때 별도 모듈로 분리하는 걸 검토한다.

### 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `PushSubscription`, `ApprovalLineActivatedEvent`, `ApprovalDocument.currentPendingApproverId()` 추가 |
| Application | `RegisterPushSubscriptionUseCase`/`UnregisterPushSubscriptionUseCase` 및 서비스 추가, `DecideApprovalLineService`에 이벤트 발행 로직 추가 |
| Infrastructure | `PushSubscriptionEntity`/`PushSubscriptionJpaRepository`/`PushSubscriptionRepositoryImpl` 추가 |
| Presentation | `PushSubscriptionController`(`/api/approvals/push-subscriptions`) 추가 |
| Migration | `V1.2.4__create_push_subscription_table.sql` 추가 |

### 완료 기준

- [x] 결재 승인 시 다음 결재자가 있으면 이벤트가 발행된다 (리스너는 없음, 발행만 확인).
- [x] 같은 사용자·endpoint로 두 번 등록해도 행이 중복 생성되지 않는다.
- [x] `./gradlew compileJava` / `./gradlew test` 통과.

---

## ✅ 2026-08-04 · 전용 ErrorCode 도입 및 목록 API 페이지네이션

### 배경

`users`/`auth` 모듈이 머지되며 `UserErrorCode`/`UserException` 형태의 도메인 전용 에러코드 선례가 생겼다. approval도 그동안 미룬 전용 `ErrorCode`를 이 선례에 맞춰 도입했다. 동시에, `docs/API_CONTRACT.md`에 정의돼 있었지만 미반영 상태였던 페이지네이션 규칙을 목록 API 3개에 적용했다.

### 확정된 정책

- `ApprovalErrorCode`(enum, `ErrorCode` 구현) + `ApprovalException`(`BusinessException` 상속)을 추가하고, 기존 `BadRequestException`/`NotFoundException`/`ForbiddenException`/`ConflictException` 직접 사용을 전부 교체했다. 코드 체계는 `APPROVAL_{HTTP상태}_{순번}` (예: `APPROVAL_404_1`).
- `page`(0부터)/`size` 쿼리 파라미터와 Spring Data `Slice`(전체 개수 미계산)를 사용해, `내 결재함`/`내가 신청한 결재`/`템플릿 목록` 3개 API에 페이지네이션을 적용했다. 응답은 `global`의 공용 `SliceResponse<T>`(`content`/`page`/`size`/`hasNext`)로 감싼다.
- `ApprovalQueryService.getMyPendingCount`가 쓰는 `findAllByApproverId(Long)`(전체 스캔, 카운트용)는 그대로 두고, 목록 조회용 페이지네이션 오버로드를 별도로 추가했다 — 두 용도가 다르기 때문이다.

### 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `ApprovalErrorCode`, `ApprovalException` 추가; 리포지토리 인터페이스에 페이지네이션 오버로드 추가 |
| Application | `ApprovalQueryUseCase`/`ApprovalTemplateQueryUseCase`가 `PageResult<T>`(`global.domain.common.page`) 반환하도록 변경 |
| Infrastructure | JPA 리포지토리가 `Slice<Entity>` + `Pageable` 기반으로 변경 |
| Presentation | 컨트롤러가 `page`/`size` 쿼리 파라미터를 받고 `SliceResponse<T>`로 응답 |

### 완료 기준

- [x] approval 코드에 `global.domain.common.exception`의 범용 예외 직접 사용이 남아있지 않다.
- [x] `./gradlew compileJava` / `./gradlew test` 통과.

---

## ✅ 2026-08-04 · 시각 생성 책임을 호출부로 이전 (KST 고정)

### 배경

서버 JVM 기본 시간대가 UTC로 설정되어 있어, 도메인 코드가 직접 `LocalDateTime.now()`를 호출하면 실행 환경에 따라 저장 시각이 9시간 어긋날 수 있는 구조적 위험이 있었다. `global` 모듈에 `Asia/Seoul` 고정 `Clock` 빈이 추가되면서, approval 도메인에 남아있던 마지막 6곳을 정리했다.

### 확정된 정책

- 도메인 모델(`ApprovalTemplate`, `ApprovalDocument`, `ApprovalDocumentLine`)은 더 이상 스스로 `LocalDateTime.now()`를 호출하지 않는다. 대신 시각을 파라미터로 전달받아 그대로 저장한다.
  - `ApprovalTemplate.create(...)` / `update(...)`
  - `ApprovalDocument.create(...)` / `markResubmitted(...)` / `decide(...)`(내부적으로 `ApprovalDocumentLine.approve/reject`에 그대로 전달)
- 호출부(서비스 5곳)는 `Clock`을 주입받아 `LocalDateTime.now(clock)`으로 시각을 만들어 넘긴다.
- `ApprovalTemplateEntity`(공유 `template` 테이블 매핑)는 `global`의 `BaseTimeEntity`를 상속하도록 변경해, `created_at`/`updated_at`을 JPA Auditing(`Asia/Seoul` 고정 `Clock` 기반)이 자동으로 채우게 했다.
- `ApprovalDocumentEntity`의 `created_at`은 `CreatedAtEntity`로 전환하지 않고 기존처럼 도메인이 넘겨준 값을 그대로 저장한다. Repository 구현이 저장할 때마다 엔티티를 새로 생성해 `save()`(merge)하는 구조라, Auditing으로 전환하면 병합 시 `createdAt`이 유실될 위험이 있어 이번 범위에서는 제외했다(추후 별도 리팩터링에서 fetch-후-mutate 패턴으로 정리되면 전환 검토).

### 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `ApprovalTemplate.create/update`, `ApprovalDocument.create/markResubmitted/decide`, `ApprovalDocumentLine.approve/reject`에 `LocalDateTime now` 파라미터 추가 |
| Application | `CreateApprovalTemplateService`, `UpdateApprovalTemplateService`, `CreateApprovalDocumentService`, `ResubmitApprovalDocumentService`, `DecideApprovalLineService`에 `Clock` 주입 및 호출부 수정 |
| Infrastructure | `ApprovalTemplateEntity`가 `BaseTimeEntity`를 상속하도록 변경, `ApprovalTemplateRepositoryImpl`에서 수동 `createdAt`/`updatedAt` 대입 제거 |

### 완료 기준

- [x] approval 도메인 코드에 `LocalDateTime.now()` 직접 호출이 남아있지 않다.
- [x] `./gradlew compileJava` / `./gradlew test` 통과.

---

## ✅ 2026-08-03 · CodeRabbit 리뷰 반영 (데이터 격리 강화)

### 확정된 정책

- 결재 문서에 `resubmittedAt`을 추가해, 반려된 문서 1건당 재상신은 1회만 허용한다.
- 템플릿 목록 조회는 요청자의 `academyId`로 스코프를 제한한다 (`findAllByTypeAndAcademyId`).
- 결재 신청 시 신청자 학원과 템플릿 학원이 다르면 `ForbiddenException`을 던진다.
- `template`-`approval_document` FK를 `(template_id, academy_id)` 복합키로 바꿔 DB 레벨에서도 교차 학원 참조를 막는다.
- `approval_line_step`의 `role_id`/`approver_id`는 정확히 하나만 채워지도록 DB `CHECK` 제약과 도메인 검증을 함께 건다.

### 완료 기준

- [x] 재상신 반복 호출 시 두 번째 시도부터 `409`를 반환한다.
- [x] 다른 학원 템플릿이 목록/신청에 섞이지 않는다.
- [x] `./gradlew test` 통과.

---

## ✅ 2026-08-03 · 팀 ERD 최종 반영 및 부가 기능 3종

### 확정된 정책

- 테이블명을 팀 ERD 최종안으로 변경: `approval_document`, `approval_step`, `approval_line_step`, `approval_attachment`.
- `approval_line_step`에 `role_id`를 추가한다 (역할 기반 결재자 지정 스키마만 우선 반영, 해석 로직은 role 테이블이 없어 보류).
- 결재 문서 첨부파일을 단일 `file_id`에서 `approval_attachment`(다대다) 다중 첨부로 전환하고, AI 요약용 컬럼(`ai_summary`/`summary_status`/`summarized_at`)을 함께 추가한다.
- 반려된 결재의 재상신, 템플릿 목록의 결재자 이름 노출, 결재 대기 건수 조회 API를 추가한다.

### 영향 범위

| 계층 | 변경 내용 |
| --- | --- |
| Domain | `ApprovalAttachment`, `AttachmentSummaryStatus` 추가, `ApprovalTemplateLine`에 `roleId` 추가, `ApprovalContent`에서 파일 필드 제거 |
| Application | 첨부파일 목록(`fileIds`)을 다루도록 Command/View 전체 수정, 재상신·대기건수 UseCase 추가 |
| Infrastructure | 엔티티/테이블명 전체 정합화, `ApprovalAttachmentEntity` 추가 |
| Migration | `V1.2.1`~`V1.2.3` 재작성 |

### 완료 기준

- [x] 결재 문서에 파일 여러 개를 첨부할 수 있다.
- [x] 반려된 결재를 같은 내용으로 재상신할 수 있다.
- [x] 사이드바 뱃지용 대기 건수 API가 목록 API 없이 동작한다.

---

## ✅ 2026-08-01 · DB 컨벤션 1차 정합화

### 확정된 정책

- 템플릿/문서에 `academy_id`(멀티테넌시) 컬럼을 추가한다.
- 컬럼명을 팀 공용 컨벤션에 맞춘다: `created_by`, `requester_user_id`, `approver_user_id`.
- 결재 템플릿은 approval 전용 테이블 대신 팀이 여러 기능에서 공유하는 `template` 테이블을 `type='APPROVAL'`로 재사용한다.

### 처리 흐름

```text
팀 erdcloud "템플릿" 테이블 스크린샷 리뷰
→ academy_id, file_id, type, created_by, updated_at 컬럼 존재 확인
→ 결재 템플릿 자체 테이블 대신 공유 template 재사용 결정
→ ApprovalTemplateEntity를 template 테이블에 매핑, type 컬럼에 "APPROVAL" 고정
```

---

## ✅ 2026-07-31 · 템플릿/문서 도메인 분리 (최초 리팩터링)

### 변경 목적 (AS-IS → TO-BE)

- AS-IS: 결재 "템플릿"을 만드는 시점에 제목·내용·결재선을 한 번에 입력해, 템플릿과 실제 결재 신청 건이 사실상 하나로 합쳐져 있었다.
- TO-BE: 노션 기능명세서 기준으로 "템플릿(틀, 재사용)"과 "문서(실제 신청 1건)"를 별도 Aggregate로 분리한다.

### 확정된 정책

- `ApprovalTemplate`: 이름 + 기본 결재선만 가진다. 내용(텍스트/파일)이 없다.
- `ApprovalDocument`: 템플릿을 참조하고, 제목·내용·실제 결재선·진행 상태를 가진다.
- 성공 응답은 팀 공용 `GlobalApiResponse<T>` 봉투로 감싼다.
- API 경로에 `/api/v1` 버전 프리픽스를 적용한다. (→ 2026-08-02에 되돌림, 아래 참고)

### 완료 기준

- [x] 템플릿 CRUD와 결재 신청·조회·승인·반려가 각각 독립된 API로 분리된다.
- [x] 순차 결재(1차 → 2차 → …) 로직이 도메인에 구현된다.

---

## ✅ 2026-08-02 · API 버전 프리픽스 제거

- `/api/v1/approvals`, `/api/v1/approval-templates`를 `/api/approvals`, `/api/approval-templates`로 되돌렸다.
- 사유: 아직 실배포 전이라 버전 호환성을 고려할 클라이언트가 없고, 프로젝트가 API 버전 정책을 전면 도입하지 않기로 결정했다 (CodeRabbit이 호환성 문제로 지적했으나, 사용자 명시적 결정으로 유지).

## 📌 후속 문서

완료된 변경의 사용자 관점 요약은 [CHANGELOG.md](CHANGELOG.md)에서 확인할 수 있다.
