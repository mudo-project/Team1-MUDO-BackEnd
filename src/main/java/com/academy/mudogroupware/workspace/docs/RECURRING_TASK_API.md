# 반복 업무 템플릿(Recurring Task Template) API 명세

> `TASK_API.md`가 업무 생성·수정·삭제를 모아 문서화하듯, 이 문서는 반복 업무 템플릿의 생성·목록 조회 API를 모은다.

---

## 반복 업무 템플릿 생성

### Endpoint

`POST /api/workspaces/{workspaceId}/recurring-templates`

# **[request]**

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 템플릿을 생성할 워크스페이스 번호 |

### Request Parameter

없음

Request Body

```json
{
  "title": "주간 출결 현황 정리",
  "recurrenceType": "WEEKLY",
  "recurrenceRule": {
    "daysOfWeek": [1]
  }
}
```

| name | 설명 |
| --- | --- |
| `title` | 템플릿 제목. 필수. 앞뒤 공백 제거 후 공백만 남으면 거부. 최대 200자(trim 후 기준). |
| `recurrenceType` | 반복 주기 타입. 필수. `WEEKLY` 또는 `MONTHLY`만 허용. |
| `recurrenceRule` | 주기별 부가 정보. 필수. `WEEKLY`는 `{"daysOfWeek":[1,3,5]}`(월=1~일=7, 최소 1개), `MONTHLY`는 `{"dayOfMonth":1}`(현재 1일만 허용). 정수가 아닌 값(예: `1.5`)은 거부한다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 템플릿 생성 성공 |

Response Body

```json
{
  "status": 201,
  "code": "WORKSPACE_201_4",
  "message": "반복 업무 템플릿 생성에 성공했습니다.",
  "data": {
    "templateId": 1
  }
}
```

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 서비스 응답 코드 |
| `message` | 응답 메시지 |
| `data.templateId` | 생성된 템플릿 번호 |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `title`이 비어있거나 200자를 초과, `recurrenceType`이 없거나 잘못된 경우 |
| `400 Bad Request` | `WORKSPACE_400_7` | 반복 주기 설정이 올바르지 않습니다. | `recurrenceRule`이 주기 타입과 맞지 않거나(범위 초과, 정수 아님 등, 소수 값 포함) 필수 키가 없는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 해당 워크스페이스의 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |

> 존재 확인을 권한 확인보다 먼저 수행한다 — 워크스페이스가 없거나 삭제된 경우 참여 여부와 무관하게 `404`를 반환한다.
> `@PreAuthorize`(`WORKSPACE:CREATE`)는 권한 모듈의 코드가 시드되기 전까지 TODO로 보류되어 있으며, 현재는 "현재 참여자" 여부만 검증한다.
> 반복 주기는 제품 요구사항에 따라 `WEEKLY`(요일 지정)와 `MONTHLY`(매달 1일 고정)만 지원한다. `DAILY`는 지원하지 않는다.

---

## 반복 업무 템플릿 목록 조회

### Endpoint

`GET /api/workspaces/{workspaceId}/recurring-templates`

# **[request]**

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 템플릿 목록을 조회할 워크스페이스 번호 |

### Request Parameter

| name | type | required | default | description |
| --- | --- | --- | --- | --- |
| `page` | int | false | `0` | 조회할 페이지 번호(0부터 시작) |
| `size` | int | false | `20` | 페이지당 조회 개수 |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 목록 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "WORKSPACE_200_9",
  "message": "반복 업무 템플릿 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "templateId": 2,
        "title": "주간 출결 현황 정리",
        "recurrenceType": "WEEKLY",
        "recurrenceRule": { "daysOfWeek": [1] },
        "createdBy": 10
      }
    ],
    "page": 0,
    "size": 20,
    "hasNext": false
  }
}
```

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 서비스 응답 코드 |
| `message` | 응답 메시지 |
| `data.content` | 템플릿 목록. 각 항목은 생성 API 응답과 동일한 필드(`templateId`, `title`, `recurrenceType`, `recurrenceRule`, `createdBy`)를 담는다. |
| `data.page` | 현재 페이지 번호 |
| `data.size` | 요청한 페이지 크기 |
| `data.hasNext` | 다음 페이지 존재 여부. 전체 개수(count)는 계산하지 않는다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |

> 존재 확인을 권한 확인보다 먼저 수행한다 — 생성 API와 동일한 순서.
> 정렬 기준은 생성일 내림차순(`createdAt desc`)이며, 같은 시각에 생성된 템플릿이 있을 경우 템플릿 번호 내림차순(`id desc`)을 2차 기준으로 사용해 페이지 간 순서가 흔들리지 않도록 한다.
> `page`·`size`에 대한 별도 입력 검증(음수, 상한 등)은 아직 없다 — 프로젝트의 다른 목록 조회 API(`공지사항 목록 조회`)와 동일한 수준이다.

---

## 참고 문서

- [TASK_API.md](TASK_API.md) — 업무(Task) API 명세
- [RECURRING_TASK_API_FLOW.md](RECURRING_TASK_API_FLOW.md) — 반복 업무 템플릿 생성·목록 조회 API 호출 흐름
- [BUSINESS_RULES.md](BUSINESS_RULES.md) — 업무 상태 결정·전이 규칙
