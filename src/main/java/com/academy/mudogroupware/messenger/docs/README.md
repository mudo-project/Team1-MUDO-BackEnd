# messenger 모듈

## 책임과 범위

메신저(채팅) 기능을 담당한다. 채팅방 생성/목록조회/참여자조회, 메시지 목록조회/전송, 업무지시 카드 등록·완료처리를 제공한다. DM(1:1)과 그룹(다인) 채팅을 하나의 `ChatRoom` Aggregate로 표현하며, 초대 인원 수에 따라 타입이 자동으로 결정된다(1명 초대=DM, 2명 이상=GROUP).

## 담당자

BE6 메신저 담당

## 소유하는 주요 데이터와 상태

- `ChatRoom` — DB 테이블 `chat_room` (name, type(DM/GROUP), created_by). `academy_id`는 2026-08-10에 제거했다 — 더 이상 학원 단위로 채팅방을 제한하지 않는다.
- `ChatRoomMember` — DB 테이블 `chat_room_member` (chat_room_id + user_id 진짜 복합키, last_read_at — 안읽은 메시지 수 계산에 사용). JPA는 `@ElementCollection`으로 매핑(값 객체라 자체 identity 없음, 복합키라 surrogate id 없이도 매핑 가능).
- `ChatMessage` — DB 테이블 `chat_message` (message_type: TEXT/IMAGE/FILE, TEXT는 content 필수, IMAGE/FILE은 file_id 필수)
- `ChatTaskCard` — DB 테이블 `chat_task_card` (chat_room_id, assigner_user_id, content, due_date(nullable))
- `ChatTaskAssignee` — DB 테이블 `chat_task_assignee` (card_id + user_id 유니크, completed_at — 담당자별 완료 여부). `chat_room_member`와 달리 surrogate id + UNIQUE KEY 구조(notice_read와 동일 컨벤션). JPA도 `@ElementCollection`으로 매핑.
- 명시적으로 제외된 컬럼: `write_policy`, `hidden_at`/`hidden_by`(나가기·숨김 기능 없음, 확정), `notification_id`(알림함/푸시 기능 자체가 미설계 상태라 제거)

## 외부에 공개하는 Application API

- `CreateChatRoomUseCase` — 채팅방 생성
- `ChatRoomQueryUseCase` — 내 채팅방 목록 조회 (안읽은 메시지 수, 최근 메시지 미리보기 포함)
- `ChatRoomMemberQueryUseCase` — 채팅방 참여자 목록 조회
- `SendMessageUseCase` — 메시지 전송
- `ChatMessageQueryUseCase` — 메시지 목록 조회 (cursor 페이지네이션, 아래 참고)
- `CreateTaskCardUseCase` — 업무지시 카드 등록 (방 멤버만 담당자로 지정 가능)
- `TaskCardQueryUseCase` — 업무지시 카드 목록 조회 (완료 인원/전체 담당자 수, 전원완료 여부 포함)
- `CompleteTaskUseCase` — 담당자 본인의 업무지시 완료 처리

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **참여자·발신자 이름 조회 및 활성 사용자 검증**: `ChatMemberDirectoryPort`(application/port)로 추상화한다. 활성 사용자 여부는 users 모듈의 공개 계약인 `UserDirectoryUseCase.findActiveUserIds()`로 확인한다. 다만 이름 조회는 아직 users 쪽 공개 조회 계약이 부족해, `ChatMemberInfoEntity`가 `users` 테이블을 읽기 전용으로 직접 매핑하는 임시 shim으로 남아 있다(academyId 필드는 2026-08-12에 제거함).
- **파일 업로드**: 이미지·파일 메시지는 `file` 모듈에서 발급받은 `fileId`를 참조한다(`ChatMessage.fileId` → `file_metadata`). 메시지 전송 API가 파일 업로드를 직접 처리하지 않고 `fileId`/`fileName`만 받는다. 원래는 프론트가 `fileUrl`을 직접 채워 보내는 방식이었으나, 파일을 업로드해서 URL을 발급받는 API 자체가 없어 실제로 채울 수 없는 값이었음이 확인되어 approval/notice와 동일하게 `fileId` 참조 방식으로 전환했다(2026-08-10).
- **실시간 전송**: `global` 모듈에 추가된 WebSocket(STOMP, `/ws`) 인프라를 사용해, 메시지 전송·읽음 처리·업무지시 등록·업무지시 완료를 이벤트 기반으로 room topic에 브로드캐스트한다. 발신자/행위자를 제외하지 않고 room 전체를 대상으로 보낸다(echo 방식) — 프론트가 optimistic UI로 자기 행위는 즉시 그리고, 자신에게 돌아온 echo는 무시하는 방식을 채택하기로 확정했다(2026-08-06). 유저 단위 알림 채널(`/user/queue` 등, 방을 보고 있지 않아도 오는 알림)은 이번 스코프에서 필요 없다고 확인되어 만들지 않았다 — room topic 구독 중일 때만 실시간이고, 안 보고 있으면 REST 재조회로 확인한다.

