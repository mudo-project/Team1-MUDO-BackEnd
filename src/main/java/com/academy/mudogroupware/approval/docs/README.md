# approval 모듈

## 책임과 범위

전자결재 기능을 담당한다. 두 가지 Aggregate로 구성된다.

- **ApprovalTemplate(결재 템플릿)**: 행정 직원이 미리 만들어두는 결재 틀. 이름 + 기본 결재선(순서 있는 결재 담당자 목록)만 가진다. 팀이 여러 기능에서 공유하는 `template` 테이블을 `type='APPROVAL'`로 재사용한다.
- **ApprovalDocument(결재 문서)**: 사용자가 템플릿을 선택해 실제로 등록하는 결재 신청 건. 제목, 내용(텍스트/첨부파일 여러 개), 실제 결재선, 진행 상태(진행중/승인/반려)를 가진다. 순차 승인만 지원하며 병렬결재는 지원하지 않는다(의도적 설계). 반려된 문서는 1회에 한해 재상신할 수 있다.

## 담당자

(팀 확인 필요 — 이 저장소 초기 세팅 및 approval 도메인 작업자: minseopark0327)

## 소유하는 주요 데이터와 상태

- `ApprovalTemplate`, `ApprovalTemplateLine` — DB 테이블 `template`(공유, `type='APPROVAL'`), `approval_line_step`
- `ApprovalDocument`, `ApprovalDocumentLine`, `ApprovalAttachment` — DB 테이블 `approval_document`, `approval_step`, `approval_attachment`
- `PushSubscription`(Web Push 구독 정보) — DB 테이블 `push_subscription` (`user_id`+`endpoint` 유니크, 실제 알림 발송 로직은 아직 없음)
- `ApprovalDocument` 상태: `IN_PROGRESS` → `APPROVED` 또는 `REJECTED`, 재상신 여부는 `resubmittedAt`으로 별도 추적
- `ApprovalDocumentLine` 상태: `WAITING` → `PENDING` → `APPROVED` 또는 `REJECTED`
- 모든 테이블에 `academy_id`(멀티테넌시) 컬럼이 있으며, 조회/생성 시 요청자 학원으로 스코프를 검증한다.

## 외부에 공개하는 Application API

템플릿 (`/api/approval-templates`):
- `CreateApprovalTemplateUseCase` — 템플릿 생성
- `ApprovalTemplateQueryUseCase` — 템플릿 목록(학원 스코프, 결재선 이름 포함)/상세 조회
- `UpdateApprovalTemplateUseCase` — 템플릿 항목(이름·라인) 수정
- `DeleteApprovalTemplateUseCase` — 템플릿 삭제

결재 문서 (`/api/approvals`):
- `CreateApprovalDocumentUseCase` — 결재 신청 (다중 파일 첨부, 학원 교차 신청 차단)
- `ApprovalQueryUseCase` — 내게 온 결재 / 내가 신청한 결재 / 대기 건수 / 상세 조회
- `UpdateApprovalDocumentLinesUseCase` — 결재선 수정 (이미 처리된 앞 단계는 유지, 이후 단계만 교체 가능)
- `DecideApprovalLineUseCase` — 결재 승인/반려 (승인으로 다음 결재자 라인이 활성화되면 `ApprovalLineActivatedEvent` 발행)
- `ResubmitApprovalDocumentUseCase` — 반려된 결재 재상신 (1회 제한)

Web Push 구독 (`/api/approvals/push-subscriptions`):
- `RegisterPushSubscriptionUseCase` — 브라우저 푸시 구독 정보(endpoint/p256dh/auth) 등록 (같은 사용자·endpoint면 키 갱신)
- `UnregisterPushSubscriptionUseCase` — 구독 해지

첨부파일 AI 요약 (`/api/approvals/{documentId}/attachments/{fileId}/summarize`):
- `SummarizeApprovalAttachmentUseCase` — Gemini API를 호출해 첨부파일 요약을 생성하고 `approval_attachment`에 반영

