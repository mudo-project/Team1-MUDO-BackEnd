# 메신저(Messenger) API 명세서

> REST 섹션(1~13)의 각 `## `이 Notion 하위 페이지 1개(엔드포인트 1개)에 대응합니다. WebSocket 섹션(14)은 이벤트 8종이 destination 하나로 멀티플렉스되어 하위 페이지 1개로 관리합니다.
> 공통 성공 응답 포맷: `{ "status", "code", "message", "data" }` (`GlobalApiResponse`). `204 No Content`는 본문 없음.
> 공통 실패 응답 포맷: `{ "timestamp", "status", "code", "message", "traceId", "details" }`
> 모든 API는 `Authorization: Bearer {AccessToken}` 헤더가 필요합니다 (미인증 시 `401 COMMON_401_1`).
> `{roomId}`가 경로에 있는 API는 요청자가 해당 채팅방 멤버여야 합니다(아니면 `403 MESSENGER_403_1`), 방 자체가 없으면 `404 MESSENGER_404_1`.

---

## 1. 채팅방 생성

`POST /api/messenger/rooms`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Body
```json
{
  "participantIds": [3, 4],
  "name": null
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `participantIds` | `List<Long>` | `true` | 초대할 사용자 ID 목록(본인 포함 여부 무관, 서버가 본인은 제외 처리). 최소 1개 이상, 각 값은 양수. |
| `name` | `String` | `false` | 채팅방 이름. 참여자가 본인 제외 2명 이상(그룹)이면 필수. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 채팅방 생성 성공 |

Response Body
```json
{
  "status": 201,
  "code": "MESSENGER_201_1",
  "message": "채팅방 생성에 성공했습니다.",
  "data": { "chatRoomId": 1 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data.chatRoomId` | 생성되었거나(또는 이미 존재해서 재사용된) 채팅방 ID입니다. |

> 참고: 참여자가 본인 제외 1명(DM)이고 이미 그 상대와 만든 DM방이 있으면, 새로 만들지 않고 기존 `chatRoomId`를 그대로 반환합니다. 이때도 HTTP 상태는 `201`로 고정됩니다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `participantIds` 비어있음/null 원소/0 이하 값 (Bean Validation) |
| `400 Bad Request` | `MESSENGER_400_3` | 본인 외에 최소 1명 이상 초대해야 합니다. | `participantIds`에서 본인을 제외하면 초대 대상이 없음 |
| `400 Bad Request` | `MESSENGER_400_4` | 그룹 채팅방은 이름을 지정해야 합니다. | 본인 제외 참여자가 2명 이상인데 `name`이 비어있음 |
| `400 Bad Request` | `MESSENGER_400_7` | 존재하지 않는 참여자가 포함되어 있습니다. | `participantIds`에 존재하지 않는 사용자 ID 포함 |
| `400 Bad Request` | `MESSENGER_400_8` | 다른 학원 소속 사용자는 초대할 수 없습니다. | 초대 대상 중 요청자와 다른 학원 소속 사용자 포함 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `404 Not Found` | `MESSENGER_404_2` | 사용자를 찾을 수 없습니다. | 요청자 본인 정보를 사용자 디렉토리에서 찾을 수 없음(비정상 상황) |

---

## 2. 채팅방 목록조회

`GET /api/messenger/rooms`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "MESSENGER_200_1",
  "message": "채팅방 목록 조회에 성공했습니다.",
  "data": [
    {
      "id": 1,
      "name": "김민수",
      "type": "DM",
      "unreadCount": 2,
      "lastMessagePreview": "네 알겠습니다",
      "lastMessageAt": "2026-08-06T09:10:00",
      "createdAt": "2026-08-01T10:00:00"
    }
  ]
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data[].id` | 채팅방 ID입니다. |
| `data[].name` | 채팅방 이름입니다. DM이면 상대방 이름, 그룹이면 방 이름입니다. |
| `data[].type` | `DM` 또는 `GROUP`입니다. |
| `data[].unreadCount` | 요청자 기준 안읽은 메시지 수입니다. |
| `data[].lastMessagePreview` | 최근 메시지 미리보기입니다. TEXT는 내용, IMAGE/FILE은 "사진을 보냈습니다."/"파일을 보냈습니다.", 메시지가 없으면 `null`입니다. |
| `data[].lastMessageAt` | 최근 메시지 시각입니다. 메시지가 없으면 `null`입니다. |
| `data[].createdAt` | 채팅방 생성 시각입니다. |

> 정렬: `lastMessageAt`(없으면 `createdAt`) 내림차순입니다. 페이지네이션 없이 전체 목록을 반환합니다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `404 Not Found` | `MESSENGER_404_2` | 사용자를 찾을 수 없습니다. | 요청자 본인 정보를 사용자 디렉토리에서 찾을 수 없음(비정상 상황) |

---

## 3. 채팅방 참여자 목록조회

`GET /api/messenger/rooms/{roomId}/members`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 조회할 채팅방의 ID입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 참여자 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "MESSENGER_200_2",
  "message": "채팅방 참여자 조회에 성공했습니다.",
  "data": [
    { "userId": 2, "name": "이지훈", "lastReadAt": "2026-08-06T09:00:00" }
  ]
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data[].userId` | 참여자 사용자 ID입니다. |
| `data[].name` | 참여자 이름입니다. |
| `data[].lastReadAt` | 해당 참여자가 마지막으로 읽음 처리한 시각입니다. 아직 읽은 적 없으면 `null`입니다. |

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 요청자가 해당 방 멤버가 아님 |
| `404 Not Found` | `MESSENGER_404_1` | 채팅방을 찾을 수 없습니다. | `roomId`에 해당하는 방이 없음 |

---

## 4. 메시지 전송

`POST /api/messenger/rooms/{roomId}/messages`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 메시지를 보낼 채팅방의 ID입니다. |

Request Body
```json
{
  "messageType": "TEXT",
  "content": "안녕하세요",
  "fileId": null,
  "fileName": null
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `messageType` | `String` | `true` | `TEXT`, `IMAGE`, `FILE` 중 하나. |
| `content` | `String` | `messageType=TEXT`일 때 `true` | 메시지 내용. |
| `fileId` | `Long` | `messageType=IMAGE/FILE`일 때 `true` | 아래 "파일 첨부 사전 절차"로 미리 발급받은 파일 ID. |
| `fileName` | `String` | `false` | 원본 파일명. |

> **⚠️ 2026-08-10 변경**: 이전엔 프론트가 파일 URL(`fileUrl`)을 직접 채워서 보내는 방식이었으나, 그 URL을 발급하는 API 자체가 없어 실제로는 채울 수 없는 값이었다. 그래서 approval/notice와 동일하게 공용 `file` 모듈에서 발급받는 `fileId` 참조 방식으로 변경했다. 이 문서는 그동안 갱신이 안 된 채 옛 `fileUrl` 방식으로 남아있었다 — IMAGE/FILE 메시지를 보내려면 반드시 아래 절차를 먼저 거쳐야 한다.

### 파일 첨부 사전 절차 (messageType=IMAGE/FILE일 때 필수)

메신저 도메인이 아니라 **공용 `file` 모듈**(`/api/files`, 결재/공지 등 다른 도메인도 공유)에서 처리한다. 순서:

**1) 업로드용 presigned URL 발급**

`POST /api/files/presigned-url`

Request Body
```json
{ "fileName": "사진.jpg", "contentType": "image/jpeg" }
```

Response Body (`200 OK`)
```json
{
  "status": 200,
  "code": "FILE_200_1",
  "message": "presigned URL 발급에 성공했습니다.",
  "data": { "objectKey": "tenants/academy-a/files/3f2c-사진.jpg", "uploadUrl": "https://..." }
}
```

**2) 클라이언트가 `uploadUrl`로 S3에 파일을 직접 `PUT` 업로드** (백엔드를 거치지 않음, `Content-Type` 헤더를 1)에서 보낸 `contentType`과 동일하게 지정)

