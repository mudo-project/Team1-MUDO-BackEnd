# Messenger Revision

## 2026-08-07 · 업무지시 카드 목록조회 페이지네이션 추가

### 배경

k6로 memo/messenger 로컬 부하테스트를 진행하며, 업무지시 카드 목록조회가 페이지네이션 없이 방의 카드를 전부 반환하고 있음을 발견했다(1,000건 seed 기준 응답 374KB). 메시지 목록조회는 이미 cursor 페이지네이션이 있어 20,000건을 seed해도 응답이 6.4KB로 고정되는 것과 대조적이었다. 카드가 계속 쌓여야 하는 도메인 특성상(memo처럼 개수 상한을 두는 방식은 부적합) 페이지네이션을 붙이기로 했다.

### 변경 내용

- `ChatTaskCardJpaRepository.findAllByChatRoomIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc` → `findPage`로 교체, 메시지 목록조회와 동일한 `createdAt`/`id` 기반 cursor 쿼리(`@EntityGraph(attributePaths = "assignees")` 유지).
- `TaskCardQueryUseCase`/`TaskCardQueryService`가 `cursorCreatedAt`/`cursorCardId`/`size`를 받아 `TaskCardPageView`(content/hasNext/nextCursorCreatedAt/nextCursorCardId)를 반환하도록 변경. size 범위(1~100)와 cursor 완전성 검증은 `ChatMessageQueryService`와 동일한 패턴.
- `GET /api/messenger/rooms/{roomId}/task-cards` 응답이 배열에서 `{content, hasNext, nextCursorCreatedAt, nextCursorCardId}` 페이지 구조로 변경(breaking change) — 프론트 반영 필요.
- 신규 에러코드: `INVALID_TASK_CARD_CURSOR`(`MESSENGER_400_17`), `INVALID_TASK_CARD_PAGE_SIZE`(`MESSENGER_400_18`).

> 작성일: 2026-08-07
> 상태: 백엔드 구현 완료, 테스트 통과, k6로 응답 크기 감소(374KB → 7.7KB) 확인. 프론트 반영은 별도 진행 필요(응답이 배열에서 페이지 객체로 바뀌는 breaking change).

## 2026-08-06 · 메시지·업무지시 카드 수정/삭제 실시간 반영

### 배경

메시지 수정/삭제 REST API는 이미 있었지만 실시간 브로드캐스트가 없어 상대방 화면에 재조회 전까지 반영되지 않았다. 업무지시 카드는 수정/삭제 API 자체가 아직 없었다.

### 변경 내용

- `MessageEditedEvent`/`MessageDeletedEvent` 추가, `MessengerWebSocketNotifier`가 메시지 전송과 동일하게 `/topic/messenger/rooms/{roomId}`로 `MESSAGE_EDITED`/`MESSAGE_DELETED` 브로드캐스트.
- `chat_task_card`에 `deleted_at` 컬럼 추가(`V6.1.4`), `ChatTaskCard.update()`/`delete()` 도메인 메서드 추가(등록자 본인만 가능, 소프트 삭제).
- `PATCH`/`DELETE /api/messenger/rooms/{roomId}/task-cards/{cardId}` 추가, `TASK_CARD_UPDATED`/`TASK_CARD_DELETED` 브로드캐스트 추가.
- 신규 에러코드: `NOT_TASK_CARD_OWNER`, `TASK_CARD_ALREADY_DELETED`.
- 담당자 목록 변경은 전체 재저장이 아니라 추가/삭제분만 targeted 쿼리로 반영 — 담당자 완료 처리와의 동시 업데이트 유실을 설계 단계에서 방지.

### 동시성 버그 수정 (CodeRabbit 리뷰)

- `UpdateTaskCardService`: `updateContent`의 영향 행 수를 확인해, 조회 이후 다른 트랜잭션이 먼저 삭제를 커밋한 경우 담당자 변경/이벤트 발행 없이 `TASK_CARD_ALREADY_DELETED`를 던지도록 수정(`deleted_at is null` 조건의 UPDATE가 잡는 행 잠금을 이용, 별도 lock/`@Version` 불필요).
- `DeleteTaskCardService`: 이미 삭제된 카드를 재삭제해도 실제 상태 변경이 없으면 `markDeleted` 호출과 이벤트 발행을 스킵(권한 검증은 항상 수행, 재삭제 자체는 메시지 삭제와 동일하게 에러 없는 idempotent 204 유지).

> 작성일: 2026-08-06
> 상태: 백엔드 구현 완료, 테스트 통과(`ChatTaskCardJpaRepository` native SQL 자체에 대한 리포지토리 레벨 테스트는 아직 없음). API 명세서(`docs/api-specs/messenger_api_spec.md`)는 REST 2건 반영 완료, Notion 반영은 별도 진행 필요.

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

