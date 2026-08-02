# approval 모듈

## 책임과 범위

전자결재 기능을 담당한다. 두 가지 Aggregate로 구성된다.

- **ApprovalTemplate(결재 템플릿)**: 행정 직원이 미리 만들어두는 결재 틀. 이름 + 기본 결재선(순서 있는 결재 담당자 목록)만 가진다.
- **ApprovalDocument(결재 문서)**: 사용자가 템플릿을 선택해 실제로 등록하는 결재 신청 건. 제목, 내용(텍스트/파일), 실제 결재선, 진행 상태(진행중/승인/반려)를 가진다. 순차 승인만 지원하며 병렬결재는 지원하지 않는다(의도적 설계).

## 담당자

(팀 확인 필요 — 이 저장소 초기 세팅 및 approval 도메인 작업자: minseopark0327)

## 소유하는 주요 데이터와 상태

- `ApprovalTemplate`, `ApprovalTemplateLine` — DB 테이블 `approval_templates`, `approval_template_lines` (아직 flyway 마이그레이션 미작성)
- `ApprovalDocument`, `ApprovalDocumentLine` — DB 테이블 `approval_documents`, `approval_document_lines` (아직 flyway 마이그레이션 미작성)
- `ApprovalDocument` 상태: `IN_PROGRESS` → `APPROVED` 또는 `REJECTED`
- `ApprovalDocumentLine` 상태: `WAITING` → `PENDING` → `APPROVED` 또는 `REJECTED`

## 외부에 공개하는 Application API

템플릿 (`/api/v1/approval-templates`):
- `CreateApprovalTemplateUseCase` — 템플릿 생성
- `ApprovalTemplateQueryUseCase` — 템플릿 목록/상세 조회
- `UpdateApprovalTemplateUseCase` — 템플릿 항목(이름·라인) 수정
- `DeleteApprovalTemplateUseCase` — 템플릿 삭제

결재 문서 (`/api/v1/approvals`):
- `CreateApprovalDocumentUseCase` — 결재 신청
- `ApprovalQueryUseCase` — 내게 온 결재 / 내가 신청한 결재 / 상세 조회
- `UpdateApprovalDocumentLinesUseCase` — 결재선 수정 (이미 처리된 앞 단계는 유지, 이후 단계만 교체 가능)
- `DecideApprovalLineUseCase` — 결재 승인/반려 (반려 시 사유 필수)

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **결재자 이름 조회**: `ApproverDirectoryPort`(application/port)로 추상화되어 있으나, User 도메인 모듈이 아직 없어 `infrastructure/persistence`에 `users` 테이블을 직접 읽는 임시 shim(`UserNameEntity`)으로 구현되어 있다. **MODULES.md의 "다른 도메인 JPA Entity 직접 참조 금지" 규칙 위반 상태이며, User 모듈이 생기면 정식 Port 구현으로 교체해야 한다.**
- **파일 업로드**: 결재 문서의 첨부파일은 URL 문자열(`fileUrl`)만 저장한다. 실제 업로드(presigned URL 발급)는 `file` 모듈이 담당하며, approval 모듈은 직접 연동하지 않는다 (프론트가 file 모듈에서 URL을 받아 이 모듈에는 결과 URL만 전달).
- **인증 사용자 정보**: `global.presentation.security.AuthUser`(JWT 인증 principal)를 컨트롤러에서 사용한다.

## 발행·소비하는 Event

- 현재 없음.
- "결재 차례 도래 시 Web Push 알림 발송" 기능 추가 시, `ApprovalDocument.decide()`에서 다음 결재자 라인이 활성화되는 시점에 이벤트(예: `ApprovalLineActivatedEvent`) 발행이 필요할 것으로 예상됨 (미착수).

## 변경 시 주의 사항

- 성공 응답은 `GlobalApiResponse<T>`로 감싸서 반환한다 (`204 No Content` 응답은 본문 없이 그대로 둔다).
- 도메인 규칙 위반은 `global.domain.common.exception`의 `BadRequestException`/`NotFoundException`/`ForbiddenException`/`ConflictException`을 사용한다. approval 전용 `ErrorCode` enum은 아직 만들지 않았다 (팀 논의 후 도입 예정 — `global.domain.auth` 패턴처럼 global 하위에 둘지, 도메인 자체 패키지에 둘지 미정).
- 템플릿 생성/수정/삭제 권한(행정직원 제한)과 결재 신청 권한(직원+강사) 인가 로직은 아직 반영되지 않았다. `users.role` 값 체계가 확정되면 Application 또는 Domain Policy에 추가한다 (Controller에 두지 않는다).

## 세부 문서

- 아직 없음. 필요 시 이 디렉터리에 추가한다.
