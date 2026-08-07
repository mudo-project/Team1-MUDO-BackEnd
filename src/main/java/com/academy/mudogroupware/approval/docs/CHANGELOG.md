# approval Changelog

## 2026-08-06

- users 모듈의 `ApprovalApproverDirectoryAdapter`로 결재자/작성자 이름 조회를 연결했다.
- approval 내부 users 직접 조회 shim을 제거했다.
- file 모듈의 `ApprovalAttachmentContentAdapter`로 첨부파일 원문 조회를 연결했다.
- `file_metadata` 테이블을 추가했다.
- AI 요약은 텍스트 계열 파일 원문이 있을 때만 Gemini를 호출한다.
- 결재 실시간 알림은 WebSocket/STOMP로 고정하고 Web Push 실제 발송은 하지 않는다.
