# 업무(Task) API 명세

> `WORKSPACE_API_FLOW.md`/`TASK_API_FLOW.md`가 워크스페이스와 업무의 호출 흐름을 분리해 문서화하듯, 이 문서는 업무(Task) API 명세만 모은다. 업무 생성·상세 조회·수정·삭제·내 업무 모아보기 다섯 API를 모두 문서화한다.

---

## 내 업무 모아보기

다른 업무 API와 달리 특정 워크스페이스에 종속되지 않는다 — 내가 참여자로 속한 **모든 워크스페이스**를 가로질러 업무를 모아 반환한다.

### Endpoint

`GET /api/tasks/me`

# **[request]**

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

### Request Parameter

| name | description |
| --- | --- |
| `status` | 선택. `WAITING`/`IN_PROGRESS`/`DELAYED` 중 하나. 생략하거나 `COMPLETED`를 보내면 3개 상태 전체가 조회된다. `COMPLETED` 업무는 이 API에서 절대 노출되지 않는다. |
| `workspaceId` | 선택. 특정 워크스페이스로 필터링한다. 내가 속하지 않은 워크스페이스 번호를 보내면 에러 없이 빈 목록이 반환된다. |
| `page` | 선택. 페이지 번호, 0부터 시작. 기본값 `0`. |
| `size` | 선택. 페이지 크기. 기본값 `20`, 최소 1, 최대 100. |

Request Body

없음

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 내 업무 목록 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "WORKSPACE_200_18",
  "message": "내 업무 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "taskId": 101,
        "workspaceId": 1,
        "workspaceName": "8월 학사 운영",
        "title": "9월 시간표 초안 작성",
        "dueAt": "2026-08-10",
        "status": "WAITING"
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
| `data.content` | 업무 목록. 기한(`dueAt`) 오름차순으로 정렬된다. |
| `data.content[].taskId` | 업무 번호. 워크스페이스 업무 상세 조회(`GET /api/workspaces/{workspaceId}/tasks/{taskId}`)로 이동할 때 사용 |
| `data.content[].workspaceId` | 업무가 속한 워크스페이스 번호. 위와 함께 상세 조회 경로에 사용 |
| `data.content[].workspaceName` | 업무가 속한 워크스페이스 이름 |
| `data.content[].title` | 업무 제목 |
| `data.content[].dueAt` | 업무 기한. 반복 업무(회차)는 원래 마감일이 없어, 발생일(`scheduledFor`)의 날짜가 대신 채워진다. |
| `data.content[].status` | 업무 상태. `WAITING`/`IN_PROGRESS`/`DELAYED` 중 하나만 나온다. |
| `data.page` | 현재 페이지 번호 |
| `data.size` | 요청한 페이지 크기 |
| `data.hasNext` | 다음 페이지 존재 여부. 전체 개수(`totalElements`)는 제공하지 않는다. |

> 조회 결과가 없으면(내가 속한 워크스페이스가 없거나, 조건에 맞는 업무가 없는 경우) `content`는 빈 배열로 반환되며 에러가 아니다. 등록자 정보는 이 API 응답에 포함되지 않는다. 소프트 삭제된 워크스페이스의 업무는 제외된다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `page`가 음수, `size`가 1 미만/100 초과, `status`에 유효하지 않은 값을 전달한 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |

> 워크스페이스 멤버십만으로 접근을 제어한다 — `WORKSPACE:READ_ALL` 같은 별도 권한 체크가 없고, `academyId` 검증도 하지 않는다(실제 배포가 학원별 DB 스키마 분리 구조라 애플리케이션 레벨의 academyId 필터링이 불필요하다).
> 무한스크롤용 offset 페이지네이션(page/size, 기본 20개)은 댓글 목록 조회와 동일한 컨벤션이다.

---

## 업무 생성

### Endpoint

`POST /api/workspaces/{workspaceId}/tasks`

# **[request]**

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 업무를 생성할 워크스페이스 번호 |

### Request Parameter

없음

Request Body

```json
{
  "title": "8월 원생 청구서 발송",
  "dueAt": "2026-08-10"
}
```

| name | 설명 |
| --- | --- |
| `title` | 업무 제목. 필수. 앞뒤 공백 제거 후 공백만 남으면 거부. 최대 200자(trim 후 기준). |
| `dueAt` | 마감일. 필수. `yyyy-MM-dd` 형식. 과거 날짜도 허용하며, 이 경우 초기 상태가 `DELAYED`로 생성된다. 오늘·미래 날짜는 `WAITING`으로 생성된다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 업무 생성 성공 |

