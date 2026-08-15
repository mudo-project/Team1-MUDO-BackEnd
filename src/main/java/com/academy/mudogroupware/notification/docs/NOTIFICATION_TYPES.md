# 알림 타입 코드

`Notification.type`에 저장되는 문자열 코드 목록. 새 도메인이 알림을 추가할 때 이 표에 행을 추가한다.

`message`는 도메인에서 최대 250자(`Notification.MAX_MESSAGE_LENGTH`)로 제한한다. 새 타입을 추가할 때 문구에 가변 길이 값(제목 등)을 끼워 넣는다면, 그 값의 컬럼 최대 길이까지 감안해도 250자를 넘지 않는지 미리 확인한다.

| 코드 | 발생 이벤트 | 발행 모듈 | 문구 |
|---|---|---|---|
| `WORKSPACE_TASK_COMMENT_MENTION` | `TaskCommentMentionedEvent` | workspace | `{이름}님이 댓글에서 회원님을 언급했습니다` |
| `APPROVAL_LINE_ACTIVATED` | `ApprovalLineActivatedEvent` | approval | `결재 문서 [{documentTitle}] 결재 차례가 되었습니다` |
| `APPROVAL_DOCUMENT_DECIDED` | `ApprovalDocumentDecidedEvent` | approval | `status`로 분기: `APPROVED` → `결재 문서가 승인되었습니다` / `REJECTED` → `결재 문서가 반려되었습니다` / `CANCELLED` → `결재 문서가 취소되었습니다` |
| `REVENUE_REPORT_GENERATED` | `RevenueReportGeneratedEvent` | revenuereport | `{yyyy년 M월} 매출 리포트가 생성되었습니다` (수신자: 원장(ACADEMY:OWNER), 원장 계정이 없으면 발행하지 않음) |
