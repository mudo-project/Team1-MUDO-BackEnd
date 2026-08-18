# 워크스페이스 실시간(WebSocket) API

## 연결

- 기존 STOMP 엔드포인트 `/ws`(SockJS)를 그대로 사용한다. 메신저·알림 때문에 이미 연결돼 있다면 **새 연결을 만들 필요 없이** 이 문서의 topic만 추가 구독하면 된다.
- 인증은 REST API와 동일하게 `accessToken` 쿠키 기반이다 — STOMP `CONNECT` 프레임에 별도 헤더를 실을 필요 없다.

## 구독 (Subscribe)

### Topic

`/topic/workspaces/{workspaceId}`

### 인증 및 권한

- 구독 시점에 서버가 요청자가 해당 `workspaceId`의 참여자인지 확인한다.
- 참여자가 아니거나, 존재하지 않는 `workspaceId`이거나, 인증되지 않은 상태면 구독 자체가 거부된다(STOMP `ERROR` 프레임).
- 워크스페이스 화면에 진입할 때 이 topic 하나만 구독하면 되고, 업무·댓글 액션 전부가 여기로 온다(액션별로 topic이 나뉘지 않는다). 화면을 벗어나면 구독을 해제한다.
- 댓글 멘션 알림(`/topic/workspaces/users/{userId}`, 개인 topic)과는 별개다 — 그건 "나를 멘션한 알림"만 오고, 이 topic은 "내가 참여한 워크스페이스의 모든 변경"이 온다.

### 공통 규칙

- **본인이 실행한 변경도 이 topic으로 그대로 돌아온다.** 변경을 요청한 사람도 예외 없이 수신 대상이다 — REST 응답으로 이미 최신 상태를 받은 직후 동일한 내용이 WS로 한 번 더 오는 것뿐이라, 같은 값으로 화면을 다시 갱신해도 문제없다. "내가 한 요청은 무시" 같은 별도 처리는 필요 없다.
- **연결이 끊겼다가 재연결되면, 그 사이 놓친 이벤트는 재전송되지 않는다.** 재연결 시점에 화면을 통째로 다시 조회해서 동기화해야 한다.
- `createdBy`/`authorId`/`completedBy`는 전부 **userId(Long)만** 내려온다. 이름 등 표시용 정보는 담지 않으므로, 이미 갖고 있는 참여자 목록에서 매핑해서 쓴다.
- 날짜/시각 포맷은 REST 응답과 동일하다 — `dueAt`은 `"yyyy-MM-dd"`, `createdAt`/`updatedAt`/`completedAt`은 `"yyyy-MM-ddTHH:mm:ss"`.
- 페이로드는 매번 **바뀐 리소스 하나(업무 1건 또는 댓글 1건)의 전체 데이터**다 — 목록 전체를 다시 보내는 게 아니다. 프론트가 이미 들고 있는 목록/캐시에서 해당 id만 찾아 교체(또는 추가/제거)하면 된다.

## 이벤트 종류

`eventType` 필드로 구분한다.

| eventType | 발생 시점 |
| --- | --- |
| `TASK_CREATED` | 업무 생성 |
| `TASK_UPDATED` | 업무 상태 또는 마감일 변경(둘 중 하나만 바꿔도 발행) |
| `TASK_DELETED` | 업무 삭제 |
| `COMMENT_CREATED` | 댓글 생성 |
| `COMMENT_UPDATED` | 댓글 내용 수정 |
| `COMMENT_TOGGLED` | 댓글 완료/완료해제 토글 |
| `COMMENT_DELETED` | 댓글 삭제 |

## 이벤트별 페이로드

### `TASK_CREATED`

```json
{
  "eventType": "TASK_CREATED",
  "workspaceId": 2,
  "taskId": 501,
  "title": "새 업무",
  "status": "WAITING",
  "dueAt": "2026-09-01",
  "createdBy": 10,
  "createdAt": "2026-08-18T10:00:00"
}
```

| name | type | description |
| --- | --- | --- |
| `workspaceId` | Long | 워크스페이스 번호 |
| `taskId` | Long | 생성된 업무 번호 |
| `title` | String | 업무 제목 |
| `status` | String | `WAITING`\|`IN_PROGRESS`\|`COMPLETED`\|`DELAYED` |
| `dueAt` | String(date) | 마감일 |
| `createdBy` | Long | 생성자 userId |
| `createdAt` | String(datetime) | 생성 시각 |

