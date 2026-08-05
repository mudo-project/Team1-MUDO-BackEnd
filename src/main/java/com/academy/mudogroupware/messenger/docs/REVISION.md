# Messenger Revision

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

