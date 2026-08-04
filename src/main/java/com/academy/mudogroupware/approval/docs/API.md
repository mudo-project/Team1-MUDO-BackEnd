# 📌 전자결재 API

> 기준일: 2026-08-03
> 공통 응답 형식: `status`, `code`, `message`, `data` (204 No Content는 본문 없음)

## 🗂️ 결재 템플릿 API

### 1. 결재 템플릿 생성

`POST /api/approval-templates`
권한: (미정 — 원래는 행정직원 전용, `users.role` 값 체계 확정 전까지 인증만 요구)

#### Request

```json
{
  "name": "연가 신청서",
  "approverIds": [12, 7]
}
```

#### Response · `201 Created`

```json
{
  "status": 201,
  "code": "APPROVAL_201_1",
  "message": "결재 템플릿 생성에 성공했습니다.",
  "data": {
    "templateId": 1
  }
}
```

#### 검증 및 정책

- `name`은 비어 있을 수 없습니다.
- `approverIds`는 최소 1명 이상이어야 하며, 순서대로 1차·2차·… 결재선이 됩니다.
- 생성자(`AuthUser`)의 소속 학원이 템플릿의 학원으로 자동 지정됩니다 (요청으로 받지 않음).

---

### 2. 결재 템플릿 목록 조회

`GET /api/approval-templates?page=0&size=20`
권한: 인증 사용자 전체

- 요청자의 소속 학원(`academyId`) 템플릿만 반환합니다 (다른 학원 템플릿은 조회되지 않음).
- 각 템플릿의 결재선(담당자 이름 포함)을 목록 단계에서부터 함께 반환합니다.
- `page`(0부터 시작, 기본값 `0`)·`size`(기본값 `20`) 쿼리 파라미터로 페이지네이션합니다. 전체 개수(`totalElements`)는 계산하지 않는 Slice 방식이라 `hasNext`로만 다음 페이지 존재 여부를 확인합니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "APPROVAL_200_1",
  "message": "결재 템플릿 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "연가 신청서",
        "createdAt": "2026-08-03T09:00:00",
        "lines": [
          { "stepOrder": 1, "approverId": 12, "approverName": "이민준" },
          { "stepOrder": 2, "approverId": 7, "approverName": "김지수" }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### 3. 결재 템플릿 상세 조회

`GET /api/approval-templates/{templateId}`
권한: 인증 사용자 전체 (학원 스코프 미검증 — 후속 작업)

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "APPROVAL_200_2",
  "message": "결재 템플릿 상세 조회에 성공했습니다.",
  "data": {
    "id": 1,
    "name": "연가 신청서",
    "creatorId": 7,
    "createdAt": "2026-08-03T09:00:00",
    "lines": [
      { "stepOrder": 1, "approverId": 12, "approverName": "이민준" },
      { "stepOrder": 2, "approverId": 7, "approverName": "김지수" }
    ]
  }
}
```

---

### 4. 결재 템플릿 수정

`PATCH /api/approval-templates/{templateId}`
권한: (미정)

#### Request

```json
{
  "name": "연가 신청서(수정)",
  "approverIds": [12, 7, 3]
}
```

#### Response · `204 No Content`

- 이름·결재선은 요청 값으로 전체 교체됩니다.

---

### 5. 결재 템플릿 삭제

`DELETE /api/approval-templates/{templateId}`
권한: (미정)

#### Response · `204 No Content`

---

## 📝 결재 신청 API

### 6. 결재 신청

`POST /api/approvals`
권한: 인증 사용자 전체 (직원+강사)

#### Request

```json
{
  "templateId": 1,
  "title": "2026년 8월 연가 신청",
  "contentType": "TEXT",
  "text": "8월 15일 연가 신청합니다.",
  "fileIds": [101, 102],
  "approverIds": null
}
```

#### Response · `201 Created`

```json
{
  "status": 201,
  "code": "APPROVAL_201_2",
  "message": "결재 신청에 성공했습니다.",
  "data": {
    "documentId": 10
  }
}
```

#### 검증 및 정책

- `contentType`이 `TEXT`면 `text`가 필수입니다.
- `approverIds`를 생략(또는 빈 배열)하면 템플릿의 기본 결재선을 그대로 사용합니다. 지정하면 이 건에 한해 결재선을 덮어씁니다.
- `fileIds`는 여러 개 첨부할 수 있으며, `approval_attachment`에 개별 행으로 저장됩니다.
- 신청자의 소속 학원과 템플릿의 소속 학원이 다르면 `403`을 반환합니다.

---

### 7. 내게 온 결재 목록 조회

`GET /api/approvals/me?page=0&size=20`
권한: 인증 사용자 전체

- `page`(기본값 `0`)·`size`(기본값 `20`) 쿼리 파라미터로 페이지네이션합니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "APPROVAL_200_3",
  "message": "내게 온 결재 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 10,
        "title": "2026년 8월 연가 신청",
        "templateName": "연가 신청서",
        "creatorName": "이민준",
        "status": "IN_PROGRESS",
        "myStepOrder": 2,
        "myLineStatus": "WAITING",
        "currentApproverStepOrder": 1,
        "currentApproverName": "이민준",
        "createdAt": "2026-08-03T09:10:00"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### 8. 내가 신청한 결재 목록 조회

`GET /api/approvals/me/submitted?page=0&size=20`
권한: 인증 사용자 전체

- `page`(기본값 `0`)·`size`(기본값 `20`) 쿼리 파라미터로 페이지네이션합니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "APPROVAL_200_4",
  "message": "내가 신청한 결재 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 10,
        "title": "2026년 8월 연가 신청",
        "templateName": "연가 신청서",
        "creatorName": "이민준",
        "status": "IN_PROGRESS",
        "currentApproverStepOrder": 1,
        "currentApproverName": "이민준",
        "createdAt": "2026-08-03T09:10:00"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

---

### 9. 결재 대기 건수 조회

`GET /api/approvals/me/pending-count`
권한: 인증 사용자 전체

- 사이드바 뱃지 전용 경량 API입니다. 목록 API를 대신 호출하지 마세요.
- 본인이 현재 차례(`PENDING`)인 진행중 결재 건수만 셉니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "APPROVAL_200_6",
  "message": "결재 대기 건수 조회에 성공했습니다.",
  "data": { "count": 3 }
}
```

