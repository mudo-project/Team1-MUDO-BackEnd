# approval API

기준일: 2026-08-07

> 이 문서는 Spring Boot Controller, Response DTO, ErrorCode 구현 기준으로 작성한다.  
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요하다.

## 권한 정책

| 권한 코드 | 적용 기능 |
| --- | --- |
| `APPROVAL:SUBMIT` | 결재 신청, 결재 재상신 |
| `APPROVAL:TEMPLATE_MANAGE` | 결재 템플릿 생성/수정/삭제 |
| `APPROVAL:SUBMIT` 또는 `APPROVAL:TEMPLATE_MANAGE` | 결재 템플릿 목록/상세 조회 |
| `APPROVAL:READ_ALL` | 소속 학원 전체 결재 문서 목록 조회, 전체 조회 권한자 상세 조회 |

---

## **1. 전체 결재 목록 조회**

`GET /api/approvals`

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

## **2. 내 결재 이력 조회**

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
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
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
| `data.page` | 현재 페이지 번호(0-based)입니다. |
| `data.size` | 페이지 크기입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |

### **실패 코드**

| **HTTP 상태** | **code** | **message** | **설명** |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

---

## **3. 결재 신청 취소**

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

## **4. 내 결재 이력 삭제**

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

## 참고 상태값

| **상태값** | **설명** |
| --- | --- |
| `IN_PROGRESS` | 결재 진행 중입니다. |
| `APPROVED` | 최종 승인 완료 상태입니다. |
| `REJECTED` | 반려 완료 상태입니다. |
| `CANCELLED` | 신청자가 결재 처리 시작 전에 취소한 상태입니다. |