**3) 업로드 완료 후 파일 메타데이터 등록**

`POST /api/files`

Request Body
```json
{ "objectKey": "tenants/academy-a/files/3f2c-사진.jpg", "contentType": "image/jpeg" }
```

Response Body (`201 Created`)
```json
{
  "status": 201,
  "code": "FILE_201_1",
  "message": "파일 등록에 성공했습니다.",
  "data": { "fileId": 42 }
}
```

**4) 발급받은 `fileId`로 메시지 전송** (위 4번 API에 `messageType: "IMAGE"` 또는 `"FILE"`, `fileId: 42`로 요청)

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 메시지 전송 성공 |

Response Body
```json
{
  "status": 201,
  "code": "MESSENGER_201_2",
  "message": "메시지 전송에 성공했습니다.",
  "data": { "messageId": 10 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data.messageId` | 생성된 메시지 ID입니다. |

> 전송 성공 시 room topic(`/topic/messenger/rooms/{roomId}`)에 `MESSAGE_SENT` 이벤트가 실시간 브로드캐스트됩니다(발신자 본인 포함). 상세 페이로드는 11번 WebSocket 섹션 참고.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `messageType` 누락/유효하지 않은 값 (Bean Validation) |
| `400 Bad Request` | `MESSENGER_400_5` | 메시지 내용은 비어 있을 수 없습니다. | `messageType=TEXT`인데 `content`가 비어있음 |
| `400 Bad Request` | `MESSENGER_400_6` | 첨부파일(fileId)이 지정되지 않았습니다. | `messageType=IMAGE/FILE`인데 `fileId`가 비어있음 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 요청자가 해당 방 멤버가 아님 |
| `404 Not Found` | `MESSENGER_404_1` | 채팅방을 찾을 수 없습니다. | `roomId`에 해당하는 방이 없음 |

