# Messenger API

## 2026-08-05 added behavior

- `POST /api/messenger/rooms` reuses an existing DM room when the requester opens a 1:1 chat with the same user again.
- `PATCH /api/messenger/rooms/{roomId}/messages/{messageId}` edits the sender's own TEXT message and returns `204 No Content`.
- `DELETE /api/messenger/rooms/{roomId}/messages/{messageId}` soft-deletes the sender's own message and returns `204 No Content`.
- Message list responses include `editedAt`, `deletedAt`, `deleted`, and `unreadCount`.
- Deleted messages keep their row for cursor stability, but API responses mask `content`, `fileId`, `fileDownloadUrl`, and `fileName`.
- Messenger WebSocket uses the existing STOMP endpoint `/ws`.
- Subscribe to `/topic/messenger/rooms/{roomId}` for `MESSAGE_SENT` and `MESSAGE_READ` events.

## 공통

- 기본 경로: `/api/messenger/rooms`
- 인증: Access Token 필요
- 응답 본문이 있는 성공 응답은 `GlobalApiResponse`를 사용한다.
- 메시지 목록 조회를 제외한 목록 API는 현재 전체 List를 반환한다.

## 채팅방 생성

- Method: `POST`
- Endpoint: `/api/messenger/rooms`
- Body: `participantIds`, `name`
- 규칙: 본인 외 최소 1명을 초대해야 한다. 그룹 채팅은 `name`이 필수다. 초대 대상은 같은 학원의 `ACTIVE` 사용자여야 한다.

## 채팅방 목록 조회

- Method: `GET`
- Endpoint: `/api/messenger/rooms`
- 응답: 내가 참여 중인 채팅방 목록, unread 수, 최근 메시지 미리보기
- 규칙: unread 수는 본인이 보낸 메시지를 제외해 계산한다.

## 참여자 목록 조회

- Method: `GET`
- Endpoint: `/api/messenger/rooms/{roomId}/members`
- 규칙: 요청자가 해당 방 멤버가 아니면 403을 반환한다.

## 메시지 전송

- Method: `POST`
- Endpoint: `/api/messenger/rooms/{roomId}/messages`
- Body: `messageType`, `content`, `fileId`, `fileName`
- 규칙: TEXT는 `content`, IMAGE/FILE은 `fileId`가 필수다. 발신자는 방 멤버여야 한다.
- `fileId`는 `file` 모듈에 먼저 업로드해서 발급받는다 (`POST /api/files/presigned-url` → S3 업로드 → `POST /api/files`).
- 전송 성공 시 발행되는 `MESSAGE_SENT` WebSocket 이벤트와 메시지 목록 조회 응답에 `fileId`와 함께 `fileDownloadUrl`(1시간짜리 presigned URL)이 바로 포함되므로, 대부분의 경우 파일 열람에 별도 API 호출이 필요 없다. `fileDownloadUrl`이 만료된 뒤 다시 봐야 하는 경우에만 `GET /api/files/{fileId}/download-url`을 직접 호출한다.

## 메시지 목록 조회

- Method: `GET`
- Endpoint: `/api/messenger/rooms/{roomId}/messages`
- Query: `cursorCreatedAt`, `cursorMessageId`, `size`
- 규칙: cursor 값은 둘 다 전달하거나 둘 다 생략한다. `size`는 1 이상 100 이하이다. cursor가 없는 첫 조회에서만 읽음 처리한다.
- IMAGE/FILE 메시지는 응답에 `fileId`와 함께 다운로드용 `fileDownloadUrl`을 포함한다(페이지 안의 fileId를 모아 배치 조회, N+1 없음). 삭제된 메시지는 `fileId`/`fileDownloadUrl`/`fileName`이 모두 `null`로 마스킹된다.

## 업무지시 카드 등록

- Method: `POST`
- Endpoint: `/api/messenger/rooms/{roomId}/task-cards`
- Body: `content`, `dueDate`, `assigneeIds`
- 규칙: 등록자와 담당자는 모두 방 멤버여야 한다.

## 업무지시 카드 목록 조회

- Method: `GET`
- Endpoint: `/api/messenger/rooms/{roomId}/task-cards`
- 규칙: 요청자가 해당 방 멤버가 아니면 403을 반환한다.

## 업무지시 완료

- Method: `PATCH`
- Endpoint: `/api/messenger/rooms/{roomId}/task-cards/{cardId}/complete`
- 규칙: 담당자 본인만 완료 처리할 수 있다. 이미 완료된 경우 완료 시각을 덮어쓰지 않는다.
