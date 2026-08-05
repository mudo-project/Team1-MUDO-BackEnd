# messenger 모듈

## 책임과 범위

메신저(채팅) 기능을 담당한다. 채팅방 생성/목록조회/참여자조회, 메시지 목록조회/전송, 업무지시 카드 등록·완료처리를 제공한다. DM(1:1)과 그룹(다인) 채팅을 하나의 `ChatRoom` Aggregate로 표현하며, 초대 인원 수에 따라 타입이 자동으로 결정된다(1명 초대=DM, 2명 이상=GROUP).

## 담당자

BE6 메신저 담당

## 소유하는 주요 데이터와 상태

- `ChatRoom` — DB 테이블 `chat_room` (academy_id, name, type(DM/GROUP), created_by)
- `ChatRoomMember` — DB 테이블 `chat_room_member` (chat_room_id + user_id 진짜 복합키, last_read_at — 안읽은 메시지 수 계산에 사용). JPA는 `@ElementCollection`으로 매핑(값 객체라 자체 identity 없음, 복합키라 surrogate id 없이도 매핑 가능).
- `ChatMessage` — DB 테이블 `chat_message` (message_type: TEXT/IMAGE/FILE, TEXT는 content 필수, IMAGE/FILE은 file_url 필수)
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

- **참여자·발신자 이름 조회 및 활성 사용자 검증**: `ChatMemberDirectoryPort`(application/port)로 추상화한다. 활성 사용자 여부는 users 모듈의 공개 계약인 `UserDirectoryUseCase.findActiveUserIds()`로 확인한다. 다만 이름·소속 학원 조회는 아직 users 쪽 공개 조회 계약이 부족해, `ChatMemberInfoEntity`가 `users` 테이블을 읽기 전용으로 직접 매핑하는 임시 shim으로 남아 있다.
- **파일 업로드**: 이미지·파일 메시지는 `file` 모듈의 presigned URL 방식을 그대로 참조한다. 메시지 전송 API가 파일 업로드를 직접 처리하지 않고 `fileUrl`/`fileName`만 받는다(approval 컨벤션을 따르기로 사용자가 명시적으로 결정).
- **실시간 전송**: `global` 모듈에 추가된 WebSocket(STOMP, `/ws`) 인프라를 메시지 전송·업무지시 완료 성공 후 이벤트 기반으로 브로드캐스트하는 데 사용할 계획이다(미착수, 팀과 최종 확정 필요). 지금은 REST 재조회로만 새 메시지/완료 상태를 확인할 수 있다.

## 발행·소비하는 Event

- 현재 없음.
- 메시지 전송·업무지시 완료 후 실시간 브로드캐스트용 이벤트 발행이 필요할 것으로 예상된다(미착수). ARCHITECTURE.md의 "상태 변경 이벤트는 트랜잭션 완료 후 발행" 원칙을 따를 계획.

## 변경 시 주의 사항

- 채팅방 목록 조회는 approval/notice와 동일하게 페이지네이션 없이 전체 List를 반환하되, 최근 메시지 시각(`lastMessageAt`, 없으면 방 생성 시각) 기준 내림차순으로 정렬한다.
- **메시지 목록 조회만 예외적으로 페이지네이션을 적용한다.** 채팅 기록은 시간이 지날수록 무한히 쌓이는 특성이라, 관리자가 만드는 유한한 콘텐츠(공지·결재)와 다르다고 판단해 사용자와 합의 후 결정했다. offset 기반 `page`가 아니라 `(createdAt, messageId)` 기반 cursor를 쓴다 — offset은 조회 중 새 메시지가 쌓이면 페이지 간 중복·누락이 생기기 때문이다. `ChatMessageRepository.findByChatRoomId`가 `size + 1`건을 가져와 호출측(application)이 `hasNext`를 판단하는 방식을 쓴다. `size`는 1 이상 100 이하로 제한한다.
- 메시지 목록 조회 시 **cursor가 없는 첫 페이지 조회일 때만** 읽음 처리(`lastReadAt` 갱신)를 하도록 확정했다 — 과거 히스토리 스크롤은 읽음으로 치지 않는다(스크롤 중 도착한 새 메시지가 잘못 읽음 처리되는 것을 방지). 카톡처럼 방에 실시간으로 머무는 동안의 즉시 읽음 처리는 WebSocket 연동 시점에 별도로 다룰 주제다.
- 메시지 전송자는 자신이 보낸 메시지 때문에 안읽음 수가 증가하지 않는다. unread 집계는 본인 발신 메시지를 제외하고, 메시지 전송 성공 시 발신자의 `lastReadAt`을 해당 메시지 시각으로 갱신한다.
- 채팅방·메시지·업무지시 생성/완료 시각은 시스템 기본 시간대에 의존하지 않고 `Clock` 빈(`Asia/Seoul`) 기준으로 생성한다.
- 업무지시 카드의 담당자는 반드시 해당 채팅방 멤버여야 한다(등록 시 검증). 완료 처리는 담당자 본인만 가능하며, 이미 완료한 담당자가 다시 호출해도 시각이 덮어써지지 않는다(단조성 보장).
- 도메인 규칙 위반은 `messenger.domain.exception.MessengerErrorCode`(→ `MessengerException`, `BusinessException` 상속)로 던진다. approval/notice의 선례를 따랐다 (`MESSENGER_{status}_{n}` 코드 체계).
- 나가기·숨김 기능이 없어 채팅방·메시지·업무지시 카드 Repository에 삭제 메서드가 없다.

## 세부 문서

- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- [API.md](API.md) — 현재 REST API 계약 요약
- [CHANGELOG.md](CHANGELOG.md) — 사용자 관점 변경 요약