---

## 5. 메시지 목록조회

`GET /api/messenger/rooms/{roomId}/messages`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 조회할 채팅방의 ID입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `cursorCreatedAt` | `LocalDateTime` | `false` | 이전 페이지 마지막 메시지의 생성 시각. `cursorMessageId`와 함께 전달. |
| `cursorMessageId` | `Long` | `false` | 이전 페이지 마지막 메시지 ID. `cursorCreatedAt`과 함께 전달. |
| `size` | `Integer` | `false` | 페이지 크기(1~100, 기본값 20). |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "MESSENGER_200_3",
  "message": "메시지 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 10,
        "senderId": 2,
        "senderName": "이지훈",
        "messageType": "TEXT",
        "content": "안녕하세요",
        "fileId": null,
        "fileDownloadUrl": null,
        "fileName": null,
        "createdAt": "2026-08-06T09:00:00",
        "editedAt": null,
        "deletedAt": null,
        "deleted": false,
        "unreadCount": 1
      }
    ],
    "hasNext": false,
    "nextCursorCreatedAt": null,
    "nextCursorMessageId": null
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data.content[].id` | 메시지 ID입니다. |
| `data.content[].senderId` / `senderName` | 발신자 정보입니다. |
| `data.content[].messageType` | `TEXT`/`IMAGE`/`FILE`입니다. |
| `data.content[].content` / `fileId` / `fileDownloadUrl` / `fileName` | 삭제된 메시지면 전부 `null`로 내려갑니다. `fileDownloadUrl`은 1시간짜리 presigned URL이라 만료 후엔 `GET /api/files/{fileId}/download-url`로 재조회해야 합니다. |
| `data.content[].createdAt` / `editedAt` / `deletedAt` | 생성/수정/삭제 시각입니다. |
| `data.content[].deleted` | 삭제 여부입니다. |
| `data.content[].unreadCount` | 해당 메시지를 아직 안 읽은 방 멤버 수입니다(카톡 스타일 숫자). |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |
| `data.nextCursorCreatedAt` / `nextCursorMessageId` | 다음 페이지 조회 시 넘길 cursor 값입니다. `hasNext=false`면 `null`입니다. |

> `cursor`가 없는 첫 페이지 조회일 때만 요청자의 읽음 처리(`lastReadAt` 갱신)가 함께 일어나고, room topic에 `MESSAGE_READ` 이벤트가 브로드캐스트됩니다. 과거 페이지 스크롤(cursor 있음)은 읽음 처리하지 않습니다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `MESSENGER_400_11` | cursorCreatedAt과 cursorMessageId는 함께 전달하거나 함께 생략해야 합니다. | 둘 중 하나만 전달됨 |
| `400 Bad Request` | `MESSENGER_400_12` | 메시지 조회 size는 1 이상 100 이하여야 합니다. | `size`가 범위를 벗어남(주로 Bean Validation `COMMON_400_1`이 먼저 걸리며, 이 코드는 서비스 레이어 방어용) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 요청자가 해당 방 멤버가 아님 |
| `404 Not Found` | `MESSENGER_404_1` | 채팅방을 찾을 수 없습니다. | `roomId`에 해당하는 방이 없음 |

---

## 6. 메시지 수정

`PATCH /api/messenger/rooms/{roomId}/messages/{messageId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 채팅방 ID입니다. |
| `messageId` | 수정할 메시지 ID입니다. |

Request Body
```json
{
  "content": "수정된 내용입니다"
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | `true` | 수정할 내용. 공백 불가. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 수정 성공 (응답 본문 없음) |

> 성공 시 room topic에 `MESSAGE_EDITED` 이벤트가 실시간 브로드캐스트됩니다(발신자 본인 포함). 상세 페이로드는 13번 WebSocket 섹션 참고.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `content` 공백/누락 (Bean Validation) |
| `400 Bad Request` | `MESSENGER_400_14` | TEXT 메시지만 수정할 수 있습니다. | IMAGE/FILE 메시지를 수정하려는 경우 |
| `400 Bad Request` | `MESSENGER_400_15` | 이미 삭제된 메시지입니다. | 이미 삭제된 메시지를 수정하려는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 본인이 보낸 메시지가 아닌 경우 |
| `404 Not Found` | `MESSENGER_404_1` | 채팅방을 찾을 수 없습니다. | `messageId`가 없거나, 있어도 `roomId`와 다른 방의 메시지인 경우 |

---

## 7. 메시지 삭제

`DELETE /api/messenger/rooms/{roomId}/messages/{messageId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 채팅방 ID입니다. |
| `messageId` | 삭제할 메시지 ID입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 삭제 성공 (응답 본문 없음, 소프트 삭제) |

> 성공 시 room topic에 `MESSAGE_DELETED` 이벤트가 실시간 브로드캐스트됩니다(발신자 본인 포함). 상세 페이로드는 13번 WebSocket 섹션 참고.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 본인이 보낸 메시지가 아닌 경우 |
| `404 Not Found` | `MESSENGER_404_1` | 채팅방을 찾을 수 없습니다. | `messageId`가 없거나, 있어도 `roomId`와 다른 방의 메시지인 경우 |

> 이미 삭제된 메시지를 다시 삭제 요청하면 에러 없이 조용히 성공(204) 처리됩니다(삭제 시각이 덮어써지지 않는 단조성 보장).

---

## 8. 업무지시 카드 등록

`POST /api/messenger/rooms/{roomId}/task-cards`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 카드를 등록할 채팅방 ID입니다. |

Request Body
```json
{
  "content": "과제 제출",
  "dueDate": "2026-08-10",
  "assigneeIds": [3, 4]
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | `true` | 업무지시 내용. 공백 불가. |
| `dueDate` | `LocalDate` | `false` | 마감일. |
| `assigneeIds` | `List<Long>` | `true` | 담당자 ID 목록. 최소 1개 이상, 각 값은 양수, 반드시 해당 채팅방 멤버여야 함. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `201 Created` | 카드 등록 성공 |

Response Body
```json
{
  "status": 201,
  "code": "MESSENGER_201_3",
  "message": "업무지시 카드 등록에 성공했습니다.",
  "data": { "cardId": 7 }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data.cardId` | 생성된 업무지시 카드 ID입니다. |

> 등록 성공 시 room topic에 `TASK_CARD_CREATED` 이벤트가 실시간 브로드캐스트됩니다(등록자 본인 포함).

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `content` 공백/누락, `assigneeIds` 비어있음/null 원소/0 이하 값 (Bean Validation) |
| `400 Bad Request` | `MESSENGER_400_13` | 유효하지 않은 담당자가 포함되어 있습니다. | `assigneeIds`에 존재하지 않는 사용자 포함 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 요청자 또는 담당자 중 해당 방 멤버가 아닌 사람이 있음 |
| `404 Not Found` | `MESSENGER_404_1` | 채팅방을 찾을 수 없습니다. | `roomId`에 해당하는 방이 없음 |

---

## 9. 업무지시 카드 목록조회

`GET /api/messenger/rooms/{roomId}/task-cards`

> 2026-08-07: 페이지네이션 없이 방의 카드를 전부 반환하던 방식이 카드가 쌓일수록 응답이 무한히 커지는 문제(부하테스트로 1,000건 기준 374KB 확인)가 있어, 메시지 목록조회와 동일한 cursor 페이지네이션으로 변경했다.

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 조회할 채팅방 ID입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `cursorCreatedAt` | `LocalDateTime` | `false` | 이전 페이지 마지막 카드의 등록 시각. `cursorCardId`와 함께 전달. |
| `cursorCardId` | `Long` | `false` | 이전 페이지 마지막 카드 ID. `cursorCreatedAt`과 함께 전달. |
| `size` | `Integer` | `false` | 페이지 크기(1~100, 기본값 20). |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "MESSENGER_200_4",
  "message": "업무지시 카드 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 7,
        "chatRoomId": 1,
        "assignerId": 2,
        "assignerName": "이지훈",
        "content": "과제 제출",
        "dueDate": "2026-08-10",
        "assignees": [
          { "userId": 3, "name": "박서연", "completedAt": null },
          { "userId": 4, "name": "김도윤", "completedAt": "2026-08-06T09:30:00" }
        ],
        "completedCount": 1,
        "assigneeCount": 2,
        "fullyCompleted": false,
        "createdAt": "2026-08-06T09:00:00"
      }
    ],
    "hasNext": false,
    "nextCursorCreatedAt": null,
    "nextCursorCardId": null
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data.content[].id` | 업무지시 카드 ID입니다. |
| `data.content[].chatRoomId` | 카드가 속한 채팅방 ID입니다(이 API는 경로에 이미 `roomId`가 있어 항상 그 값과 같습니다. 13번 "내 업무지시 카드 목록조회"처럼 여러 방을 가로지르는 조회에서 방 구분용으로 쓰입니다). |
| `data.content[].assignerId` / `assignerName` | 등록자 정보입니다. |
| `data.content[].content` / `dueDate` | 업무지시 내용/마감일입니다. |
| `data.content[].assignees[].userId` / `name` / `completedAt` | 담당자별 완료 시각입니다. 미완료면 `null`입니다. |
| `data.content[].completedCount` / `assigneeCount` | 완료 인원 / 전체 담당자 수입니다. |
| `data.content[].fullyCompleted` | 담당자 전원 완료 여부입니다. |
| `data.content[].createdAt` | 카드 등록 시각입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |
| `data.nextCursorCreatedAt` / `nextCursorCardId` | 다음 페이지 조회 시 넘길 cursor 값입니다. `hasNext=false`면 `null`입니다. |

> 정렬: 등록 시각(`createdAt`) 내림차순(최신순, 동일 시각이면 ID 내림차순)입니다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `MESSENGER_400_17` | cursorCreatedAt과 cursorCardId는 함께 전달하거나 함께 생략해야 합니다. | 둘 중 하나만 전달됨 |
| `400 Bad Request` | `MESSENGER_400_18` | 업무지시 카드 조회 size는 1 이상 100 이하여야 합니다. | `size`가 범위를 벗어남(주로 Bean Validation `COMMON_400_1`이 먼저 걸리며, 이 코드는 서비스 레이어 방어용) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 요청자가 해당 방 멤버가 아님 |
| `404 Not Found` | `MESSENGER_404_1` | 채팅방을 찾을 수 없습니다. | `roomId`에 해당하는 방이 없음 |

---

## 10. 업무지시 완료 처리

`PATCH /api/messenger/rooms/{roomId}/task-cards/{cardId}/complete`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 채팅방 ID입니다. |
| `cardId` | 완료 처리할 업무지시 카드 ID입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 완료 처리 성공 (응답 본문 없음) |

> 성공 시 room topic에 `TASK_CARD_COMPLETED` 이벤트가 실시간 브로드캐스트됩니다(완료 처리자 본인 포함). 이벤트에는 `completedCount`/`assigneeCount`/`fullyCompleted`가 포함되어 프론트가 재조회 없이 progress bar를 갱신할 수 있습니다.

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_2` | 해당 업무지시의 담당자가 아닙니다. | 요청자가 그 카드의 담당자로 지정되지 않음 |
| `404 Not Found` | `MESSENGER_404_3` | 업무지시 카드를 찾을 수 없습니다. | `cardId`가 없거나, 있어도 `roomId`와 다른 방의 카드인 경우 |

> 이미 완료 처리한 담당자가 다시 호출해도 에러 없이 성공(204) 처리됩니다(완료 시각이 덮어써지지 않는 단조성 보장).

---

## 11. 업무지시 카드 수정

`PATCH /api/messenger/rooms/{roomId}/task-cards/{cardId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 채팅방 ID입니다. |
| `cardId` | 수정할 업무지시 카드 ID입니다. |

Request Body
```json
{
  "content": "과제 제출(마감 연장)",
  "dueDate": "2026-08-20",
  "assigneeIds": [4, 5]
}
```
| name | type | required | 설명 |
| --- | --- | --- | --- |
| `content` | `String` | `true` | 업무지시 내용. 공백 불가. |
| `dueDate` | `LocalDate` | `false` | 마감일. |
| `assigneeIds` | `List<Long>` | `true` | 담당자 ID 목록(전체 교체). 최소 1개, 각 값 양수, 반드시 방 멤버여야 함. 유지되는 담당자의 완료 기록은 그대로 보존됩니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 수정 성공 (응답 본문 없음) |

> 성공 시 room topic에 `TASK_CARD_UPDATED` 이벤트가 실시간 브로드캐스트됩니다(등록자 본인 포함).

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `content` 공백/누락, `assigneeIds` 비어있음/null/0 이하 (Bean Validation) |
| `400 Bad Request` | `MESSENGER_400_13` | 유효하지 않은 담당자가 포함되어 있습니다. | `assigneeIds`에 존재하지 않는 사용자 포함 |
| `400 Bad Request` | `MESSENGER_400_16` | 이미 삭제된 업무지시입니다. | 삭제된 카드를 수정하려는 경우 |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_1` | 채팅방 참여자가 아닙니다. | 새로 지정한 담당자 중 방 멤버가 아닌 사람이 있음 |
| `403 Forbidden` | `MESSENGER_403_3` | 본인이 등록한 업무지시만 수정/삭제할 수 있습니다. | 등록자가 아닌 사람이 수정하려는 경우 |
| `404 Not Found` | `MESSENGER_404_3` | 업무지시 카드를 찾을 수 없습니다. | `cardId`가 없거나, 있어도 `roomId`와 다른 방의 카드인 경우 |

---

## 12. 업무지시 카드 삭제

`DELETE /api/messenger/rooms/{roomId}/task-cards/{cardId}`

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Parameter

| name | description |
| --- | --- |
| `roomId` | 채팅방 ID입니다. |
| `cardId` | 삭제할 업무지시 카드 ID입니다. |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `204 No Content` | 삭제 성공 (응답 본문 없음, 소프트 삭제) |

> 성공 시 room topic에 `TASK_CARD_DELETED` 이벤트가 실시간 브로드캐스트됩니다(등록자 본인 포함).

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |
| `403 Forbidden` | `MESSENGER_403_3` | 본인이 등록한 업무지시만 수정/삭제할 수 있습니다. | 등록자가 아닌 사람이 삭제하려는 경우 |
| `404 Not Found` | `MESSENGER_404_3` | 업무지시 카드를 찾을 수 없습니다. | `cardId`가 없거나, 있어도 `roomId`와 다른 방의 카드인 경우 |

> 이미 삭제된 카드를 다시 삭제 요청해도 에러 없이 204 처리됩니다(삭제 시각 덮어쓰기 없음, 중복 브로드캐스트도 안 나감).

---

## 13. 내 업무지시 카드 목록조회

`GET /api/messenger/task-cards`

> 2026-08-17 추가: 9번(방별 업무지시 카드 목록조회)과 달리 `roomId` 없이, 요청자가 참여 중인 모든 채팅방을 가로질러 `role`에 따라 내가 전달한/받은 업무지시 카드를 모아 조회합니다. 프론트가 방마다 9번 API를 호출해 클라이언트에서 합치고 필터링하던 방식(N+1, 필터 누락 위험)을 대체합니다.

# **[request]**

Request Header

| name | description |
| --- | --- |
| `Authorization` | `Bearer {AccessToken}` 형식의 사용자 인증 토큰입니다. |

Request Query Parameter

| name | type | required | description |
| --- | --- | --- | --- |
| `role` | `String` | `true` | `SENT`(내가 전달한 업무) 또는 `RECEIVED`(내가 받은 업무). |
| `cursorCreatedAt` | `LocalDateTime` | `false` | 이전 페이지 마지막 카드의 등록 시각. `cursorCardId`와 함께 전달. |
| `cursorCardId` | `Long` | `false` | 이전 페이지 마지막 카드 ID. `cursorCreatedAt`과 함께 전달. |
| `size` | `Integer` | `false` | 페이지 크기(1~100, 기본값 20). |

# **[response]**

### 성공코드

| HTTP 상태 | 설명 |
| --- | --- |
| `200 OK` | 목록 조회 성공 |

Response Body
```json
{
  "status": 200,
  "code": "MESSENGER_200_5",
  "message": "내 업무지시 카드 목록 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "id": 7,
        "chatRoomId": 1,
        "assignerId": 2,
        "assignerName": "이지훈",
        "content": "과제 제출",
        "dueDate": "2026-08-10",
        "assignees": [
          { "userId": 3, "name": "박서연", "completedAt": null },
          { "userId": 4, "name": "김도윤", "completedAt": "2026-08-06T09:30:00" }
        ],
        "completedCount": 1,
        "assigneeCount": 2,
        "fullyCompleted": false,
        "createdAt": "2026-08-06T09:00:00"
      }
    ],
    "hasNext": false,
    "nextCursorCreatedAt": null,
    "nextCursorCardId": null
  }
}
```
### Response Field

| name | 설명 |
| --- | --- |
| `data.content[].id` | 업무지시 카드 ID입니다. |
| `data.content[].chatRoomId` | 카드가 속한 채팅방 ID입니다. 여러 방을 가로지르는 조회라 프론트가 어느 방의 카드인지 구분(예: 클릭 시 해당 방으로 이동)하는 데 필요합니다. |
| `data.content[].assignerId` / `assignerName` | 등록자 정보입니다. |
| `data.content[].content` / `dueDate` | 업무지시 내용/마감일입니다. |
| `data.content[].assignees[].userId` / `name` / `completedAt` | 담당자별 완료 시각입니다. 미완료면 `null`입니다. |
| `data.content[].completedCount` / `assigneeCount` | 완료 인원 / 전체 담당자 수입니다. |
| `data.content[].fullyCompleted` | 담당자 전원 완료 여부입니다. |
| `data.content[].createdAt` | 카드 등록 시각입니다. |
| `data.hasNext` | 다음 페이지 존재 여부입니다. |
| `data.nextCursorCreatedAt` / `nextCursorCardId` | 다음 페이지 조회 시 넘길 cursor 값입니다. `hasNext=false`면 `null`입니다. |

> 정렬: 등록 시각(`createdAt`) 내림차순(최신순, 동일 시각이면 ID 내림차순)입니다. 이 API는 특정 방 멤버 여부를 검증하지 않습니다(요청자 본인 기준으로만 필터링하므로 `roomId` 자체가 없음).

### 실패 코드

| HTTP 상태 | code | message | 설명 |
| --- | --- | --- | --- |
| `400 Bad Request` | `COMMON_400_1` | 입력값이 올바르지 않습니다. | `role` 누락/유효하지 않은 값(Bean Validation) |
| `400 Bad Request` | `MESSENGER_400_17` | cursorCreatedAt과 cursorCardId는 함께 전달하거나 함께 생략해야 합니다. | 둘 중 하나만 전달됨 |
| `400 Bad Request` | `MESSENGER_400_18` | 업무지시 카드 조회 size는 1 이상 100 이하여야 합니다. | `size`가 범위를 벗어남(주로 Bean Validation `COMMON_400_1`이 먼저 걸리며, 이 코드는 서비스 레이어 방어용) |
| `401 Unauthorized` | `COMMON_401_1` | 인증이 필요합니다. | 토큰 누락/만료 |

---

## 14. 실시간 이벤트 수신 (WebSocket)

REST와 달리 클라이언트가 요청을 보내는 게 아니라 서버가 이벤트를 밀어주는 방식입니다. 이벤트 8종이 destination 하나로 멀티플렉스되어 하위 페이지 1개로 관리합니다.

### WebSocket 연결

| 항목 | 값 |
| --- | --- |
| 연결 엔드포인트 | `/ws` |
| 프로토콜 | STOMP over WebSocket (SockJS fallback) |
| 인증 | httpOnly `accessToken` 쿠키 기반. SockJS 핸드셰이크 HTTP 요청에 브라우저가 자동으로 실어 보내는 쿠키를 `JwtHandshakeInterceptor`가 읽어 세션에 저장하고, `JwtChannelInterceptor`가 STOMP `CONNECT` 시점에 이를 검증합니다. **프론트는 토큰 값을 직접 읽거나 CONNECT 프레임 헤더에 넣을 필요가 없고, SockJS 연결 시 `withCredentials: true`(cross-origin일 때 쿠키 전송에 필요)만 설정하면 됩니다.** 인증 실패 시 SockJS/WebSocket transport 핸드셰이크가 완료될 수 있지만 STOMP `CONNECT`가 거부되며, 클라이언트는 구독할 수 없습니다. |

### 구독 경로

| 항목 | 값 |
| --- | --- |
| 채팅방 이벤트 구독 | `/topic/messenger/rooms/{roomId}` |

> 이 경로 하나로 이벤트 8종류가 다 옵니다. 페이로드의 `eventType` 필드로 분기해서 처리해야 합니다.
> `[publish]` 섹션 없음 — 메시지/업무지시 카드 관련 쓰기는 전부 REST API(1~13번)로 처리합니다. 소켓은 "받기 전용"이며, 발신자/행위자 본인도 자신이 보낸 이벤트를 그대로 수신합니다(echo 방식, 2026-08-06 optimistic UI 결정에 따라 프론트는 자기 자신 echo를 무시하고 REST 응답으로 먼저 반영).
> 유저 단위 알림 채널(`/user/queue/...`)은 없습니다. 방을 구독 중일 때만 실시간 수신됩니다.

### [subscribe] 메시지 전송

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "MESSAGE_SENT",
  "chatRoomId": 1,
  "messageId": 10,
  "senderUserId": 2,
  "messageType": "TEXT",
  "content": "안녕하세요",
  "fileId": null,
  "fileDownloadUrl": null,
  "fileName": null,
  "createdAt": "2026-08-06T09:00:00",
  "unreadCount": 1
}
```

### [subscribe] 읽음 처리

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "MESSAGE_READ",
  "chatRoomId": 1,
  "readerUserId": 2,
  "readAt": "2026-08-06T09:05:00"
}
```

### [subscribe] 메시지 수정

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "MESSAGE_EDITED",
  "chatRoomId": 1,
  "messageId": 10,
  "senderUserId": 2,
  "content": "수정된 내용입니다",
  "editedAt": "2026-08-06T14:00:00"
}
```