---

### 10. 결재 상세 조회

`GET /api/approvals/{documentId}`
권한: 신청자 본인 또는 결재선 포함자만

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "APPROVAL_200_5",
  "message": "결재 상세 조회에 성공했습니다.",
  "data": {
    "id": 10,
    "templateId": 1,
    "templateName": "연가 신청서",
    "title": "2026년 8월 연가 신청",
    "contentType": "TEXT",
    "text": "8월 15일 연가 신청합니다.",
    "attachments": [
      { "fileId": 101, "aiSummary": null, "summaryStatus": "PENDING", "summarizedAt": null }
    ],
    "creatorId": 7,
    "creatorName": "이민준",
    "status": "IN_PROGRESS",
    "createdAt": "2026-08-03T09:10:00",
    "lines": [
      { "lineId": 21, "stepOrder": 1, "approverId": 12, "approverName": "이민준", "status": "PENDING", "comment": null, "decidedAt": null },
      { "lineId": 22, "stepOrder": 2, "approverId": 7, "approverName": "김지수", "status": "WAITING", "comment": null, "decidedAt": null }
    ]
  }
}
```

---

### 11. 결재선 수정

`PATCH /api/approvals/{documentId}/lines`
권한: 신청자 본인만

#### Request

```json
{ "approverIds": [12, 3] }
```

#### Response · `204 No Content`

- 이미 승인/반려가 하나라도 처리된 건은 수정할 수 없습니다 (`409`).

---

### 12. 결재 승인/반려

`POST /api/approvals/{documentId}/decide`
권한: 현재 차례의 결재자만

#### Request

```json
{ "decision": "REJECT", "comment": "예산 확인 후 다시 신청해주세요." }
```

#### Response · `204 No Content`

- `decision`은 `APPROVE` 또는 `REJECT`입니다.
- 본인 차례가 아니면 `409`, 이미 완료된 결재는 `409`를 반환합니다.

---

### 13. 결재 재상신

`POST /api/approvals/{documentId}/resubmit`
권한: 신청자 본인만

#### Response · `201 Created`

```json
{
  "status": 201,
  "code": "APPROVAL_201_3",
  "message": "결재 재상신에 성공했습니다.",
  "data": { "documentId": 15 }
}
```

- 반려(`REJECTED`)된 문서만 재상신할 수 있고, 문서 1건당 1회만 가능합니다 (재상신 시각이 기록되어 두 번째 시도는 `409`).
- 제목·내용·결재선·첨부파일을 그대로 복사한 새 문서를 생성합니다.

---

## 🔔 Web Push 구독 API

> ⚠️ 이 API는 구독 정보 저장까지만 합니다. 실제 푸시 발송(VAPID, `web-push` 라이브러리 연동)은 아직 구현되지 않았습니다. 결재 승인으로 다음 결재자 차례가 되면 서버 내부적으로 `ApprovalLineActivatedEvent`가 발행되지만, 이를 소비해 실제로 알림을 보내는 리스너는 없습니다.

### 14. 푸시 구독 등록

`POST /api/approvals/push-subscriptions`
권한: 인증 사용자 전체

#### Request

```json
{
  "endpoint": "https://fcm.googleapis.com/fcm/send/xxxxx",
  "p256dh": "BN4Gv...",
  "auth": "k8Jv..."
}
```

#### Response · `201 Created`

```json
{
  "status": 201,
  "code": "APPROVAL_201_4",
  "message": "푸시 구독 등록에 성공했습니다.",
  "data": { "subscriptionId": 1 }
}
```

- 같은 사용자·`endpoint` 조합으로 다시 등록하면 새로 만들지 않고 `p256dh`/`auth` 키만 갱신합니다 (브라우저가 주기적으로 구독을 재발급하는 경우 대비).

---

### 15. 푸시 구독 해지

`DELETE /api/approvals/push-subscriptions?endpoint={endpoint}`
권한: 인증 사용자 전체 (본인 구독만 삭제)

#### Response · `204 No Content`

---

## 🤖 AI 요약 API

> ⚠️ 실제 첨부파일 내용이 아니라 placeholder 텍스트로 요약을 생성합니다. `file` 모듈이 `fileId → 실제 파일 내용` 조회를 제공하기 전까지는 반환되는 `aiSummary`가 진짜 파일 요약이 아닙니다 (연동 확인용).

### 16. 첨부파일 AI 요약 생성

`POST /api/approvals/{documentId}/attachments/{fileId}/summarize`
권한: 신청자 본인 또는 결재선 포함자만

- 호출할 때마다 동기적으로 Gemini API(`gemini-2.0-flash`, `GEMINI_MODEL`로 변경 가능)를 호출합니다. 업로드 시 자동 트리거는 없습니다.
- Gemini 호출이 실패하면 `summaryStatus`가 `FAILED`로 기록되고 `502`를 반환합니다.

#### Response · `200 OK`

```json
{
  "status": 200,
  "code": "APPROVAL_200_7",
  "message": "첨부파일 요약 생성에 성공했습니다.",
  "data": {
    "fileId": 101,
    "aiSummary": "(Gemini가 생성한 요약 텍스트)",
    "summaryStatus": "COMPLETED",
    "summarizedAt": "2026-08-04T15:00:00"
  }
}
```

---

## ⚠️ 오류 코드 (`ApprovalErrorCode`)

approval 도메인 규칙 위반은 `ApprovalErrorCode`(→ `ApprovalException`)로 던집니다. 응답의 `code` 필드에 아래 값이 그대로 노출됩니다.

| code | HTTP | 메시지 |
| --- | --- | --- |
| `APPROVAL_400_1` | 400 | 결재 내용(텍스트)이 올바르지 않습니다. |
| `APPROVAL_400_2` | 400 | 결재 제목은 비어 있을 수 없습니다. |
| `APPROVAL_400_3` | 400 | 결재선은 최소 1명 이상 지정해야 합니다. |
| `APPROVAL_400_4` | 400 | 템플릿 이름은 비어 있을 수 없습니다. |
| `APPROVAL_403_1` | 403 | 해당 결재를 조회할 권한이 없습니다. |
| `APPROVAL_403_2` | 403 | 다른 학원의 템플릿으로는 결재를 신청할 수 없습니다. |
| `APPROVAL_403_3` | 403 | 본인이 신청한 결재만 재상신할 수 있습니다. |
| `APPROVAL_403_4` | 403 | 본인이 신청한 결재만 결재선을 수정할 수 있습니다. |
| `APPROVAL_404_1` | 404 | 결재 템플릿을 찾을 수 없습니다. |
| `APPROVAL_404_2` | 404 | 결재 문서를 찾을 수 없습니다. |
| `APPROVAL_404_3` | 404 | 사용자를 찾을 수 없습니다. |
| `APPROVAL_404_4` | 404 | 첨부파일을 찾을 수 없습니다. |
| `APPROVAL_409_1` | 409 | 반려된 결재만 재상신할 수 있습니다. |
| `APPROVAL_409_2` | 409 | 이미 재상신된 결재입니다. |
| `APPROVAL_409_3` | 409 | 이미 처리가 완료된 결재입니다. |
| `APPROVAL_409_4` | 409 | 본인 차례의 결재가 아닙니다. |
| `APPROVAL_409_5` | 409 | 이미 결재가 진행된 건은 결재선을 수정할 수 없습니다. |
| `APPROVAL_409_6` | 409 | 이미 처리가 완료된 결재선입니다. |
| `APPROVAL_502_1` | 502 | 첨부파일 요약 생성에 실패했습니다. |

인증 실패(`401`)와 `role_id`/`approver_id` 상호배타 위반처럼 DB `CHECK` 제약으로만 막는 경우는 `CommonErrorCode`(글로벌 공통)를 그대로 사용합니다.
