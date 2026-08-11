# 전자결재(Approval) API 명세서

> 아래 각 `## ` 섹션이 Notion 하위 페이지 1개(엔드포인트 1개)에 대응합니다. 섹션별로 그대로 복사해서 각 페이지에 붙여넣으세요.
> 공통 성공 응답 포맷: `{ "status", "code", "message", "data" }` (`GlobalApiResponse`)
> 공통 실패 응답 포맷: `{ "status", "code", "message", "data": null }`
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요합니다 (미인증 시 `401 COMMON_401_1`).

---

## 1. 결재 템플릿 생성

`POST /api/approval-templates`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body
```json
{
  "name": "휴가 신청서",
  "approverIds": [10, 11]
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `name` | `String` | `true` | 템플릿 이름. 공백 불가. |
| `approverIds` | `List<Long>` | `true` | 기본 결재선 담당자 ID 목록(순서대로 1차, 2차...). 최소 1명 이상. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 템플릿 생성 성공 |

Response Body
```json
{
  "status": 201,
  "code": "APPROVAL_201_1",
  "message": "결재 템플릿 생성에 성공했습니다.",
  "data": { "templateId": 1 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.templateId` | 생성된 템플릿 ID입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `APPROVAL_400_4` | 템플릿 이름은 비어 있을 수 없습니다. | `name`이 공백/누락 |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `approverIds`가 비어 있음(`@NotEmpty` 검증 실패) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 소속 사용자 포함 |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자 포함 |

---

## 2. 결재 템플릿 목록 조회

`GET /api/approval-templates`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 요청자 소속 학원의 템플릿 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "APPROVAL_200_1",
  "message": "결재 템플릿 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "휴가 신청서",
        "createdAt": "2026-08-04T10:00:00",
        "lines": [
          { "stepOrder": 1, "approverId": 10, "approverName": "김팀장" }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.content[].id` | 템플릿 ID입니다. |
| `data.content[].name` | 템플릿 이름입니다. |
| `data.content[].createdAt` | 템플릿 생성 시각(KST)입니다. |
| `data.content[].lines[].stepOrder` | 결재 순번입니다. |
| `data.content[].lines[].approverId` | 결재자 사용자 ID입니다. |
| `data.content[].lines[].approverName` | 결재자 이름입니다. |
| `data.page` | 현재 페이지 번호(0-based)입니다. |
| `data.size` | 페이지 크기입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 3. 결재 템플릿 상세 조회

`GET /api/approval-templates/{templateId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `templateId` | 조회할 템플릿 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 템플릿 상세 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "APPROVAL_200_2",
  "message": "결재 템플릿 상세 조회에 성공했습니다.",
  "data": {
    "id": 1,
    "name": "휴가 신청서",
    "creatorId": 5,
    "createdAt": "2026-08-04T10:00:00",
    "lines": [
      { "stepOrder": 1, "approverId": 10, "approverName": "김팀장" }
    ]
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.id` | 템플릿 ID입니다. |
| `data.name` | 템플릿 이름입니다. |
| `data.creatorId` | 템플릿 생성자 사용자 ID입니다. |
| `data.createdAt` | 생성 시각(KST)입니다. |
| `data.lines[].stepOrder` | 결재 순번입니다. |
| `data.lines[].approverId` | 결재자 사용자 ID입니다. |
| `data.lines[].approverName` | 결재자 이름입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_5` | 해당 템플릿에 접근할 권한이 없습니다. | 다른 학원 소속 템플릿 조회 시도 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | 존재하지 않는 `templateId` |

---

## 4. 결재 템플릿 수정

`PATCH /api/approval-templates/{templateId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `templateId` | 수정할 템플릿 ID |

Request Body
```json
{
  "name": "휴가 신청서(수정)",
  "approverIds": [10, 12]
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `name` | `String` | `true` | 새 템플릿 이름. |
| `approverIds` | `List<Long>` | `true` | 새 결재선 담당자 ID 목록(기존 값을 전체 교체). |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 수정 성공(본문 없음) |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `APPROVAL_400_4` | 템플릿 이름은 비어 있을 수 없습니다. | `name`이 공백/누락 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_5` | 해당 템플릿에 접근할 권한이 없습니다. | 다른 학원 소속 템플릿 수정 시도 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 소속 사용자 포함 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | 존재하지 않는 `templateId` |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자 포함 |

> ⚠️ 현재는 요청자 본인이 작성자인지 여부와 무관하게 같은 학원 소속이면 수정 가능합니다(작성자 전용 제한은 `users.role` 값 체계 확정 후 추가 예정, 컨트롤러 TODO 참고).

---

## 5. 결재 템플릿 삭제

`DELETE /api/approval-templates/{templateId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `templateId` | 삭제할 템플릿 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 삭제 성공(본문 없음) |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_5` | 해당 템플릿에 접근할 권한이 없습니다. | 다른 학원 소속 템플릿 삭제 시도 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | 존재하지 않는 `templateId` |

---

## 6. 결재 신청

`POST /api/approvals`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body
```json
{
  "templateId": 1,
  "title": "8월 휴가 신청",
  "contentType": "TEXT",
  "text": "8/10~8/12 휴가 신청합니다.",
  "fileIds": [101, 102],
  "approverIds": [10, 11]
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `templateId` | `Long` | `true` | 사용할 결재 템플릿 ID. |
| `title` | `String` | `true` | 결재 제목. 공백 불가. |
| `contentType` | `String` | `true` | 결재 내용 유형. `TEXT` 또는 `FILE` 중 하나. |
| `text` | `String` | `false` | 텍스트 결재 내용(`contentType=TEXT`일 때 사용). |
| `fileIds` | `List<Long>` | `false` | 첨부파일 ID 목록(다중 첨부 가능). |
| `approverIds` | `List<Long>` | `false` | 결재선 담당자 ID 목록. 생략 시 템플릿의 기본 결재선을 사용. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 결재 신청 성공 |

Response Body
```json
{
  "status": 201,
  "code": "APPROVAL_201_2",
  "message": "결재 신청에 성공했습니다.",
  "data": { "documentId": 100 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.documentId` | 생성된 결재 문서 ID입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `APPROVAL_400_1` | 결재 내용(텍스트)이 올바르지 않습니다. | `contentType=TEXT`인데 `text`가 비어 있음 등 |
| `400 Bad Request` | `APPROVAL_400_2` | 결재 제목은 비어 있을 수 없습니다. | `title`이 공백/누락 |
| `400 Bad Request` | `APPROVAL_400_3` | 결재선은 최소 1명 이상 지정해야 합니다. | 결재선(템플릿 기본값 포함)이 비어 있음 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_2` | 다른 학원의 템플릿으로는 결재를 신청할 수 없습니다. | 신청자와 템플릿 소속 학원 불일치 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 소속 사용자 포함 |
| `404 Not Found` | `APPROVAL_404_1` | 결재 템플릿을 찾을 수 없습니다. | 존재하지 않는 `templateId` |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자 포함 |

---

## 7. 내게 온 결재 목록 조회

`GET /api/approvals/me`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 내가 결재선에 포함된 결재 문서 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "APPROVAL_200_3",
  "message": "내게 온 결재 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 100,
        "title": "8월 휴가 신청",
        "templateName": "휴가 신청서",
        "creatorName": "이사원",
        "status": "IN_PROGRESS",
        "myStepOrder": 1,
        "myLineStatus": "PENDING",
        "currentApproverStepOrder": 1,
        "currentApproverName": "김팀장",
        "createdAt": "2026-08-04T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.content[].id` | 결재 문서 ID입니다. |
| `data.content[].title` | 결재 제목입니다. |
| `data.content[].templateName` | 사용된 템플릿 이름입니다. |
| `data.content[].creatorName` | 신청자 이름입니다. |
| `data.content[].status` | 문서 전체 상태입니다. `IN_PROGRESS`/`APPROVED`/`REJECTED` 중 하나. |
| `data.content[].myStepOrder` | 요청자의 결재 순번입니다. |
| `data.content[].myLineStatus` | 요청자의 결재선 상태입니다. `WAITING`/`PENDING`/`APPROVED`/`REJECTED` 중 하나. |
| `data.content[].currentApproverStepOrder` | 현재 차례 결재자의 순번입니다(없으면 `null`). |
| `data.content[].currentApproverName` | 현재 차례 결재자 이름입니다(없으면 `null`). |
| `data.content[].createdAt` | 신청 시각(KST)입니다. |
| `data.page` | 현재 페이지 번호(0-based)입니다. |
| `data.size` | 페이지 크기입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 8. 내가 신청한 결재 목록 조회

`GET /api/approvals/me/submitted`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `page` | `int` | `false` | 페이지 번호(0부터 시작). 기본값 `0`. |
| `size` | `int` | `false` | 페이지 크기. 기본값 `20`. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 내가 신청자인 결재 문서 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "APPROVAL_200_4",
  "message": "내가 신청한 결재 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 100,
        "title": "8월 휴가 신청",
        "templateName": "휴가 신청서",
        "creatorName": "이사원",
        "status": "IN_PROGRESS",
        "currentApproverStepOrder": 1,
        "currentApproverName": "김팀장",
        "createdAt": "2026-08-04T10:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.content[].id` | 결재 문서 ID입니다. |
| `data.content[].title` | 결재 제목입니다. |
| `data.content[].templateName` | 사용된 템플릿 이름입니다. |
| `data.content[].creatorName` | 신청자 이름입니다. |
| `data.content[].status` | 문서 전체 상태입니다. `IN_PROGRESS`/`APPROVED`/`REJECTED` 중 하나. |
| `data.content[].currentApproverStepOrder` | 현재 차례 결재자의 순번입니다(없으면 `null`). |
| `data.content[].currentApproverName` | 현재 차례 결재자 이름입니다(없으면 `null`). |
| `data.content[].createdAt` | 신청 시각(KST)입니다. |
| `data.page` | 현재 페이지 번호(0-based)입니다. |
| `data.size` | 페이지 크기입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 9. 결재 대기 건수 조회

`GET /api/approvals/me/pending-count`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 조회 성공(사이드바 뱃지용) |

Response Body
```json
{
  "status": 200,
  "code": "APPROVAL_200_6",
  "message": "결재 대기 건수 조회에 성공했습니다.",
  "data": { "count": 3 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.count` | 내가 현재 차례(`PENDING`)인 진행중 결재 건수입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 10. 결재 상세 조회

`GET /api/approvals/{documentId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `documentId` | 조회할 결재 문서 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 결재 상세 조회 성공(신청자 본인 또는 결재선 포함자만) |

Response Body
```json
{
  "status": 200,
  "code": "APPROVAL_200_5",
  "message": "결재 상세 조회에 성공했습니다.",
  "data": {
    "id": 100,
    "templateId": 1,
    "templateName": "휴가 신청서",
    "title": "8월 휴가 신청",
    "contentType": "TEXT",
    "text": "8/10~8/12 휴가 신청합니다.",
    "attachments": [
      {
        "fileId": 101,
        "aiSummary": null,
        "summaryStatus": "PENDING",
        "summarizedAt": null
      }
    ],
    "creatorId": 5,
    "creatorName": "이사원",
    "status": "IN_PROGRESS",
    "createdAt": "2026-08-04T10:00:00",
    "lines": [
      {
        "lineId": 1000,
        "stepOrder": 1,
        "approverId": 10,
        "approverName": "김팀장",
        "status": "PENDING",
        "comment": null,
        "decidedAt": null
      }
    ]
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.id` | 결재 문서 ID입니다. |
| `data.templateId` | 사용된 템플릿 ID입니다. |
| `data.templateName` | 사용된 템플릿 이름입니다. |
| `data.title` | 결재 제목입니다. |
| `data.contentType` | 내용 유형입니다. `TEXT`/`FILE` 중 하나. |
| `data.text` | 텍스트 결재 내용입니다(`FILE` 유형이면 `null`). |
| `data.attachments[].fileId` | 첨부파일 ID입니다. |
| `data.attachments[].aiSummary` | AI 요약 결과입니다(아직 생성 전이면 `null`). |
| `data.attachments[].summaryStatus` | 요약 상태입니다. `PENDING`/`COMPLETED`/`FAILED` 중 하나. |
| `data.attachments[].summarizedAt` | 요약 완료/실패 시각(KST)입니다(없으면 `null`). |
| `data.creatorId` | 신청자 사용자 ID입니다. |
| `data.creatorName` | 신청자 이름입니다. |
| `data.status` | 문서 전체 상태입니다. `IN_PROGRESS`/`APPROVED`/`REJECTED` 중 하나. |
| `data.createdAt` | 신청 시각(KST)입니다. |
| `data.lines[].lineId` | 결재선 항목 ID입니다. |
| `data.lines[].stepOrder` | 결재 순번입니다. |
| `data.lines[].approverId` | 결재자 사용자 ID입니다. |
| `data.lines[].approverName` | 결재자 이름입니다. |
| `data.lines[].status` | 해당 결재선의 상태입니다. `WAITING`/`PENDING`/`APPROVED`/`REJECTED` 중 하나. |
| `data.lines[].comment` | 결재 코멘트입니다(승인/반려 전이면 `null`). |
| `data.lines[].decidedAt` | 승인/반려 처리 시각(KST)입니다(없으면 `null`). |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_1` | 해당 결재를 조회할 권한이 없습니다. | 신청자 본인도, 결재선 포함자도 아님 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | 존재하지 않는 `documentId` |

---

## 11. 결재선 수정

`PATCH /api/approvals/{documentId}/lines`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `documentId` | 결재선을 수정할 결재 문서 ID |

Request Body
```json
{
  "approverIds": [10, 12]
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `approverIds` | `List<Long>` | `true` | 새 결재선 담당자 ID 목록(순서대로 적용). 최소 1명 이상. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 수정 성공(본문 없음) |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `approverIds`가 비어 있음 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_4` | 본인이 신청한 결재만 결재선을 수정할 수 있습니다. | 신청자 본인이 아님 |
| `403 Forbidden` | `APPROVAL_403_6` | 다른 학원 소속 사용자를 결재자로 지정할 수 없습니다. | `approverIds`에 다른 학원 소속 사용자 포함 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | 존재하지 않는 `documentId` |
| `404 Not Found` | `APPROVAL_404_3` | 사용자를 찾을 수 없습니다. | `approverIds`에 존재하지 않는 사용자 포함 |
| `409 Conflict` | `APPROVAL_409_5` | 이미 결재가 진행된 건은 결재선을 수정할 수 없습니다. | 결재선 중 하나라도 이미 승인/반려 처리됨 |

---

## 12. 결재 승인/반려

`POST /api/approvals/{documentId}/decide`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `documentId` | 승인/반려할 결재 문서 ID |

Request Body
```json
{
  "decision": "APPROVE",
  "comment": "확인했습니다."
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `decision` | `String` | `true` | `APPROVE`(승인) 또는 `REJECT`(반려) 중 하나. |
| `comment` | `String` | `false` | 결재 코멘트. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 승인/반려 처리 성공(본문 없음) |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | 존재하지 않는 `documentId` |
| `409 Conflict` | `APPROVAL_409_3` | 이미 처리가 완료된 결재입니다. | 문서가 이미 `APPROVED`/`REJECTED` 상태 |
| `409 Conflict` | `APPROVAL_409_4` | 본인 차례의 결재가 아닙니다. | 현재 차례의 결재자가 아님 |
| `409 Conflict` | `APPROVAL_409_6` | 이미 처리가 완료된 결재선입니다. | 해당 결재선이 이미 승인/반려됨 |

---

## 13. 결재 재상신

`POST /api/approvals/{documentId}/resubmit`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `documentId` | 재상신할(반려된) 결재 문서 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 재상신 성공(새 결재 문서 생성) |

Response Body
```json
{
  "status": 201,
  "code": "APPROVAL_201_3",
  "message": "결재 재상신에 성공했습니다.",
  "data": { "documentId": 105 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.documentId` | 재상신으로 새로 생성된 결재 문서 ID입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_3` | 본인이 신청한 결재만 재상신할 수 있습니다. | 신청자 본인이 아님 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | 존재하지 않는 `documentId` |
| `409 Conflict` | `APPROVAL_409_1` | 반려된 결재만 재상신할 수 있습니다. | 문서 상태가 `REJECTED`가 아님 |
| `409 Conflict` | `APPROVAL_409_2` | 이미 재상신된 결재입니다. | 문서 1건당 재상신은 1회로 제한 |

---

## 14. 첨부파일 AI 요약 생성

`POST /api/approvals/{documentId}/attachments/{fileId}/summarize`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `documentId` | 첨부파일이 속한 결재 문서 ID |
| `fileId` | 요약을 생성할 첨부파일의 파일 ID |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | AI 요약 생성 성공 |

Response Body
```json
{
  "status": 200,
  "code": "APPROVAL_200_7",
  "message": "첨부파일 요약 생성에 성공했습니다.",
  "data": {
    "fileId": 101,
    "aiSummary": "휴가 신청 사유 및 기간에 대한 요약입니다.",
    "summaryStatus": "COMPLETED",
    "summarizedAt": "2026-08-04T10:05:00"
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.fileId` | 첨부파일 ID입니다. |
| `data.aiSummary` | Gemini API가 생성한 요약 텍스트입니다. |
| `data.summaryStatus` | 요약 상태입니다. `PENDING`/`COMPLETED`/`FAILED` 중 하나. |
| `data.summarizedAt` | 요약 완료 시각(KST)입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `APPROVAL_403_1` | 해당 결재를 조회할 권한이 없습니다. | 신청자 본인도, 결재선 포함자도 아님 |
| `404 Not Found` | `APPROVAL_404_2` | 결재 문서를 찾을 수 없습니다. | 존재하지 않는 `documentId` |
| `404 Not Found` | `APPROVAL_404_4` | 첨부파일을 찾을 수 없습니다. | 문서에 속하지 않거나 존재하지 않는 `fileId` |
| `409 Conflict` | `APPROVAL_409_7` | 첨부파일 원문 조회 기능이 없어 요약할 수 없습니다. | 지원하지 않는 형식(hwp 등)이거나, PDF/이미지 용량이 15MB를 초과하거나, docx 텍스트 추출에 실패한 경우 |
| `502 Bad Gateway` | `APPROVAL_502_1` | 첨부파일 요약 생성에 실패했습니다. | Gemini API 호출 실패 |

지원 형식: UTF-8 텍스트, PDF(`application/pdf`), 이미지(`image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif`), Word 문서(`.docx`). PDF/이미지는 Gemini 멀티모달 입력으로 직접 전달되고, docx는 서버에서 텍스트를 추출해 전달된다. PDF/이미지는 15MB를 초과하면 요약할 수 없다. hwp 등 위 목록에 없는 형식은 지원하지 않는다.

---

## 15. 푸시 구독 등록

`POST /api/approvals/push-subscriptions`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body
```json
{
  "endpoint": "https://fcm.googleapis.com/fcm/send/xxxxx",
  "p256dh": "BN...",
  "auth": "k8J..."
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `endpoint` | `String` | `true` | 브라우저 푸시 구독 endpoint URL. |
| `p256dh` | `String` | `true` | 구독 공개키(p256dh). |
| `auth` | `String` | `true` | 구독 인증 비밀값(auth secret). |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 등록 성공(같은 사용자·endpoint 조합이 이미 있으면 새로 만들지 않고 키만 갱신) |

Response Body
```json
{
  "status": 201,
  "code": "APPROVAL_201_4",
  "message": "푸시 구독 등록에 성공했습니다.",
  "data": { "subscriptionId": 7 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드입니다. |
| `code` | 서비스 응답 코드입니다. |
| `message` | 응답 메시지입니다. |
| `data.subscriptionId` | 구독 정보 ID입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `endpoint`/`p256dh`/`auth` 중 공백/누락 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

> ⚠️ 알려진 제한사항: 구독 정보 저장까지만 구현되어 있으며, 실제 푸시 발송(VAPID, web-push 라이브러리 연동)은 아직 구현하지 않았습니다.

---

## 16. 푸시 구독 해지

`DELETE /api/approvals/push-subscriptions`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `endpoint` | `String` | `true` | 해지할 구독의 endpoint URL. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 해지 성공(본문 없음). 본인 구독만 해지 가능. |

Response Body

`(없음)`

### Response Field

`(없음)`

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `endpoint` 쿼리 파라미터 누락 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
