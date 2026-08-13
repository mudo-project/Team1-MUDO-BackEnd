# 업무 댓글·멘션 API 명세

> `TASK_API.md`가 업무(Task) 자체의 CRUD를 다루듯, 이 문서는 업무 댓글·멘션 CRUD API만 모은다. 댓글 생성·목록 조회·수정·삭제·완료 토글 다섯 API를 모두 문서화한다.

---

## 업무 댓글 생성

### Endpoint

`POST /api/workspaces/{workspaceId}/tasks/{taskId}/comments`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 댓글을 생성할 업무가 속한 워크스페이스 번호 |
| `taskId` | 댓글을 생성할 업무 번호 |

### Request Parameter

없음

Request Body

```json
{
  "content": "확인 부탁드립니다",
  "mentionedUserIds": [11, 12]
}
```

| name | 설명 |
| --- | --- |
| `content` | 댓글 내용. 필수. 앞뒤 공백 제거 후 공백만 남으면 거부. |
| `mentionedUserIds` | 멘션할 사용자 번호 목록. 선택(생략 시 빈 목록). 워크스페이스 참여자만 지정할 수 있다. |

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 댓글 생성 성공 |

Response Body

```json
{
  "status": 201,
  "code": "WORKSPACE_201_3",
  "message": "업무 댓글 생성에 성공했습니다.",
  "data": {
    "commentId": 501,
    "taskId": 101,
    "authorId": 10,
    "content": "확인 부탁드립니다",
    "completed": false,
    "completedBy": null,
    "completedAt": null,
    "mentionedUserIds": [11, 12],
    "createdAt": "2026-08-07T10:00:00",
    "updatedAt": "2026-08-07T10:00:00"
  }
}
```

| name | 설명 |
| --- | --- |
| `status` | HTTP 상태 코드 |
| `code` | 서비스 응답 코드 |
| `message` | 응답 메시지 |
| `data.commentId` | 생성된 댓글 번호 |
| `data.taskId` | 소속 업무 번호 |
| `data.authorId` | 작성자 사용자 번호 |
| `data.content` | 댓글 내용 |
| `data.completed` | 완료 여부 |
| `data.completedBy` | 완료 처리자 사용자 번호. 미완료면 `null` |
| `data.completedAt` | 완료 처리 시각. 미완료면 `null` |
| `data.mentionedUserIds` | 멘션된 참여자 번호 목록 |
| `data.createdAt` | 작성 시각 |
| `data.updatedAt` | 수정 시각 |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `content`가 비어있거나(trim 후 공백) 형식이 잘못된 경우 |
| `400 Bad Request` | `WORKSPACE_400_6` | 멘션 대상은 워크스페이스 참여자여야 합니다. | `mentionedUserIds`에 워크스페이스 참여자가 아닌 사용자가 포함된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 해당 워크스페이스의 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |
| `404 Not Found` | `WORKSPACE_404_3` | 업무를 찾을 수 없습니다. | 업무가 없거나 해당 워크스페이스 소속이 아닌 경우 |

실패 예시 (`400 Bad Request`, 비참여자 멘션)

```json
{
  "timestamp": "2026-08-07T12:00:00",
  "status": 400,
  "code": "WORKSPACE_400_6",
  "message": "멘션 대상은 워크스페이스 참여자여야 합니다.",
  "traceId": "a1b2c3d4"
}
```

> 존재 확인(워크스페이스 → 업무)을 참여자 확인·멘션 검증보다 먼저 수행한다.
> 댓글 생성/수정/삭제/완료토글은 참여자 여부만 검증한다(권한 체크 없음, 2026-08-10 확정 — `WORKSPACE:CREATE`는 워크스페이스 생성에만 적용된다).
> **멘션된 사용자(요청자 자신은 제외)에게는 두 가지가 함께 처리된다: ① 실시간 WebSocket 알림(`/topic/workspaces/users/{userId}`) ② 알림 저장.** 저장은 별도 엔드포인트가 없다 — `NotificationCreationListener`가 커밋 후 이벤트를 구독해 자동으로 저장한다. 저장된 알림은 `GET /api/notifications`로 **조회**할 수 있다(저장 자체를 수행하는 API가 아님). 상세는 [COMMENT_API_FLOW.md](COMMENT_API_FLOW.md), 알림함 API는 [notification 모듈 API.md](../../notification/docs/API.md) 참고.

---

## 댓글 목록 조회

### Endpoint

`GET /api/workspaces/{workspaceId}/tasks/{taskId}/comments`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 댓글이 속한 업무의 워크스페이스 번호 |
| `taskId` | 댓글 목록을 조회할 업무 번호 |

