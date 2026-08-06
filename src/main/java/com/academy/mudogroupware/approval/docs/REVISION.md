# approval Revision

## 2026-08-06 · users/file 공식 Adapter 연결

### 배경

approval 내부에 users 테이블 직접 조회 shim이 있었고, 첨부파일 AI 요약은 실제 파일 원문을 읽을 공식 경로가 없었다. 모듈 경계를 맞추기 위해 approval이 필요한 Port를 정의하고, 데이터 소유 모듈이 Adapter를 구현하는 방식으로 정리했다.

### 변경

- users 모듈에 `ApprovalApproverDirectoryAdapter`를 추가해 `ApproverDirectoryPort`를 구현했다.
- approval 내부 `UserNameEntity`, `UserNameJpaRepository`, `ApproverDirectoryPortAdapter` shim을 제거했다.
- file 모듈에 `file_metadata` 조회 Entity/Repository와 `ApprovalAttachmentContentAdapter`를 추가했다.
- AI 요약은 텍스트 계열 파일 원문을 실제로 읽을 수 있을 때만 Gemini를 호출한다.
- 미지원 파일 또는 메타데이터 없음은 `APPROVAL_409_7`로 실패한다.

### 검증

- `ApprovalApproverDirectoryAdapterTest`
- `ApprovalAttachmentContentAdapterTest`
- `SummarizeApprovalAttachmentServiceTest`

## 2026-08-06 · WebSocket 알림 고정 및 Web Push 미사용

- 결재 실시간 알림은 WebSocket/STOMP로 고정한다.
- Web Push 발송, VAPID, web-push 라이브러리 연동은 하지 않는다.
- PushSubscription API는 호환성 때문에 남겨둔다.

## 2026-08-04 · 첨부파일 AI 요약 초안

- 최초 구현의 placeholder 요약 방식은 폐기했다.
- 현재는 실제 원문이 없으면 Gemini를 호출하지 않는다.