세부 요청/응답 형식은 [docs/API.md](API.md), 계층별 호출 흐름은 [docs/API_FLOW.md](API_FLOW.md) 참고.

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **결재자·생성자 이름/역할/소속학원 조회**: `ApproverDirectoryPort`(application/port)로 추상화되어 있으나, User 도메인 모듈이 아직 없어 `infrastructure/persistence`에 `users` 테이블을 직접 읽는 임시 shim(`UserNameEntity`)으로 구현되어 있다. **MODULES.md의 "다른 도메인 JPA Entity 직접 참조 금지" 규칙 위반 상태이며, User 모듈이 생기면 정식 Port 구현으로 교체해야 한다.** (notice 모듈에도 같은 성격의 별도 shim이 있다 — User 모듈 생기면 둘 다 교체 필요)
- **파일 업로드**: 결재 문서의 첨부파일은 공유 `file_id`(BIGINT) 목록만 저장한다. 실제 업로드(presigned URL 발급)는 `file` 모듈이 담당하며, approval 모듈은 직접 연동하지 않는다.
- **인증 사용자 정보**: `global.presentation.security.AuthUser`(JWT 인증 principal)를 컨트롤러에서 사용한다.
- **AI 요약(Gemini)**: `AttachmentSummarizerPort`(application/port)로 추상화되어 있고, `infrastructure/external/gemini`의 `GeminiSummarizerAdapter`가 Google Gemini `generateContent` REST API를 직접 호출해 구현한다. `GEMINI_API_KEY`(필수)/`GEMINI_MODEL`(기본값 `gemini-2.0-flash`) 환경변수로 설정한다. **`file` 모듈이 아직 `fileId → 실제 파일 내용` 조회를 제공하지 않아, 실제 첨부파일 내용 대신 안내문(placeholder)을 요약 요청으로 보낸다** — file 모듈이 조회 기능을 제공하면 교체해야 한다.

## 발행·소비하는 Event

- `ApprovalLineActivatedEvent`(documentId, documentTitle, approverId, activatedAt) — `DecideApprovalLineService`가 승인 처리로 다음 결재자 라인이 `PENDING`이 될 때 Spring `ApplicationEventPublisher`로 발행한다.
- **아직 이 이벤트를 소비하는 리스너는 없다.** 실제 Web Push 발송(구독 대상 조회 → `web-push` 라이브러리로 전송)은 VAPID 키 발급과 프론트 서비스워커가 준비된 뒤 별도 작업으로 리스너를 추가해 연동한다. 지금은 `PushSubscription`(구독 정보: endpoint/p256dh/auth) 저장 API까지만 준비되어 있다.

## 변경 시 주의 사항

- 성공 응답은 `GlobalApiResponse<T>`로 감싸서 반환한다 (`204 No Content` 응답은 본문 없이 그대로 둔다).
- 도메인 규칙 위반은 `approval.domain.exception.ApprovalErrorCode`(→ `ApprovalException`, `BusinessException` 상속)로 던진다. `users`/`auth` 모듈의 `UserErrorCode`/`UserException` 선례를 따랐다 (`APPROVAL_{status}_{n}` 코드 체계, [API.md](API.md) 오류 코드 표 참고). 프로그래머 계약 위반(파라미터 null 등)은 여전히 `IllegalArgumentException`을 사용한다.
- API 경로에 버전 프리픽스(`/api/v1`)를 붙이지 않는다 — 한때 붙였다가 실배포 전이라는 이유로 되돌린 이력이 있다 ([REVISION.md](REVISION.md) 참고).
- 목록 API(내 결재함, 내가 신청한 결재, 템플릿 목록)는 `page`/`size` 쿼리 파라미터 기반 Slice 페이지네이션을 지원한다 (`API_CONTRACT.md` 규칙 반영, 전체 개수 없이 `hasNext`만 제공).
- 템플릿 생성/수정/삭제 권한(행정직원 제한)과 결재 신청 권한(직원+강사) 인가 로직은 아직 반영되지 않았다. `users.role` 값 체계가 확정되면 Application 또는 Domain Policy에 추가한다 (Controller에 두지 않는다).
- `role_id`(결재선의 역할 기반 지정) 컬럼은 스키마만 있고 해석 로직이 없다. role 테이블이 생기면 구현한다.
- AI 요약은 `POST .../summarize` 호출 시 동기적으로 Gemini를 호출해 처리한다(업로드 시 자동 트리거 없음). **file 모듈이 실제 파일 내용을 조회하는 방법을 제공하기 전까지는 실제 첨부파일 내용이 아니라 placeholder 텍스트로 요약을 생성한다** — 진짜 요약이 아니므로 그대로 서비스에 노출하면 안 된다.

## 세부 문서

- [API.md](API.md) — 엔드포인트별 요청/응답 예시, 검증 규칙, 오류 코드
- [API_FLOW.md](API_FLOW.md) — 계층별 호출 흐름
- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점 변경 요약
