# 업무(Task) API 명세

> `WORKSPACE_API_FLOW.md`/`TASK_API_FLOW.md`가 워크스페이스와 업무의 호출 흐름을 분리해 문서화하듯, 이 문서는 업무(Task) API 명세만 모은다. 업무 생성·수정·삭제 세 API를 모두 문서화한다.

---

## 업무 생성

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
> `@PreAuthorize`(`WORKSPACE:CREATE`)는 권한 모듈의 코드가 시드되기 전까지 TODO로 보류되어 있으며, 현재는 "현재 참여자" 여부만 검증한다.

---

## 업무 상태·마감일 수정

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
> 수정 대상 업무는 비관적 락(`findByIdForUpdate`)으로 조회해 삭제와의 경합을 막는다.

---

## 업무 삭제

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
| `204 No Content` | 업무 삭제 성공 |

응답 본문은 없다.

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
