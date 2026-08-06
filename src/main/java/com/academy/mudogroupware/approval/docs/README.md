# approval 모듈

전자결재 템플릿, 결재 문서, 결재선, 첨부파일 AI 요약, 결재 차례 실시간 알림을 담당한다.

## 책임과 범위

- `ApprovalTemplate`: 반복 사용되는 결재 양식과 기본 결재선.
- `ApprovalDocument`: 사용자가 실제로 상신한 결재 문서.
- `ApprovalDocumentLine`: 결재자별 승인/반려 상태.
- `ApprovalAttachment`: 결재 문서에 연결된 `fileId`와 AI 요약 상태.
- `ApprovalLineActivatedEvent`: 다음 결재자 차례가 되었을 때 발행되는 이벤트.
- `PushSubscription`: 과거 Web Push 준비용 구독 정보. 현재 정책상 실제 발송은 하지 않는다.

## 다른 모듈 연동

- users: `ApproverDirectoryPort`를 users 모듈의 `ApprovalApproverDirectoryAdapter`가 구현한다. approval은 users 테이블을 직접 매핑하지 않는다.
- file: `AttachmentContentPort`를 file 모듈의 `ApprovalAttachmentContentAdapter`가 구현한다. `file_metadata`에서 objectKey/contentType을 찾고, 텍스트 계열 파일만 S3 원문을 읽어 Gemini 요약에 전달한다.
- global security: `AuthUser`로 인증 사용자 정보를 받는다.

## 알림 정책

- 결재 차례 실시간 반영은 WebSocket/STOMP로 고정한다.
- `ApprovalWebSocketNotifier`가 `ApprovalLineActivatedEvent`를 트랜잭션 커밋 후 소비해 `/topic/approvals/users/{userId}`로 보낸다.
- Web Push 발송, VAPID, web-push 라이브러리 연동은 구현하지 않는다.

## AI 요약 정책

- placeholder 텍스트를 Gemini에 보내지 않는다.
- 파일 메타데이터가 없거나 미지원 contentType이면 `APPROVAL_409_7`로 실패하고 `summaryStatus=FAILED`로 저장한다.
- 현재 지원 범위는 UTF-8 텍스트 계열 contentType이다. PDF/docx 텍스트 추출은 별도 file 모듈 확장 범위다.

## 문서

- [API.md](API.md)
- [API_FLOW.md](API_FLOW.md)
- [REVISION.md](REVISION.md)
- [CHANGELOG.md](CHANGELOG.md)