### Request Parameter

| name | 설명 |
| --- | --- |
| `page` | 페이지 번호. 0부터 시작. 기본값 `0`. `0` 미만이면 `400`. |
| `size` | 페이지 크기. 기본값 `20`. `1` 미만 또는 `100` 초과면 `400`. |

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 댓글 목록 조회 성공 |

Response Body

```json
{
  "status": 200,
  "code": "WORKSPACE_200_17",
  "message": "댓글 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "commentId": 1,
        "content": "수학A반 완료",
        "author": { "userId": 12, "name": "윤예진" },
        "completed": true,
        "createdAt": "2026-08-01T16:00:00"
      },
      {
        "commentId": 2,
        "content": "영어B반 진행 중 — @정다은 내일까지 취합 도와주세요",
        "author": { "userId": 12, "name": "윤예진" },
        "completed": false,
        "createdAt": "2026-08-02T18:00:00"
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
| `data.content` | 댓글 목록. 생성일 **오름차순**(오래된 댓글 먼저) |
| `data.content[].commentId` | 댓글 번호 |
| `data.content[].content` | 댓글 내용 |
| `data.content[].author` | 작성자. `{ userId, name }`. 이름을 찾지 못하면 `"알 수 없음"` |
| `data.content[].completed` | 완료 여부 |
| `data.content[].createdAt` | 생성일시 |
| `data.page` | 요청한 페이지 번호 |
| `data.size` | 요청한 페이지 크기 |
| `data.hasNext` | 다음 페이지 존재 여부. **전체 개수(`totalElements`)는 제공하지 않는다** — count 쿼리 없는 Slice 방식이라, 프론트는 무한스크롤 시 `hasNext`가 `true`인 동안 `page+1`을 계속 호출하면 된다. |

**완료일시(`completedAt`)와 멘션 목록(`mentionedUserIds`)은 이 응답에 포함하지 않는다** — 요구사항 범위 밖으로 결정됨(생성/수정/완료토글 응답에는 계속 포함된다).

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `page`가 음수, `size`가 0 이하 또는 100 초과인 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |
| `404 Not Found` | `WORKSPACE_404_3` | 업무를 찾을 수 없습니다. | 업무가 없거나 해당 워크스페이스 소속이 아닌 경우 |

> 정렬은 `createdAt asc, id asc`(tie-break) — 동시간대에 여러 댓글이 저장돼도 정렬 순서가 항상 결정적이다.
> 업무 상세 조회 API와 별도 엔드포인트로 분리했다(무한스크롤 대응, 업무 상세 응답의 갱신 주기와 다름). 업무 상세 응답에는 댓글이 포함되지 않는다.

---

## 업무 댓글 수정

### Endpoint

`PATCH /api/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |
| `Content-Type` | `application/json` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 댓글이 속한 업무의 워크스페이스 번호 |
| `taskId` | 댓글이 속한 업무 번호 |
| `commentId` | 수정할 댓글 번호 |

### Request Parameter

없음

Request Body

```json
{
  "content": "확인했습니다",
  "mentionedUserIds": [11]
}
```

| name | 설명 |
| --- | --- |
| `content` | 수정할 댓글 내용. 필수. |
| `mentionedUserIds` | 교체할 멘션 대상 번호 목록. 선택(생략 시 빈 목록). 기존 멘션은 요청 내용으로 전체 교체된다. |

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 댓글 수정 성공 |

Response Body

```json
{
  "status": 200,
  "code": "WORKSPACE_200_7",
  "message": "업무 댓글 수정에 성공했습니다.",
  "data": {
    "commentId": 501,
    "taskId": 101,
    "authorId": 10,
    "content": "확인했습니다",
    "completed": false,
    "completedBy": null,
    "completedAt": null,
    "mentionedUserIds": [11],
    "createdAt": "2026-08-07T10:00:00",
    "updatedAt": "2026-08-07T11:00:00"
  }
}
```

