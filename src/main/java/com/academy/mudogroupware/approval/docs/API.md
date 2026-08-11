# approval API

기준일: 2026-08-09

> 이 문서는 Spring Boot Controller, Response DTO, ErrorCode 구현 기준으로 작성한다.  
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요하다.

## 권한 정책

| 권한 코드 | 적용 기능 |
| --- | --- |
| `APPROVAL:SUBMIT` | 결재 신청, 결재 재상신 |
| `APPROVAL:TEMPLATE_MANAGE` | 결재 템플릿 생성/수정/삭제 |
| `APPROVAL:SUBMIT` 또는 `APPROVAL:TEMPLATE_MANAGE` | 결재 템플릿 목록/상세 조회 |
| `APPROVAL:READ_ALL` | 소속 학원 전체 결재 문서 목록 조회, 전체 조회 권한자 상세 조회 |
| (권한 코드 없음, 로그인만 필요) | 결재 신청 외 나머지 결재 문서 기능(내 결재함, 상세, 결재선 수정, 승인/반려, 취소, 재상신, 이력, 첨부파일 요약) — 서비스 계층에서 신청자/결재선 참여자 여부로 검증 |

---

## **1. 결재 신청**

`POST /api/approvals`

권한: `APPROVAL:SUBMIT`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body