Response Body

```json
{
  "status": 201,
  "code": "WORKSPACE_201_2",
  "message": "업무 생성에 성공했습니다.",
  "data": {
    "taskId": 101
  }
}
```

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 서비스 응답 코드 |
| `message` | 응답 메시지 |
| `data.taskId` | 생성된 업무 번호 |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `title`이 비어있거나(trim 후 공백) 200자를 초과, `dueAt`이 없거나 형식이 잘못된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 해당 워크스페이스의 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |

실패 예시 (`400 Bad Request`, 제목 누락)

```json
{
  "timestamp": "2026-08-07T12:00:00",
  "status": 400,
  "code": "COMMON_400_1",
  "message": "입력값이 올바르지 않습니다.",
  "traceId": "a1b2c3d4",
  "details": {
    "errors": [
      { "field": "title", "reason": "업무 제목은 필수입니다." }
    ]
  }
}
```

> 존재 확인을 권한 확인보다 먼저 수행한다 — 워크스페이스가 없거나 삭제된 경우 참여 여부와 무관하게 `404`를 반환한다.
> 업무 생성/수정/삭제는 참여자 여부만 검증한다(권한 체크 없음, 2026-08-10 확정 — `WORKSPACE:CREATE`는 워크스페이스 생성에만 적용된다).

---

## 업무 상세 조회

### Endpoint

`GET /api/workspaces/{workspaceId}/tasks/{taskId}`

# **[request]**

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 업무가 속한 워크스페이스 번호 |
| `taskId` | 조회할 업무 번호 |

### Request Parameter

없음

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 업무 상세 조회 성공 |

Response Body (상태 변경 이력이 있는 경우)

```json
{
  "status": 200,
  "code": "WORKSPACE_200_16",
  "message": "업무 상세 조회에 성공했습니다.",
  "data": {
    "taskId": 101,
    "title": "성적 데이터 7월분 엑셀 정리",
    "creator": { "userId": 10, "name": "윤예진" },
    "createdAt": "2026-07-29T09:30:00",
    "status": "IN_PROGRESS",
    "dueAt": "2026-08-05",
    "lastStatusChangedAt": "2026-08-02T09:00:00"
  }
}
```

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 서비스 응답 코드 |
| `message` | 응답 메시지 |
| `data.taskId` | 업무 번호 |
| `data.title` | 업무 제목 |
| `data.creator` | 등록자. `{ userId, name }`. 이름을 찾지 못하면 `"알 수 없음"` |
| `data.createdAt` | 등록일시 |
| `data.status` | 현재 상태 |
| `data.dueAt` | 기한. 반복 업무는 `null` |
| `data.lastStatusChangedAt` | 최종 상태 변경일시. **한 번도 상태가 바뀌지 않았으면 이 필드 자체가 응답에서 생략된다**(`null` 값이 아니라 키가 없음). |

> **최종 상태 변경자(누가 바꿨는지)는 응답에 포함하지 않는다.** `TaskStatusHistory.changedBy` 자체는 이력마다 저장되고 있지만, 이 API는 시각만 노출하도록 설계 시 프론트와 합의했다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |
| `404 Not Found` | `WORKSPACE_404_3` | 업무를 찾을 수 없습니다. | 업무가 없거나 해당 워크스페이스 소속이 아닌 경우 |

> 존재 확인(워크스페이스 → 업무)을 참여자 확인보다 먼저 수행한다. 다른 워크스페이스에 속한 업무 번호를 보내면 존재를 노출하지 않기 위해 `403`이 아니라 `WORKSPACE_404_3`을 반환한다.
> 조회 전용 API라 `TaskRepository.findById(workspaceId, taskId)`(락 없음)로 조회한다 — 수정·삭제가 쓰는 `findByIdForUpdate`(비관적 락)와는 별개의 메서드다.

---

## 업무 상태·마감일 수정

### Endpoint

`PATCH /api/workspaces/{workspaceId}/tasks/{taskId}`

# **[request]**

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 업무가 속한 워크스페이스 번호 |
| `taskId` | 수정할 업무 번호 |

### Request Parameter

없음

Request Body

```json
{
  "status": "IN_PROGRESS",
  "dueAt": "2026-08-20"
}
```

