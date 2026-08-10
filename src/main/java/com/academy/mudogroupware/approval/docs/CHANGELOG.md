# approval Changelog

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