```json
{
  "templateId": 1,
  "title": "휴가 신청",
  "contentType": "TEXT",
  "text": "8/10~8/12 개인 사정으로 휴가를 신청합니다.",
  "fileIds": [],
  "approverIds": [10, 20],
  "leaveStartDate": "2026-08-10",
  "leaveEndDate": "2026-08-12"
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `templateId` | `Long` | `true` | 사용할 결재 템플릿 ID |
| `title` | `String` | `true` | 결재 제목 |
| `contentType` | `String` | `true` | `TEXT` 또는 `FILE` |
| `text` | `String` | `contentType=TEXT`일 때 `true` | 결재 본문. `TEXT` 타입이면 필수 |
| `fileIds` | `List<Long>` | `false` | 첨부파일 ID 목록. `POST /api/files/presigned-url` → S3 업로드 → `POST /api/files`로 발급받은 `fileId`를 넣는다. 자세한 흐름은 [file 모듈 API.md](../../file/docs/API.md) 참고 |
| `approverIds` | `List<Long>` | `false` | 템플릿 기본 결재선 대신 사용할 결재선. 비우면 템플릿의 결재선을 그대로 사용 |
| `leaveStartDate` | `String(date)` | `false` | 휴가 연동 시작일. 지정 시 `leaveEndDate`도 함께 지정해야 함(근태 자동 반영용) |
| `leaveEndDate` | `String(date)` | `false` | 휴가 연동 종료일 |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `201 Created` | 결재 신청 성공 |

Response Body

```json
{"status":201,"code":"APPROVAL_201_2","message":"결재 신청에 성공했습니다.","data":{"documentId":1}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.documentId` | 생성된 결재 문서 ID입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `APPROVAL_400_1` | 결재 내용 형식이 올바르지 않습니다. | `contentType=TEXT`인데 `text`가 비어 있는 경우 |
| `400 Bad Request` | `APPROVAL_400_2` | 결재 제목은 비어 있을 수 없습니다. | `title`이 비어 있는 경우 |
| `400 Bad Request` | `APPROVAL_400_3` | 결재선은 최소 1명 이상 지정해야 합니다. | 최종 결재선이 비어 있는 경우 |
| `400 Bad Request` | `APPROVAL_400_5` | 휴가 기간은 시작일과 종료일을 함께 입력해야 하며, 종료일은 시작일보다 빠를 수 없습니다. | `leaveStartDate`/`leaveEndDate` 중 하나만 있거나 순서가 잘못된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_2` | 다른 학원의 템플릿으로는 결재를 신청할 수 없습니다. | `templateId`가 다른 학원 소속인 경우 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 사용자가 포함된 경우 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | `templateId`에 해당하는 템플릿이 없는 경우 |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자가 포함된 경우 |

---

## **2. 전체 결재 목록 조회**

`GET /api/approvals`

권한: `APPROVAL:READ_ALL`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 소속 학원의 전체 결재 목록 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_8","message":"전체 결재 목록 조회에 성공했습니다.","data":{"content":[{"id":1,"title":"휴가 신청","templateName":"휴가 신청서","creatorName":"김직원","status":"IN_PROGRESS","currentApproverStepOrder":1,"currentApproverName":"박원장","createdAt":"2026-08-04T10:00:00"}],"page":0,"size":20,"hasNext":false}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.content[].id` | 결재 문서 ID입니다. |
| `data.content[].title` | 결재 제목입니다. |
| `data.content[].templateName` | 결재 템플릿 이름입니다. |
| `data.content[].creatorName` | 결재 신청자 이름입니다. |
| `data.content[].status` | 결재 문서 상태입니다. `IN_PROGRESS`, `APPROVED`, `REJECTED`, `CANCELLED` 중 하나입니다. |
| `data.content[].currentApproverStepOrder` | 현재 결재 차례의 순번입니다. 완료/반려/취소된 문서는 `null`입니다. |
| `data.content[].currentApproverName` | 현재 결재 차례의 결재자 이름입니다. 완료/반려/취소된 문서는 `null`입니다. |
| `data.content[].createdAt` | 결재 신청 시각(KST)입니다. |
| `data.page` | 현재 페이지 번호(0-based)입니다. |
| `data.size` | 페이지 크기입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `COMMON_403_1` | 접근 권한이 없습니다. | `APPROVAL:READ_ALL` 권한이 없는 경우 |

---

## **3. 내게 온 결재 목록 조회**

`GET /api/approvals/me`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 내가 결재선에 포함된 결재 목록 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_3","message":"내게 온 결재 목록 조회에 성공했습니다.","data":{"content":[{"id":1,"title":"휴가 신청","templateName":"휴가 신청서","creatorName":"김직원","status":"IN_PROGRESS","myStepOrder":1,"myLineStatus":"PENDING","currentApproverStepOrder":1,"currentApproverName":"박원장","createdAt":"2026-08-04T10:00:00"}],"page":0,"size":20,"hasNext":false}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.content[].id` | 결재 문서 ID입니다. |
| `data.content[].title` | 결재 제목입니다. |
| `data.content[].templateName` | 결재 템플릿 이름입니다. |
| `data.content[].creatorName` | 결재 신청자 이름입니다. |
| `data.content[].status` | 결재 문서 전체 상태입니다. |
| `data.content[].myStepOrder` | 내가 담당한 결재 순번입니다. |
| `data.content[].myLineStatus` | 내 결재선 처리 상태입니다. `WAITING`, `PENDING`, `APPROVED`, `REJECTED` 중 하나입니다. |
| `data.content[].currentApproverStepOrder` | 현재 결재 차례의 순번입니다. 완료/반려/취소된 문서는 `null`입니다. |
| `data.content[].currentApproverName` | 현재 결재 차례의 결재자 이름입니다. 완료/반려/취소된 문서는 `null`입니다. |
| `data.content[].createdAt` | 결재 신청 시각(KST)입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **4. 내가 신청한 결재 목록 조회**

`GET /api/approvals/me/submitted`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 내가 신청자인 결재 목록 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_4","message":"내가 신청한 결재 목록 조회에 성공했습니다.","data":{"content":[{"id":1,"title":"휴가 신청","templateName":"휴가 신청서","creatorName":"김직원","status":"IN_PROGRESS","currentApproverStepOrder":1,"currentApproverName":"박원장","createdAt":"2026-08-04T10:00:00"}],"page":0,"size":20,"hasNext":false}}
```

### **Response Field**

`전체 결재 목록 조회`(2번)와 동일한 필드 구성입니다.

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **5. 내 결재 이력 조회**

`GET /api/approvals/me/history`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 내가 승인/반려 처리한 결재 이력 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_9","message":"내 결재 이력 조회에 성공했습니다.","data":{"content":[{"id":1,"title":"휴가 신청","templateName":"휴가 신청서","creatorName":"김직원","status":"APPROVED","myStepOrder":1,"myLineStatus":"APPROVED","currentApproverStepOrder":null,"currentApproverName":null,"createdAt":"2026-08-04T10:00:00"}],"page":0,"size":20,"hasNext":false}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.content[].id` | 결재 문서 ID입니다. |
| `data.content[].title` | 결재 제목입니다. |
| `data.content[].templateName` | 결재 템플릿 이름입니다. |
| `data.content[].creatorName` | 결재 신청자 이름입니다. |
| `data.content[].status` | 결재 문서 전체 상태입니다. |
| `data.content[].myStepOrder` | 내가 담당한 결재 순번입니다. |
| `data.content[].myLineStatus` | 내 결재선 처리 상태입니다. `APPROVED` 또는 `REJECTED`입니다. |
| `data.content[].currentApproverStepOrder` | 현재 결재 차례의 순번입니다. 완료/반려/취소된 문서는 `null`입니다. |
| `data.content[].currentApproverName` | 현재 결재 차례의 결재자 이름입니다. 완료/반려/취소된 문서는 `null`입니다. |
| `data.content[].createdAt` | 결재 신청 시각(KST)입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **6. 내 결재 이력 삭제**

`DELETE /api/approvals/me/history/{documentId}`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 내 결재 이력에서 숨길 결재 문서 ID입니다. |

Request Body

없음

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 내 결재 이력 삭제 성공 |

Response Body

없음

### **Response Field**

없음

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_1` | 해당 결재를 조회할 권한이 없습니다. | 해당 결재 문서의 결재선 참여자가 아닌 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |
| `409 Conflict` | `APPROVAL_409_9` | 처리가 완료된 본인 결재 이력만 삭제할 수 있습니다. | 내가 아직 승인/반려 처리하지 않은 결재를 삭제하려는 경우 |

---

## **7. 결재 대기 건수 조회**

`GET /api/approvals/me/pending-count`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 내가 현재 차례인 진행 중 결재 건수 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_6","message":"결재 대기 건수 조회에 성공했습니다.","data":{"count":3}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.count` | 내가 현재 결재 차례인 진행 중 결재 건수입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **8. 결재 상세 조회**

`GET /api/approvals/{documentId}`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 조회할 결재 문서 ID입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 결재 상세 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_5","message":"결재 상세 조회에 성공했습니다.","data":{"id":1,"templateId":1,"templateName":"휴가 신청서","title":"휴가 신청","contentType":"TEXT","text":"8/10~8/12 개인 사정으로 휴가를 신청합니다.","attachments":[],"creatorId":5,"creatorName":"김직원","status":"IN_PROGRESS","createdAt":"2026-08-04T10:00:00","lines":[{"lineId":1,"stepOrder":1,"approverId":10,"approverName":"박원장","status":"PENDING","comment":null,"decidedAt":null}]}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.id` | 결재 문서 ID입니다. |
| `data.templateId` | 사용된 템플릿 ID입니다. |
| `data.templateName` | 템플릿 이름입니다. |
| `data.title` | 결재 제목입니다. |
| `data.contentType` | `TEXT` 또는 `FILE`입니다. |
| `data.text` | 결재 본문입니다. `FILE` 타입이면 `null`일 수 있습니다. |
| `data.attachments[].fileId` | 첨부파일 ID입니다. |
| `data.attachments[].aiSummary` | AI 요약 텍스트입니다. 요약 전이면 `null`입니다. |
| `data.attachments[].summaryStatus` | 요약 상태입니다. `PENDING`, `COMPLETED`, `FAILED` 중 하나입니다. |
| `data.attachments[].summarizedAt` | 요약 생성 시각입니다. |
| `data.creatorId` | 신청자 ID입니다. |
| `data.creatorName` | 신청자 이름입니다. |
| `data.status` | 결재 문서 전체 상태입니다. |
| `data.createdAt` | 신청 시각(KST)입니다. |
| `data.lines[].lineId` | 결재선 ID입니다. |
| `data.lines[].stepOrder` | 결재 순번입니다. |
| `data.lines[].approverId` | 결재자 ID입니다. |
| `data.lines[].approverName` | 결재자 이름입니다. |
| `data.lines[].status` | 결재선 상태입니다. `WAITING`, `PENDING`, `APPROVED`, `REJECTED` 중 하나입니다. |
| `data.lines[].comment` | 결재 코멘트입니다. 처리 전이면 `null`입니다. |
| `data.lines[].decidedAt` | 결재 처리 시각입니다. 처리 전이면 `null`입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_1` | 해당 결재를 조회할 권한이 없습니다. | 신청자, 결재선 참여자, `APPROVAL:READ_ALL` 권한자 모두 아닌 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |

---

## **9. 결재선 수정**

`PATCH /api/approvals/{documentId}/lines`

신청자 본인만 가능하다. 이미 승인/반려가 하나라도 처리된 건은 수정할 수 없다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 결재선을 수정할 결재 문서 ID입니다. |

Request Body

```json
{
  "approverIds": [10, 30]
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `approverIds` | `List<Long>` | `true` | 새 결재선(순서대로) |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 결재선 수정 성공 |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `APPROVAL_400_3` | 결재선은 최소 1명 이상 지정해야 합니다. | `approverIds`가 비어 있는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_4` | 본인이 신청한 결재만 결재선을 수정할 수 있습니다. | 신청자 본인이 아닌 경우 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 사용자가 포함된 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자가 포함된 경우 |
| `409 Conflict` | `APPROVAL_409_5` | 이미 결재가 진행된 건이라 결재선을 수정할 수 없습니다. | 결재선 중 하나라도 이미 승인/반려된 경우 |

---

## **10. 결재 승인/반려**

`POST /api/approvals/{documentId}/decide`

현재 차례의 결재자만 승인 또는 반려할 수 있다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 승인/반려할 결재 문서 ID입니다. |

Request Body

```json
{
  "decision": "APPROVE",
  "comment": "확인했습니다."
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `decision` | `String` | `true` | `APPROVE` 또는 `REJECT` |
| `comment` | `String` | `false` | 결재 코멘트 |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 승인/반려 처리 성공 |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |
| `409 Conflict` | `APPROVAL_409_3` | 이미 처리가 완료된 결재입니다. | 문서가 `IN_PROGRESS` 상태가 아닌 경우 |
| `409 Conflict` | `APPROVAL_409_4` | 본인 차례의 결재가 아닙니다. | 현재 결재 차례의 결재자가 아닌 경우 |
| `409 Conflict` | `APPROVAL_409_6` | 이미 처리가 완료된 결재선입니다. | 해당 결재선이 이미 승인/반려 처리된 경우 |

---

## **11. 결재 신청 취소**

`POST /api/approvals/{documentId}/cancel`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 취소할 결재 문서 ID입니다. |

Request Body

없음

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 결재 신청 취소 성공 |

Response Body

없음

### **Response Field**

없음

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_7` | 본인이 신청한 결재만 취소할 수 있습니다. | 신청자가 아닌 사용자가 취소를 요청한 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |
| `409 Conflict` | `APPROVAL_409_8` | 이미 처리가 시작된 결재는 취소할 수 없습니다. | 이미 승인/반려가 처리된 결재이거나 진행 중 상태가 아닌 경우 |

---

## **12. 결재 재상신**

`POST /api/approvals/{documentId}/resubmit`

권한: `APPROVAL:SUBMIT`. 반려된 문서만 신청자 본인이 재상신할 수 있다. 문서 1건당 1회로 제한한다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 재상신할(반려된) 결재 문서 ID입니다. |

Request Body

없음

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `201 Created` | 재상신 성공. 기존 제목·내용·결재 라인이 그대로 채워진 새 문서가 생성된다. |

Response Body

```json
{"status":201,"code":"APPROVAL_201_3","message":"결재 재상신에 성공했습니다.","data":{"documentId":2}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.documentId` | 새로 생성된 결재 문서 ID입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_3` | 본인이 신청한 결재만 재상신할 수 있습니다. | 신청자 본인이 아닌 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |
| `409 Conflict` | `APPROVAL_409_1` | 반려된 결재만 재상신할 수 있습니다. | 문서 상태가 `REJECTED`가 아닌 경우 |
| `409 Conflict` | `APPROVAL_409_2` | 이미 재상신된 결재입니다. | 해당 문서를 이미 한 번 재상신한 경우 |

---

## **13. 첨부파일 AI 요약 생성**

`POST /api/approvals/{documentId}/attachments/{fileId}/summarize`

Gemini API를 호출해 첨부파일 요약을 생성한다. 신청자 또는 결재선 참여자만 가능하다.

지원하는 첨부파일 형식은 다음과 같다.

| 구분 | contentType | 처리 방식 |
| --- | --- | --- |
| 텍스트 | `text/plain` 등 UTF-8 텍스트 계열 | 원문 텍스트를 그대로 프롬프트에 포함 |
| PDF | `application/pdf` | Gemini 멀티모달 입력(inline base64)으로 전달 |
| 이미지 | `image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif` | Gemini 멀티모달 입력(inline base64)으로 전달 |
| Word 문서 | `application/vnd.openxmlformats-officedocument.wordprocessingml.document` (docx) | Apache POI로 텍스트를 추출한 뒤 프롬프트에 포함 |

PDF/이미지는 원본 용량이 15MB를 초과하면 요약할 수 없다(`APPROVAL_409_7`). 위 표에 없는 형식(예: hwp, ppt, xlsx 등)도 지원하지 않는다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 결재 문서 ID입니다. |
| `fileId` | 요약할 첨부파일 ID입니다. |

Request Body

없음

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 첨부파일 요약 생성 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_7","message":"첨부파일 요약 생성에 성공했습니다.","data":{"fileId":3,"aiSummary":"휴가 신청 사유 및 기간을 정리한 요약문입니다.","summaryStatus":"COMPLETED","summarizedAt":"2026-08-04T10:05:00"}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.fileId` | 첨부파일 ID입니다. |
| `data.aiSummary` | Gemini가 생성한 요약 텍스트입니다. |
| `data.summaryStatus` | `PENDING`, `COMPLETED`, `FAILED` 중 하나입니다. |
| `data.summarizedAt` | 요약 생성 시각입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_1` | 해당 결재를 조회할 권한이 없습니다. | 신청자/결재선 참여자가 아닌 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |
| `404 Not Found` | `APPROVAL_404_4` | 첨부파일을 찾을 수 없습니다. | `fileId`가 해당 문서의 첨부파일이 아닌 경우 |
| `409 Conflict` | `APPROVAL_409_7` | 첨부파일 원문 조회 기능이 없어 요약할 수 없습니다. | 지원하지 않는 형식(hwp 등)이거나, PDF/이미지 용량이 15MB를 초과하거나, docx 텍스트 추출에 실패한 경우 |
| `502 Bad Gateway` | `APPROVAL_502_1` | 첨부파일 요약 생성에 실패했습니다. | Gemini API 호출이 실패한 경우 |

---

## **14. 첨부파일 다운로드 URL 조회**

`GET /api/approvals/{documentId}/attachments/{fileId}/download-url`

결재 첨부파일은 기밀 자료이므로 신청자 또는 결재선 참여자만 다운로드 URL을 받을 수 있다. `file` 모듈의 `GET /api/files/{fileId}/download-url`([file 모듈 API.md](../../file/docs/API.md))은 인증된 사용자면 fileId만으로 누구나 호출할 수 있으므로, 이 엔드포인트가 "요청자가 이 결재 문서의 신청자/결재선 참여자인지"와 "이 fileId가 실제로 이 documentId 소속인지"를 먼저 검증한다. 두 조건을 모두 통과해야 presigned URL을 발급한다.

### **14.1 요청**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `documentId` | 결재 문서 ID입니다. |
| `fileId` | 다운로드 URL을 받을 첨부파일 ID입니다. |

Request Body

없음

### **14.2 응답**

#### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 다운로드 URL 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_10","message":"첨부파일 다운로드 URL 조회에 성공했습니다.","data":{"downloadUrl":"https://s3.ap-northeast-2.amazonaws.com/..."}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.downloadUrl` | 짧은 시간 동안만 유효한 S3 presigned 다운로드 URL입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_1` | 해당 결재를 조회할 권한이 없습니다. | 신청자/결재선 참여자가 아닌 경우 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | `documentId`에 해당하는 결재 문서가 없는 경우 |
| `404 Not Found` | `APPROVAL_404_4` | 첨부파일을 찾을 수 없습니다. | `fileId`가 해당 문서의 첨부파일이 아닌 경우 |
| `404 Not Found` | `FILE_404_1` | 파일을 찾을 수 없습니다. | 문서 소속 확인은 통과했지만 file 모듈에 해당 fileId 메타데이터가 없는 경우(정상 상황에서는 발생하지 않음) |

### **알려진 제약**

- 이 엔드포인트는 결재 첨부파일 전용이다. notice 등 다른 도메인의 첨부파일은 여전히 `file` 모듈의 범용 엔드포인트(`GET /api/files/{fileId}/download-url`)를 그대로 쓴다.
- 프론트는 결재 상세 화면의 첨부파일 다운로드/미리보기에서 반드시 이 엔드포인트를 호출해야 한다. 범용 `file` 모듈 엔드포인트를 직접 호출하면 인증된 사용자면 누구나 다운로드 URL을 받을 수 있다(의도한 동작이 아님).

---

## **15. 결재 템플릿 생성**

`POST /api/approval-templates`

권한: `APPROVAL:TEMPLATE_MANAGE`. 이름과 기본 결재선(순서 있는 담당자 목록)을 지정해 템플릿을 만든다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body

```json
{
  "name": "휴가 신청서",
  "approverIds": [10, 20]
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `name` | `String` | `true` | 템플릿 이름 |
| `approverIds` | `List<Long>` | `true` | 순서 있는 기본 결재선 |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `201 Created` | 결재 템플릿 생성 성공 |

Response Body

```json
{"status":201,"code":"APPROVAL_201_1","message":"결재 템플릿 생성에 성공했습니다.","data":{"templateId":1}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.templateId` | 생성된 템플릿 ID입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `APPROVAL_400_4` | 템플릿 이름은 비어 있을 수 없습니다. | `name`이 비어 있는 경우 |
| `400 Bad Request` | `APPROVAL_400_3` | 결재선은 최소 1명 이상 지정해야 합니다. | `approverIds`가 비어 있는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 사용자가 포함된 경우 |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자가 포함된 경우 |

---

## **16. 결재 템플릿 목록 조회**

`GET /api/approval-templates`

권한: `APPROVAL:SUBMIT` 또는 `APPROVAL:TEMPLATE_MANAGE`. 요청자 소속 학원의 템플릿만 조회한다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 결재 템플릿 목록 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_1","message":"결재 템플릿 목록 조회에 성공했습니다.","data":{"content":[{"id":1,"name":"휴가 신청서","createdAt":"2026-08-01T09:00:00","lines":[{"stepOrder":1,"approverId":10,"approverName":"박원장"}]}],"page":0,"size":20,"hasNext":false}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.content[].id` | 템플릿 ID입니다. |
| `data.content[].name` | 템플릿 이름입니다. |
| `data.content[].createdAt` | 템플릿 생성 시각입니다. |
| `data.content[].lines[].stepOrder` | 결재 순번입니다. |
| `data.content[].lines[].approverId` | 결재자 ID입니다. |
| `data.content[].lines[].approverName` | 결재자 이름입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **17. 결재 템플릿 상세 조회**

`GET /api/approval-templates/{templateId}`

권한: `APPROVAL:SUBMIT` 또는 `APPROVAL:TEMPLATE_MANAGE`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `templateId` | 조회할 템플릿 ID입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `200 OK` | 결재 템플릿 상세 조회 성공 |

Response Body

```json
{"status":200,"code":"APPROVAL_200_2","message":"결재 템플릿 상세 조회에 성공했습니다.","data":{"id":1,"name":"휴가 신청서","creatorId":5,"createdAt":"2026-08-01T09:00:00","lines":[{"stepOrder":1,"approverId":10,"approverName":"박원장"}]}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.id` | 템플릿 ID입니다. |
| `data.name` | 템플릿 이름입니다. |
| `data.creatorId` | 템플릿을 만든 사용자 ID입니다. |
| `data.createdAt` | 템플릿 생성 시각입니다. |
| `data.lines[].stepOrder` | 결재 순번입니다. |
| `data.lines[].approverId` | 결재자 ID입니다. |
| `data.lines[].approverName` | 결재자 이름입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_5` | 해당 템플릿에 접근할 권한이 없습니다. | 다른 학원의 템플릿인 경우 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | `templateId`에 해당하는 템플릿이 없는 경우 |

---

## **18. 결재 템플릿 수정**

`PATCH /api/approval-templates/{templateId}`

권한: `APPROVAL:TEMPLATE_MANAGE`. 이름·결재선을 요청 값으로 전체 교체한다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `templateId` | 수정할 템플릿 ID입니다. |

Request Body

```json
{
  "name": "휴가 신청서(수정)",
  "approverIds": [10, 30]
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `name` | `String` | `true` | 템플릿 이름 |
| `approverIds` | `List<Long>` | `true` | 순서 있는 결재선(전체 교체) |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 결재 템플릿 수정 성공 |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `APPROVAL_400_4` | 템플릿 이름은 비어 있을 수 없습니다. | `name`이 비어 있는 경우 |
| `400 Bad Request` | `APPROVAL_400_3` | 결재선은 최소 1명 이상 지정해야 합니다. | `approverIds`가 비어 있는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_5` | 해당 템플릿에 접근할 권한이 없습니다. | 다른 학원의 템플릿인 경우 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 사용자가 포함된 경우 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | `templateId`에 해당하는 템플릿이 없는 경우 |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자가 포함된 경우 |

---

## **19. 결재 템플릿 삭제**

`DELETE /api/approval-templates/{templateId}`

권한: `APPROVAL:TEMPLATE_MANAGE`

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| **name** | **description** |
| --- | --- |
| `templateId` | 삭제할 템플릿 ID입니다. |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 결재 템플릿 삭제 성공 |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `APPROVAL_403_5` | 해당 템플릿에 접근할 권한이 없습니다. | 다른 학원의 템플릿인 경우 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | `templateId`에 해당하는 템플릿이 없는 경우 |

---

## **20. (사용 중지 예정) 웹 푸시 구독 등록**

`POST /api/approvals/push-subscriptions`

> ⚠️ 결재 실시간 알림은 WebSocket/STOMP로 고정됐다. 이 API는 호환성 때문에 저장 로직만 남겨뒀고, 실제 Web Push 발송 기능은 구현하지 않는다. 신규 연동에는 사용하지 않는다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body

```json
{
  "endpoint": "https://fcm.googleapis.com/...",
  "p256dh": "base64-public-key",
  "auth": "base64-auth-secret"
}
```

| **name** | **type** | **required** | **설명** |
| --- | --- | --- | --- |
| `endpoint` | `String` | `true` | 브라우저가 발급한 푸시 구독 endpoint |
| `p256dh` | `String` | `true` | 구독 공개키 |
| `auth` | `String` | `true` | 구독 인증 시크릿 |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `201 Created` | 푸시 구독 등록(저장) 성공 |

Response Body

```json
{"status":201,"code":"APPROVAL_201_4","message":"푸시 구독 등록에 성공했습니다.","data":{"subscriptionId":1}}
```

### **Response Field**

| **name** | **설명** |
| --- | --- |
| `data.subscriptionId` | 저장된 구독 ID입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `endpoint`/`p256dh`/`auth` 중 하나라도 비어 있는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **21. (사용 중지 예정) 웹 푸시 구독 해지**

`DELETE /api/approvals/push-subscriptions?endpoint={endpoint}`

> ⚠️ 19번과 동일하게 사용 중지 예정이다.

# **[request]**

Request Header

| **name** | **description** |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| **name** | **type** | **required** | **description** |
| --- | --- | --- | --- |
| `endpoint` | `String` | `true` | 해지할 구독의 endpoint |

# **[response]**

### **성공코드**

| **HTTP 상태** | **설명** |
| --- | --- |
| `204 No Content` | 본인 구독 해지 성공 |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## 참고 상태값

| **상태값** | **설명** |
| --- | --- |
| `ApprovalStatus.IN_PROGRESS` | 결재 진행 중입니다. |
| `ApprovalStatus.APPROVED` | 최종 승인 완료 상태입니다. |
| `ApprovalStatus.REJECTED` | 반려 완료 상태입니다. |
| `ApprovalStatus.CANCELLED` | 신청자가 결재 처리 시작 전에 취소한 상태입니다. |
| `ApprovalLineStatus.WAITING` | 아직 내 차례가 오지 않은 결재선입니다. |
| `ApprovalLineStatus.PENDING` | 현재 처리 대기 중인 결재선입니다. |
| `ApprovalLineStatus.APPROVED` | 해당 결재선이 승인 처리됐습니다. |
| `ApprovalLineStatus.REJECTED` | 해당 결재선이 반려 처리됐습니다. |
| `ApprovalContentType.TEXT` | 텍스트 본문 결재입니다. |
| `ApprovalContentType.FILE` | 첨부파일 중심 결재입니다. |
| `ApprovalDecision.APPROVE` | 승인 결정입니다. |
| `ApprovalDecision.REJECT` | 반려 결정입니다. |
| `AttachmentSummaryStatus.PENDING` | 아직 AI 요약을 생성하지 않았습니다. |
| `AttachmentSummaryStatus.COMPLETED` | AI 요약 생성이 완료됐습니다. |
| `AttachmentSummaryStatus.FAILED` | AI 요약 생성이 실패했습니다. |

## 오류 코드 전체 목록

| code | HTTP | 메시지 |
| --- | --- | --- |
| `APPROVAL_400_1` | 400 | 결재 내용 형식이 올바르지 않습니다. |
| `APPROVAL_400_2` | 400 | 결재 제목은 비어 있을 수 없습니다. |
| `APPROVAL_400_3` | 400 | 결재선은 최소 1명 이상 지정해야 합니다. |
| `APPROVAL_400_4` | 400 | 템플릿 이름은 비어 있을 수 없습니다. |
| `APPROVAL_400_5` | 400 | 휴가 기간은 시작일과 종료일을 함께 입력해야 하며, 종료일은 시작일보다 빠를 수 없습니다. |
| `APPROVAL_403_1` | 403 | 해당 결재를 조회할 권한이 없습니다. |
| `APPROVAL_403_2` | 403 | 다른 학원의 템플릿으로는 결재를 신청할 수 없습니다. |
| `APPROVAL_403_3` | 403 | 본인이 신청한 결재만 재상신할 수 있습니다. |
| `APPROVAL_403_4` | 403 | 본인이 신청한 결재만 결재선을 수정할 수 있습니다. |
| `APPROVAL_403_5` | 403 | 해당 템플릿에 접근할 권한이 없습니다. |
| `APPROVAL_403_6` | 403 | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. |
| `APPROVAL_403_7` | 403 | 본인이 신청한 결재만 취소할 수 있습니다. |
| `APPROVAL_404_1` | 404 | 결재 템플릿을 찾을 수 없습니다. |
| `APPROVAL_404_2` | 404 | 결재 문서를 찾을 수 없습니다. |
| `APPROVAL_404_3` | 404 | 사용자를 찾을 수 없습니다. |
| `APPROVAL_404_4` | 404 | 첨부파일을 찾을 수 없습니다. |
| `APPROVAL_409_1` | 409 | 반려된 결재만 재상신할 수 있습니다. |
| `APPROVAL_409_2` | 409 | 이미 재상신된 결재입니다. |
| `APPROVAL_409_3` | 409 | 이미 처리가 완료된 결재입니다. |
| `APPROVAL_409_4` | 409 | 본인 차례의 결재가 아닙니다. |
| `APPROVAL_409_5` | 409 | 이미 결재가 진행된 건이라 결재선을 수정할 수 없습니다. |
| `APPROVAL_409_6` | 409 | 이미 처리가 완료된 결재선입니다. |
| `APPROVAL_409_7` | 409 | 첨부파일 원문 조회 기능이 없어 요약할 수 없습니다. |
| `APPROVAL_409_8` | 409 | 이미 처리가 시작된 결재는 취소할 수 없습니다. |
| `APPROVAL_409_9` | 409 | 처리가 완료된 본인 결재 이력만 삭제할 수 있습니다. |
| `APPROVAL_502_1` | 502 | 첨부파일 요약 생성에 실패했습니다. |