| name | 설명 |
| --- | --- |
| `status` | 선택. `WAITING` / `IN_PROGRESS` / `COMPLETED` / `DELAYED` 중 하나. |
| `dueAt` | 선택. 새 마감일(`yyyy-MM-dd`). |

`status`·`dueAt` 둘 다 생략하면 `400`이다. `status`만 보내면 상태만 전이하고, `dueAt`만 보내면 마감일만 수정하며 상태는 바꾸지 않는다. 둘 다 보내면 새 마감일을 반영한 뒤 상태를 전이한다.

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 업무 수정 성공 |

Response Body

```json
{
  "status": 200,
  "code": "WORKSPACE_200_6",
  "message": "업무 수정에 성공했습니다.",
  "data": {
    "taskId": 101,
    "status": "IN_PROGRESS",
    "dueAt": "2026-08-20"
  }
}
```

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 서비스 응답 코드 |
| `message` | 응답 메시지 |
| `data.taskId` | 업무 번호 |
| `data.status` | 반영된 업무 상태 |
| `data.dueAt` | 반영된 마감일. 반복 업무는 `null` |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `status`·`dueAt`을 모두 생략했거나 형식이 잘못된 경우 |
| `400 Bad Request` | `WORKSPACE_400_3` | 허용되지 않은 업무 상태 변경입니다. | 완료된 업무를 `DELAYED`로 바꾸려는 경우 |
| `400 Bad Request` | `WORKSPACE_400_4` | 기한이 지난 업무는 오늘 이후의 새 마감일과 함께 변경해야 합니다. | 기한이 지난 업무를 대기·진행 중으로 되돌리는데 오늘 이후의 새 마감일이 없는 경우 |
| `400 Bad Request` | `WORKSPACE_400_5` | 반복 업무는 마감일을 수정할 수 없습니다. | 반복 업무의 마감일을 수정하려는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |
| `404 Not Found` | `WORKSPACE_404_3` | 업무를 찾을 수 없습니다. | 업무가 없거나 해당 워크스페이스 소속이 아닌 경우 |

> 다른 워크스페이스에 속한 업무 번호를 보내면 존재를 노출하지 않기 위해 `403`이 아니라 `WORKSPACE_404_3`을 반환한다.
> 같은 상태로의 전이는 성공으로 응답하되 상태 이력을 남기지 않는다(이때 `dueAt`이 함께 왔으면 마감일은 반영됨).
> 수정 대상 업무는 비관적 락(`findByIdForUpdate`, `(workspaceId, taskId)`로 워크스페이스 범위 제한)으로 조회해 삭제와의 경합을 막는다.

---

## 업무 삭제

### Endpoint

`DELETE /api/workspaces/{workspaceId}/tasks/{taskId}`

# **[request]**

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 업무가 속한 워크스페이스 번호 |
| `taskId` | 삭제할 업무 번호 |

### Request Parameter

없음

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 업무 삭제 성공 |

Response Body

```json
{
  "status": 200,
  "code": "WORKSPACE_200_13",
  "message": "업무 삭제에 성공했습니다.",
  "data": null
}
```

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |
| `404 Not Found` | `WORKSPACE_404_3` | 업무를 찾을 수 없습니다. | 업무가 없거나 해당 워크스페이스 소속이 아닌 경우 |

> 하드 삭제이며 복구할 수 없다. 업무 댓글, 댓글 멘션, 상태 변경 이력을 함께 삭제한다.
> 반복 업무의 회차를 삭제하면 같은 트랜잭션에서 `recurring_task_skip`에 `(recurring_template_id, scheduled_for)` 기록을 남긴다. 같은 기록이 이미 있으면 중복 오류 없이 멱등적으로 처리한다. 일반 업무 삭제 시에는 skip 기록을 남기지 않는다.
> 다른 워크스페이스에 속한 업무 번호를 보내면 존재를 노출하지 않기 위해 `403`이 아니라 `WORKSPACE_404_3`을 반환한다.

---

## 참고 문서

- [WORKSPACE_API.md](WORKSPACE_API.md) — 워크스페이스 API 명세 (업무 생성·수정·삭제 섹션이 이 문서와 중복 수록되어 있음, 추후 정리 예정)
- [TASK_API_FLOW.md](TASK_API_FLOW.md) — 업무 자동 지연 스케줄러 호출 흐름과 업무 생성·수정·삭제 API 호출 흐름
- [BUSINESS_RULES.md](BUSINESS_RULES.md) — 업무 상태 결정·전이 규칙
