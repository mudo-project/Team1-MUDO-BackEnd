# Messenger Revision

## 2026-08-06 · 업무지시 카드 실시간 반영 + echo/optimistic 최종 결정

### 배경

메시지 전송/읽음 처리는 이미 WebSocket으로 실시간이었지만, 업무지시 카드 등록·완료는 이벤트 발행이 없어 REST 재조회 없이는 실시간으로 반영되지 않았다. 또한 발신자 자신에게도 소켓 echo를 그대로 보내는 현재 방식(echo)이 프론트 UX·안정성 측면에서 맞는지 별도로 논의가 필요한 상태로 남아 있었다.

### echo vs optimistic 결정

**방식2(optimistic UI) + B안으로 확정.** 서버는 지금처럼 room topic에 발신자/행위자를 포함해 전체 브로드캐스트하고(서버 코드 변경 없음), 프론트가 자신의 행위는 REST 응답으로 즉시 반영하고 자기 자신에게 돌아온 소켓 echo(`senderUserId`/행위자 ID가 본인과 같은 경우)는 무시한다.

- echo만 쓰는 방식은 REST 저장 → 커밋 → 소켓 push 사이에 연결이 잠깐 끊기면 이미 저장된 내 행위가 내 화면에는 반영되지 않는 신뢰성 문제가 있다. optimistic은 REST 응답만으로 내 상태를 확정해 이 문제를 구조적으로 없앤다.
- 서버가 발신자를 실제로 제외하고 개별 전송하는 A안(`convertAndSendToUser` 기반)은 구조 변경이 크고, B안 대비 얻는 이득이 없어 채택하지 않았다.
- `MessageSendResponse`에 서버 `createdAt`을 추가할지도 검토했으나, 메시지 목록 정렬은 시계 오차와 무관한 `messageId` 기준으로 하면 되므로 기각했다.

### 업무지시 카드 실시간 반영

- `TaskCardCreatedEvent`, `TaskCardCompletedEvent` 추가.
- `CreateTaskCardService`/`CompleteTaskService`가 저장/완료 후 각각 이벤트를 발행하고, `MessengerWebSocketNotifier`가 메시지와 동일하게 `/topic/messenger/rooms/{roomId}`로 브로드캐스트한다.
- 완료 이벤트는 `chatTaskCard.complete()` 호출 직후의 인메모리 값으로 `completedCount`/`assigneeCount`/`fullyCompleted`를 함께 담아, 프론트가 재조회 없이 progress bar를 갱신할 수 있게 했다.

### Decision

유저 단위 알림 채널(`/user/queue`)은 만들지 않았다. 업무 탭(받은업무/전달한업무) 알림이 방을 보고 있지 않을 때도 실시간으로 와야 하는지 확인한 결과, 이번 스코프에서는 불필요하다고 확정했다(방을 보고 있을 때만 실시간, 그 외엔 REST 재조회). 사이드바 안읽음 뱃지 등에서 다시 필요해질 수 있어 완전히 닫힌 결정은 아니다.

> 작성일: 2026-08-06
> 상태: 백엔드 구현 완료, 테스트 통과. 프론트 반영은 별도 진행 필요.

## 2026-08-05 - Realtime and message controls

### Background

The messenger package needed the remaining product behavior after the initial review fixes: duplicate DM prevention, sender-only message edit/delete, KakaoTalk-style unread numbers, and realtime room updates through the existing WebSocket infrastructure.

### Changes

- Added existing DM lookup before creating a 1:1 room.
- Added `edited_at` and `deleted_at` columns to `chat_message`.
- Added sender-only edit and soft delete use cases.
- Added `unreadCount` per message using `chat_room_member.last_read_at`.
- Added `ChatMessageSentEvent` and `ChatRoomReadEvent`.
- Added `MessengerWebSocketNotifier` using `/topic/messenger/rooms/{roomId}`.

### Decision

No `notification` package was created. Messenger realtime updates are handled inside messenger, while shared WebSocket infrastructure remains in `global`.

> 작성일: 2026-08-05
> 상태: 코드 리뷰 보완 완료

## 2026-08-05 · 메신저 코드 리뷰 보완

### 배경

빠른 머지 후 리뷰에서 시간 정책, unread 집계, 사용자 상태 검증, 입력 검증, fetch plan, 테스트·문서 공백이 확인됐다.

### 반영 내용

- `ChatRoom`/`ChatMessage` 생성 시각을 `Clock` 기반으로 주입받아 기록하도록 바꿨다.
- unread 집계에서 본인이 보낸 메시지를 제외하고, 메시지 전송 시 발신자의 `lastReadAt`을 갱신한다.
- 초대·참여자 정보 조회 시 users 공개 계약인 `UserDirectoryUseCase.findActiveUserIds()`로 `ACTIVE` 사용자만 통과시킨다.
- 메시지 목록 조회 `size`를 1~100으로 제한한다.
- 참여자/담당자 ID 리스트에 null 또는 0 이하 값이 들어오면 400 계열 도메인 예외로 막는다.
- `chat_room.members`, `chat_task_card.assignees` 조회에 fetch plan을 지정해 `ElementCollection` N+1 가능성을 줄였다.
- 메신저 전용 유닛/슬라이스 테스트를 추가했다.

### 남은 구조 부채

`ChatMemberInfoEntity`는 아직 `users` 테이블에서 이름과 academyId를 직접 읽는 임시 shim이다. 활성 사용자 검증은 users 공개 UseCase로 옮겼지만, 이름·소속 조회까지 완전히 정리하려면 users 도메인에 공개 조회 계약 추가가 필요하다. 다른 도메인 코드 직접 수정 금지 규칙 때문에 이번 변경에서는 messenger 내부 보완까지만 적용했다.

### 완료 기준

- [x] 메신저 시간 기록이 시스템 기본 시간대에 의존하지 않는다.
- [x] 본인 발신 메시지가 unread로 집계되지 않는다.
- [x] 비활성/퇴사 사용자는 메신저 참여자 검증을 통과하지 못한다.
- [x] 메시지 조회 size와 ID 리스트 입력값을 검증한다.
- [x] 메신저 전용 테스트가 추가됐다.