## 발행·소비하는 Event

모두 `AFTER_COMMIT` 시점에 `MessengerWebSocketNotifier`가 `/topic/messenger/rooms/{roomId}`로 브로드캐스트한다(ARCHITECTURE.md의 "상태 변경 이벤트는 트랜잭션 완료 후 발행" 원칙).

- `ChatMessageSentEvent` (`eventType: MESSAGE_SENT`) — 메시지 전송 시.
- `ChatRoomReadEvent` (`eventType: MESSAGE_READ`) — 읽음 처리 시.
- `TaskCardCreatedEvent` (`eventType: TASK_CARD_CREATED`) — 업무지시 카드 등록 시.
- `TaskCardCompletedEvent` (`eventType: TASK_CARD_COMPLETED`) — 담당자 완료 처리 시, `completedCount`/`assigneeCount`/`fullyCompleted`를 함께 담아 progress bar 갱신에 필요한 값을 재조회 없이 전달한다.

## 변경 시 주의 사항

- 채팅방 목록 조회는 approval/notice와 동일하게 페이지네이션 없이 전체 List를 반환하되, 최근 메시지 시각(`lastMessageAt`, 없으면 방 생성 시각) 기준 내림차순으로 정렬한다.
- **메시지 목록 조회만 예외적으로 페이지네이션을 적용한다.** 채팅 기록은 시간이 지날수록 무한히 쌓이는 특성이라, 관리자가 만드는 유한한 콘텐츠(공지·결재)와 다르다고 판단해 사용자와 합의 후 결정했다. offset 기반 `page`가 아니라 `(createdAt, messageId)` 기반 cursor를 쓴다 — offset은 조회 중 새 메시지가 쌓이면 페이지 간 중복·누락이 생기기 때문이다. `ChatMessageRepository.findByChatRoomId`가 `size + 1`건을 가져와 호출측(application)이 `hasNext`를 판단하는 방식을 쓴다. `size`는 1 이상 100 이하로 제한한다.
- 메시지 목록 조회 시 **cursor가 없는 첫 페이지 조회일 때만** 읽음 처리(`lastReadAt` 갱신)를 하도록 확정했다 — 과거 히스토리 스크롤은 읽음으로 치지 않는다(스크롤 중 도착한 새 메시지가 잘못 읽음 처리되는 것을 방지). 카톡처럼 방에 실시간으로 머무는 동안의 즉시 읽음 처리는 WebSocket 연동 시점에 별도로 다룰 주제다.
- 메시지 전송자는 자신이 보낸 메시지 때문에 안읽음 수가 증가하지 않는다. unread 집계는 본인 발신 메시지를 제외하고, 메시지 전송 성공 시 발신자의 `lastReadAt`을 해당 메시지 시각으로 갱신한다.
- 채팅방·메시지·업무지시 생성/완료 시각은 시스템 기본 시간대에 의존하지 않고 `Clock` 빈(`Asia/Seoul`) 기준으로 생성한다.
- 업무지시 카드의 담당자는 반드시 해당 채팅방 멤버여야 한다(등록 시 검증). 완료 처리는 담당자 본인만 가능하며, 이미 완료한 담당자가 다시 호출해도 시각이 덮어써지지 않는다(단조성 보장).
- 담당자 중 1명이라도 완료 처리한 업무지시 카드는 수정·삭제할 수 없다(`TASK_CARD_HAS_COMPLETION`, `MESSENGER_400_19`, 2026-08-11). 완료 기록이 남은 뒤 내용/담당자/마감일이 바뀌거나 카드 자체가 사라지는 것을 막기 위함이다.
- 도메인 규칙 위반은 `messenger.domain.exception.MessengerErrorCode`(→ `MessengerException`, `BusinessException` 상속)로 던진다. approval/notice의 선례를 따랐다 (`MESSENGER_{status}_{n}` 코드 체계).
- 나가기·숨김 기능이 없어 채팅방·메시지·업무지시 카드 Repository에 삭제 메서드가 없다.

## 세부 문서

- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- [API.md](API.md) — 현재 REST API 계약 요약
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점 변경 요약
# 2026-08-05 realtime/message-control update

- Existing DM rooms are reused instead of creating duplicate 1:1 rooms.
- Text messages can be edited by the original sender.
- Messages can be soft-deleted by the original sender; deleted rows remain for cursor stability.
- Message responses expose KakaoTalk-style `unreadCount` rather than reader names.
- Messenger publishes `ChatMessageSentEvent` and `ChatRoomReadEvent`.
- `MessengerWebSocketNotifier` broadcasts both events through the existing global STOMP endpoint `/ws`.
- Room clients subscribe to `/topic/messenger/rooms/{roomId}` and branch on `eventType`.
- No `notification` package was introduced.
