# approval Changelog

## 2026-08-16

- 결재 문서 생성/결재선 수정 시 신청자 본인이 결재선에 포함되면 `APPROVAL_400_6`으로 차단한다.
- 결재 문서와 결재 템플릿 모두 같은 결재자가 결재선에 중복 지정되면 `APPROVAL_400_7`로 차단한다.
- 템플릿 작성자는 실제 기안자가 아닐 수 있으므로, 템플릿 생성/수정에서는 자기 자신 포함 여부가 아니라 중복 결재자만 검증한다.

## 2026-08-14

- `GeminiFieldExtractionAdapter`/`GeminiSummarizerAdapter`가 Gemini 호출 직전에 이번 달 AI 토큰 사용량이 플랜 한도(무료 10만/유료 100만 토큰)를 넘으면 429로 차단하도록 변경했다. `GeminiTokenUsageTracker`는 응답 이후 기록용이라 사전 차단 지점이 될 수 없어, 별도로 `resourceusage`의 월별 합계를 조회한다.
- 전자결재 문서 보존 정책을 코드와 DB에 반영했다.
- `approval_document`에 `retention_policy`, `retention_until`, `legal_hold`, `archived_at` 컬럼을 추가했다.
- 일반 결재는 `GENERAL_BUSINESS`로 생성일 기준 3년, 법인카드/비용정산 결재는 `TAX_EVIDENCE`로 생성일 기준 5년 보존기한을 자동 계산한다.
- 계약/중요 지출/핵심 의사결정용 `IMPORTANT_BUSINESS` 정책은 10년 보존 기준으로 enum에 준비했으며, 현재 자동 분류 대상은 아니다.
- 이번 단계에서는 실제 하드 삭제나 아카이브 배치를 추가하지 않고, 향후 운영 정책 판단에 필요한 기준 컬럼만 준비했다.

## 2026-08-11 (2)

- file 모듈의 `file_metadata` academyId 제거에 맞춰, `GetApprovalAttachmentDownloadUrlCommand`/`GetApprovalAttachmentDownloadUrlService`/`ApprovalController`에서 academyId 전달을 뺐다. 방어 로직 자체(신청자/결재선 참여자 검증 + fileId 문서 소속 검증)는 academyId와 무관하게 그대로 유지된다.

## 2026-08-11

- 첨부파일 AI 요약이 PDF/이미지/docx까지 지원하도록 확장됐다. `AttachmentContentPort`가 텍스트 대신 `AttachmentContent`(TEXT/BINARY)를 반환하도록 바뀌었다.
- PDF·이미지(jpeg/png/webp/heic/heif)는 Gemini 멀티모달 입력(inline base64)으로 직접 전달한다. 원본 15MB를 초과하면 `APPROVAL_409_7`로 실패한다.
- docx는 Apache POI(`XWPFDocument`)로 텍스트를 추출한 뒤 기존 텍스트 요약 경로로 전달한다.
- hwp 등 나머지 형식은 여전히 미지원이며 `APPROVAL_409_7`로 실패한다.
- Gemini 요청에 요약 지시문(SUMMARY_INSTRUCTION)이 빠져 있던 문제를 함께 고쳤다 — 이전에는 원문만 그대로 전달했다.

## 2026-08-10

- `GET /api/approvals/{documentId}/attachments/{fileId}/download-url` 결재 첨부파일 전용 다운로드 URL 조회 API를 추가했다.
- `file` 모듈의 범용 `GET /api/files/{fileId}/download-url`은 academyId만 검증해 같은 학원 소속이면 결재선과 무관한 사용자도 기밀 첨부파일 URL을 받을 수 있었다. 이 API는 신청자/결재선 참여자 여부와 fileId의 문서 소속 여부를 먼저 검증한 뒤에만 `file` 모듈에 URL 발급을 위임한다.
- `GetApprovalAttachmentDownloadUrlUseCase`/`GetApprovalAttachmentDownloadUrlService`를 추가했다. 검증 로직은 기존 `SummarizeApprovalAttachmentService`와 동일한 `isApprover`/`getCreatorId`/`findAttachmentByFileId` 조합을 재사용한다.

## 2026-08-08

- 결재 신청/재상신은 `APPROVAL:SUBMIT` 권한으로 제한했다.
- 결재 템플릿 생성/수정/삭제는 `APPROVAL:TEMPLATE_MANAGE` 권한으로 제한했다.
- 결재 템플릿 목록/상세 조회는 상신 화면에서도 필요하므로 `APPROVAL:SUBMIT` 또는 `APPROVAL:TEMPLATE_MANAGE` 중 하나가 있으면 허용한다.

## 2026-08-07

- `GET /api/approvals` 전체 결재 목록 조회 API를 추가했다. `APPROVAL:READ_ALL` 권한이 필요하며 소속 학원 문서만 조회한다.
- `GET /api/approvals/me/history` 내 결재 이력 조회 API를 추가했다.
- `DELETE /api/approvals/me/history/{documentId}` 내 결재 이력 숨김 API를 추가했다. 문서 원본은 삭제하지 않는다.
- `POST /api/approvals/{documentId}/cancel` 결재 신청 취소 API를 추가했다.
- `ApprovalStatus.CANCELLED` 상태를 추가했다.
- `approval_history_hidden` 테이블과 `APPROVAL:READ_ALL` 권한 카탈로그 시드 마이그레이션을 추가했다.
- 전체조회 권한자는 같은 학원 문서에 한해 상세조회할 수 있게 했다.

## 2026-08-06

- users 모듈의 `ApprovalApproverDirectoryAdapter`로 결재자/작성자 이름 조회를 연결했다.
- approval 내부 users 직접 조회 shim을 제거했다.
- file 모듈의 `ApprovalAttachmentContentAdapter`로 첨부파일 원문 조회를 연결했다.
- `file_metadata` 테이블을 추가했다.
- AI 요약은 텍스트 계열 파일 원문이 있을 때만 Gemini를 호출한다.
- 결재 실시간 알림은 WebSocket/STOMP로 고정하고 Web Push 실제 발송은 구현하지 않는다.