### `TASK_UPDATED`

```json
{
  "eventType": "TASK_UPDATED",
  "workspaceId": 2,
  "taskId": 501,
  "status": "COMPLETED",
  "dueAt": null
}
```

| name | type | description |
| --- | --- | --- |
| `workspaceId` | Long | 워크스페이스 번호 |
| `taskId` | Long | 업무 번호 |
| `status` | String | 변경 후 상태 |
| `dueAt` | String(date)\|null | 변경 후 마감일. 반복 업무는 마감일을 쓰지 않아 항상 `null` |

`title`은 포함하지 않는다 — 이 이벤트가 다루는 PATCH 대상은 상태/마감일뿐이라 제목은 안 바뀐다.

### `TASK_DELETED`

```json
{ "eventType": "TASK_DELETED", "workspaceId": 2, "taskId": 501 }
```

| name | type | description |
| --- | --- | --- |
| `workspaceId` | Long | 워크스페이스 번호 |
| `taskId` | Long | 삭제된 업무 번호 |

### `COMMENT_CREATED`

```json
{
  "eventType": "COMMENT_CREATED",
  "workspaceId": 2,
  "taskId": 501,
  "commentId": 88,
  "authorId": 10,
  "content": "댓글 내용",
  "createdAt": "2026-08-18T11:00:00"
}
```

| name | type | description |
| --- | --- | --- |
| `workspaceId` | Long | 워크스페이스 번호 |
| `taskId` | Long | 댓글이 달린 업무 번호 |
| `commentId` | Long | 생성된 댓글 번호 |
| `authorId` | Long | 작성자 userId |
| `content` | String | 댓글 내용 |
| `createdAt` | String(datetime) | 생성 시각 |

멘션 대상 목록은 포함하지 않는다 — 멘션된 사용자에게는 별도로 `/topic/workspaces/users/{userId}` 개인 topic으로 `TASK_COMMENT_MENTIONED` 알림이 온다(이 문서 범위 밖, 기존 멘션 알림 기능).

### `COMMENT_UPDATED`

```json
{
  "eventType": "COMMENT_UPDATED",
  "workspaceId": 2,
  "taskId": 501,
  "commentId": 88,
  "content": "수정된 내용",
  "updatedAt": "2026-08-18T12:00:00"
}
```

| name | type | description |
| --- | --- | --- |
| `workspaceId` | Long | 워크스페이스 번호 |
| `taskId` | Long | 업무 번호 |
| `commentId` | Long | 수정된 댓글 번호 |
| `content` | String | 수정 후 내용 |
| `updatedAt` | String(datetime) | 수정 시각 |

### `COMMENT_TOGGLED`

```json
{
  "eventType": "COMMENT_TOGGLED",
  "workspaceId": 2,
  "taskId": 501,
  "commentId": 88,
  "completed": true,
  "completedBy": 10,
  "completedAt": "2026-08-18T13:00:00"
}
```

| name | type | description |
| --- | --- | --- |
| `workspaceId` | Long | 워크스페이스 번호 |
| `taskId` | Long | 업무 번호 |
| `commentId` | Long | 댓글 번호 |
| `completed` | boolean | 토글 후 완료 여부 |
| `completedBy` | Long\|null | 완료 처리한 userId. `completed=false`(완료 해제)면 `null` |
| `completedAt` | String(datetime)\|null | 완료 처리 시각. `completed=false`면 `null` |

### `COMMENT_DELETED`

```json
{ "eventType": "COMMENT_DELETED", "workspaceId": 2, "taskId": 501, "commentId": 88 }
```

| name | type | description |
| --- | --- | --- |
| `workspaceId` | Long | 워크스페이스 번호 |
| `taskId` | Long | 업무 번호 |
| `commentId` | Long | 삭제된 댓글 번호 |

## 범위 밖

- 워크스페이스 이름변경, 참여자 추가/제거, 반복 업무 템플릿 CRUD는 이번 범위에 없다(후속 라운드에서 같은 패턴으로 추가 예정).
- 이벤트 유실(전송 실패) 시 재전송/보정 기능은 없다 — 재연결 시 전체 재조회로 프론트가 직접 동기화해야 한다.

관련 구현 근거는 [REVISION.md](REVISION.md)의 `2026-08-18 · 업무·댓글 실시간 브로드캐스트(WebSocket) 추가` 항목을 참고한다.
