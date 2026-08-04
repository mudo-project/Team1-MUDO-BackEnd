# messenger 모듈

## 책임과 범위

메신저(채팅) 기능을 담당한다. 채팅방 생성/목록조회, 메시지 목록조회/전송을 제공한다. DM(1:1)과 그룹(다인) 채팅을 하나의 `ChatRoom` Aggregate로 표현하며, 초대 인원 수에 따라 타입이 자동으로 결정된다(1명 초대=DM, 2명 이상=GROUP).

## 담당자

(팀 확인 필요)

## 소유하는 주요 데이터와 상태

- `ChatRoom` — DB 테이블 `chat_room` (academy_id, name, type(DM/GROUP), created_by)
- `ChatRoomMember` — DB 테이블 `chat_room_member` (chat_room_id + user_id 복합키, last_read_at — 안읽은 메시지 수 계산에 사용)
- `ChatMessage` — DB 테이블 `chat_message` (message_type: TEXT/IMAGE/FILE, TEXT는 content 필수, IMAGE/FILE은 file_url 필수)
- 명시적으로 제외된 컬럼: `write_policy`, `hidden_at`/`hidden_by`(나가기·숨김 기능 없음, 확정), `notification_id`(알림함/푸시 기능 자체가 미설계 상태라 제거)

## 외부에 공개하는 Application API

(usecase 인터페이스까지만 정의됨 — service 구현 전, 미완성 상태)

- `CreateChatRoomUseCase` — 채팅방 생성
- `ChatRoomQueryUseCase` — 내 채팅방 목록 조회 (안읽은 메시지 수 포함)
- `SendMessageUseCase` — 메시지 전송
- `ChatMessageQueryUseCase` — 메시지 목록 조회 (페이지네이션 적용, 아래 참고)

## 다른 모듈 또는 외부 시스템에 요청하는 의존성

- **참여자·발신자 이름 조회**: `ChatMemberDirectoryPort`(application/port)로 추상화. User 도메인 모듈이 아직 없어, approval의 `UserNameEntity`·notice의 `UserInfoEntity`와 동일한 성격의 임시 shim으로 구현할 예정이다(미착수). User 모듈이 생기면 세 모듈 모두 정식 구현으로 교체해야 한다.
- **파일 업로드**: 이미지·파일 메시지는 `file` 모듈의 presigned URL 방식을 그대로 참조한다. 메시지 전송 API가 파일 업로드를 직접 처리하지 않고 `fileUrl`/`fileName`만 받는다(approval 컨벤션을 따르기로 사용자가 명시적으로 결정).
- **실시간 전송**: `global` 모듈에 추가된 WebSocket(STOMP, `/ws`) 인프라를 메시지 전송 성공 후 이벤트 기반으로 브로드캐스트하는 데 사용할 계획이다(미착수, 팀과 최종 확정 필요).

## 발행·소비하는 Event

- 현재 없음.
- 메시지 전송 완료 후 실시간 브로드캐스트용 이벤트(예: `MessageSentEvent`) 발행이 필요할 것으로 예상된다(미착수). ARCHITECTURE.md의 "상태 변경 이벤트는 트랜잭션 완료 후 발행" 원칙을 따를 계획.

## 변경 시 주의 사항

- 채팅방 목록 조회는 approval/notice와 동일하게 페이지네이션 없이 전체 List를 반환한다.
- **메시지 목록 조회만 예외적으로 페이지네이션을 적용한다.** 채팅 기록은 시간이 지날수록 무한히 쌓이는 특성이라, 관리자가 만드는 유한한 콘텐츠(공지·결재)와 다르다고 판단해 사용자와 합의 후 결정했다. offset 기반 `page`가 아니라 `(createdAt, messageId)` 기반 cursor를 쓴다 — offset은 조회 중 새 메시지가 쌓이면 페이지 간 중복·누락이 생기기 때문이다. `ChatMessageRepository.findByChatRoomId`가 `size + 1`건을 가져와 호출측(application)이 `hasNext`를 판단하는 방식을 쓴다.
- 메시지 목록 조회 시 **cursor가 없는 첫 페이지 조회일 때만** 읽음 처리(`lastReadAt` 갱신)를 하도록 설계했다 — 과거 히스토리 스크롤은 읽음으로 치지 않는다. **스펙에 명시된 규칙이 아니라 구현 중 판단한 가정이므로 팀 확인이 필요하다.**
- 도메인 규칙 위반은 `global.domain.common.exception`의 `BadRequestException`/`NotFoundException`/`ForbiddenException`을 사용한다.
- 나가기·숨김 기능이 없어 채팅방·메시지 Repository에 삭제 메서드가 없다.

## 세부 문서

- [REVISION.md](REVISION.md) — 설계 변경 이력과 배경
- API.md, API_FLOW.md, CHANGELOG.md는 service/infrastructure/presentation 계층 구현과 테스트가 끝난 뒤 실제 코드 기준으로 추가할 예정이다.
