# approval API

기준일: 2026-08-06

## 결재 템플릿

- `POST /api/approval-templates`
- `GET /api/approval-templates`
- `GET /api/approval-templates/{templateId}`
- `PUT /api/approval-templates/{templateId}`
- `DELETE /api/approval-templates/{templateId}`

## 결재 문서

- `POST /api/approvals`
- `GET /api/approvals/submitted`
- `GET /api/approvals/pending`
- `GET /api/approvals/{documentId}`
- `PUT /api/approvals/{documentId}/lines`
- `POST /api/approvals/{documentId}/decisions`
- `POST /api/approvals/{documentId}/resubmit`

### 휴가 기간 선택 입력 (`POST /api/approvals`)

템플릿에 카테고리를 두지 않고, 결재 신청 요청 자체에 `leaveStartDate`/`leaveEndDate`(둘 다 `LocalDate`, 선택)를 추가로 받을 수 있다.

- 둘 다 비어 있으면 일반 결재로 처리한다.
- 둘 중 하나만 있거나 `leaveEndDate`가 `leaveStartDate`보다 빠르면 `400 APPROVAL_400_5`.
- 둘 다 있으면 approval DB에는 저장하지 않고, 신청 시점에 `LeaveRequestSubmittedEvent`로 attendance에 전달한다. 결재가 최종 승인/반려 확정되면 `ApprovalDocumentDecidedEvent`가 발행돼 attendance가 팀 근태 조회에 반영한다(상세: `attendance/docs/LEAVE_INTEGRATION_PROPOSAL.md`).

## 첨부파일 AI 요약

`POST /api/approvals/{documentId}/attachments/{fileId}/summarize`

정책:

- 요청자는 결재 문서의 작성자 또는 결재선 참여자여야 한다.
- file 모듈의 `ApprovalAttachmentContentAdapter`가 `file_metadata`에서 `fileId -> objectKey/contentType`을 조회한다.
- 텍스트 계열 contentType이면 S3에서 UTF-8 원문을 읽어 Gemini에 전달한다.
- 파일 메타데이터가 없거나 미지원 contentType이면 `APPROVAL_409_7`로 실패한다.
- Gemini 호출 실패는 `APPROVAL_502_1`로 실패한다.
- placeholder 텍스트는 Gemini에 보내지 않는다.

성공 응답:

```json
{
  "status": 200,
  "code": "APPROVAL_200_7",
  "message": "첨부파일 요약 생성에 성공했습니다.",
  "data": {
    "fileId": 101,
    "aiSummary": "요약 내용",
    "summaryStatus": "COMPLETED",
    "summarizedAt": "2026-08-06T15:00:00"
  }
}
```

## WebSocket 알림

- 연결 경로: `/ws`
- 구독 경로: `/topic/approvals/users/{userId}`
- 이벤트: 다음 결재자 차례가 되었을 때 `ApprovalLineActivatedEvent` 발행 후 STOMP 메시지 전송

## Web Push 구독 API

- `POST /api/approvals/push-subscriptions`
- `DELETE /api/approvals/push-subscriptions`

이 API는 호환성 때문에 남아 있다. 실제 Web Push 발송은 구현하지 않고, 신규 화면은 WebSocket/STOMP를 사용한다.

## 주요 오류 코드

| code | HTTP | 의미 |
|---|---:|---|
| `APPROVAL_403_1` | 403 | 결재 문서 조회 권한 없음 |
| `APPROVAL_403_5` | 403 | 템플릿 접근 권한 없음 |
| `APPROVAL_403_6` | 403 | 다른 학원 소속 사용자를 결재자로 지정 |
| `APPROVAL_404_1` | 404 | 결재 템플릿 없음 |
| `APPROVAL_404_2` | 404 | 결재 문서 없음 |
| `APPROVAL_404_3` | 404 | 사용자 없음 |
| `APPROVAL_404_4` | 404 | 첨부파일 없음 |
| `APPROVAL_409_7` | 409 | 첨부파일 원문 조회 불가 |
| `APPROVAL_502_1` | 502 | AI 요약 생성 실패 |
