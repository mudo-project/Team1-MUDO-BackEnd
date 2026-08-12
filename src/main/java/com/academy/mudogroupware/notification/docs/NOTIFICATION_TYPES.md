# 알림 타입 코드

`Notification.type`에 저장되는 문자열 코드 목록. 새 도메인이 알림을 추가할 때 이 표에 행을 추가한다.

| 코드 | 발생 이벤트 | 발행 모듈 | 문구 |
|---|---|---|---|
| `WORKSPACE_TASK_COMMENT_MENTION` | `TaskCommentMentionedEvent` | workspace | `{이름}님이 댓글에서 회원님을 언급했습니다` |
| `APPROVAL_LINE_ACTIVATED` | `ApprovalLineActivatedEvent` | approval | `결재 문서 [{documentTitle}] 결재 차례가 되었습니다` |
| `APPROVAL_DOCUMENT_DECIDED` | `ApprovalDocumentDecidedEvent` | approval | `approved` boolean으로 분기: `true` → `결재 문서가 승인되었습니다` / `false` → `결재 문서 처리가 철회되었습니다.`(반려·취소 구분 불가 — approval 이벤트 개선 요청 중) |