### [subscribe] 메시지 삭제

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "MESSAGE_DELETED",
  "chatRoomId": 1,
  "messageId": 10,
  "deleterUserId": 2,
  "deletedAt": "2026-08-06T14:05:00"
}
```

### [subscribe] 업무지시 카드 등록

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "TASK_CARD_CREATED",
  "chatRoomId": 1,
  "cardId": 7,
  "assignerId": 2,
  "content": "과제 제출",
  "dueDate": "2026-08-10",
  "assigneeIds": [3, 4],
  "createdAt": "2026-08-06T09:00:00"
}
```

### [subscribe] 업무지시 완료 처리

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "TASK_CARD_COMPLETED",
  "chatRoomId": 1,
  "cardId": 7,
  "completedUserId": 3,
  "completedAt": "2026-08-06T09:30:00",
  "completedCount": 1,
  "assigneeCount": 2,
  "fullyCompleted": false
}
```

### [subscribe] 업무지시 카드 수정

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "TASK_CARD_UPDATED",
  "chatRoomId": 1,
  "cardId": 7,
  "content": "과제 제출(마감 연장)",
  "dueDate": "2026-08-20",
  "assigneeIds": [4, 5]
}
```

### [subscribe] 업무지시 카드 삭제

**Destination:** `/topic/messenger/rooms/{roomId}`

**Response**
```json
{
  "eventType": "TASK_CARD_DELETED",
  "chatRoomId": 1,
  "cardId": 7,
  "deletedAt": "2026-08-06T15:00:00"
}
```

### 에러 수신

별도 에러 채널 없음. 소켓으로 클라이언트가 요청을 보내는 동작이 없어서(모든 쓰기는 REST), 에러는 각 REST API 호출의 HTTP 응답으로만 옵니다 — 위 1~13번의 "실패 코드" 표를 참고하세요.
