# Messenger API

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
- Body: `messageType`, `content`, `fileUrl`, `fileName`
- 규칙: TEXT는 `content`, IMAGE/FILE은 `fileUrl`이 필수다. 발신자는 방 멤버여야 한다.

## 메시지 목록 조회

- Method: `GET`
- Endpoint: `/api/messenger/rooms/{roomId}/messages`
- Query: `cursorCreatedAt`, `cursorMessageId`, `size`
- 규칙: cursor 값은 둘 다 전달하거나 둘 다 생략한다. `size`는 1 이상 100 이하이다. cursor가 없는 첫 조회에서만 읽음 처리한다.

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