응답 필드는 댓글 생성과 동일하다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `content`가 비어있거나 형식이 잘못된 경우 |
| `400 Bad Request` | `WORKSPACE_400_6` | 멘션 대상은 워크스페이스 참여자여야 합니다. | `mentionedUserIds`에 참여자가 아닌 사용자가 포함된 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |
| `404 Not Found` | `WORKSPACE_404_3` | 업무를 찾을 수 없습니다. | 업무가 없거나 해당 워크스페이스 소속이 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_4` | 업무 댓글을 찾을 수 없습니다. | 댓글이 없거나 해당 업무 소속이 아닌 경우 |

> 작성자 본인 여부는 확인하지 않는다 — 현재 워크스페이스 참여자면 누구나 수정할 수 있다.
> 다른 업무에 속한 댓글 번호를 보내면 존재를 노출하지 않기 위해 `403`이 아니라 `WORKSPACE_404_4`를 반환한다.
> 멘션은 전체 교체 방식이라, **요청자 본인을 제외한, 새로 추가된 멘션 대상에게만** 알림(실시간 WebSocket + 알림 저장)이 간다. 기존에 유지된 멘션과 요청자 본인은 재알림하지 않는다.

---

## 업무 댓글 삭제

### Endpoint

`DELETE /api/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 댓글이 속한 업무의 워크스페이스 번호 |
| `taskId` | 댓글이 속한 업무 번호 |
| `commentId` | 삭제할 댓글 번호 |

### Request Parameter

없음

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 댓글 삭제 성공 |

Response Body

```json
{
  "status": 200,
  "code": "WORKSPACE_200_14",
  "message": "업무 댓글 삭제에 성공했습니다.",
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
| `404 Not Found` | `WORKSPACE_404_4` | 업무 댓글을 찾을 수 없습니다. | 댓글이 없거나 해당 업무 소속이 아닌 경우 |

> 하드 삭제이며 복구할 수 없다. 멘션은 FK `ON DELETE CASCADE`로 함께 삭제된다.
> 작성자 본인 여부는 확인하지 않는다 — 현재 워크스페이스 참여자면 누구나 삭제할 수 있다.

---

## 업무 댓글 완료 토글

### Endpoint

`PATCH /api/workspaces/{workspaceId}/tasks/{taskId}/comments/{commentId}/complete`

### Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {accessToken}` |

### Path Variable

| name | description |
| --- | --- |
| `workspaceId` | 댓글이 속한 업무의 워크스페이스 번호 |
| `taskId` | 댓글이 속한 업무 번호 |
| `commentId` | 완료 상태를 전환할 댓글 번호 |

### Request Parameter

없음

본문 없음.

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 완료 상태 변경 성공 |

Response Body (완료 처리 시)

```json
{
  "status": 200,
  "code": "WORKSPACE_200_8",
  "message": "업무 댓글 완료 상태 변경에 성공했습니다.",
  "data": {
    "commentId": 501,
    "taskId": 101,
    "authorId": 10,
    "content": "확인 부탁드립니다",
    "completed": true,
    "completedBy": 11,
    "completedAt": "2026-08-07T11:00:00",
    "mentionedUserIds": [],
    "createdAt": "2026-08-07T10:00:00",
    "updatedAt": "2026-08-07T11:00:00"
  }
}
```

응답 필드는 댓글 생성과 동일하다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | Access Token이 없거나 유효하지 않은 경우 |
| `403 Forbidden` | `WORKSPACE_403_1` | 워크스페이스에 접근할 권한이 없습니다. | 요청자가 참여자가 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_1` | 워크스페이스를 찾을 수 없습니다. | 워크스페이스가 없거나 삭제된 경우 |
| `404 Not Found` | `WORKSPACE_404_3` | 업무를 찾을 수 없습니다. | 업무가 없거나 해당 워크스페이스 소속이 아닌 경우 |
| `404 Not Found` | `WORKSPACE_404_4` | 업무 댓글을 찾을 수 없습니다. | 댓글이 없거나 해당 업무 소속이 아닌 경우 |

> 호출마다 완료↔취소를 반전하는 토글 방식이다. 완료 처리자(`completedBy`)는 마지막으로 토글한 참여자로 갱신되며 작성자와 무관하다.
> 취소로 전환되면 `completedBy`·`completedAt`은 `null`로 초기화된다.
> 작성자 본인 여부는 확인하지 않는다 — 현재 워크스페이스 참여자면 누구나 전환할 수 있다.

---

## 참고 문서

- [COMMENT_API_FLOW.md](COMMENT_API_FLOW.md) — 댓글·멘션 CRUD API의 호출 흐름 상세
- [TASK_API.md](TASK_API.md) — 업무(Task) 자체의 생성·수정·삭제 API 명세
- [WORKSPACE_PERMISSIONS.md](WORKSPACE_PERMISSIONS.md) — 참여자 기반 권한 검증 정책과 `WORKSPACE:CREATE` TODO 현황
- [notification 모듈 API.md](../../notification/docs/API.md) — 멘션·결재 알림이 저장되는 알림함 조회/읽음처리/삭제 API 명세
