# notification 모듈

## 책임

WebSocket 연결이 끊긴 사용자에게 유실되던 알림을 저장하고, 목록/안읽은 개수 조회·읽음 처리·삭제 API를 제공한다.

## 소유하는 주요 데이터

- `notification` 테이블 (`Notification` 도메인 모델) — 수신자별 알림 1건: 타입, 대상 ID, 완성된 문구, 읽음 시각, 소프트 삭제 시각

## 외부에 공개하는 Application API

(아직 없음 — 조회/읽음처리/삭제 API가 추가될 때마다 이 섹션에 한 줄씩 추가한다.)

## 다른 모듈에 요청하는 의존성

- `NotificationUserInfoPort`(자체 정의) — `users` 모듈이 `NotificationUserInfoAdapter`로 구현. 멘션 알림 문구에 필요한 사용자 이름 조회 용도.

## 발행·소비하는 Event

- 발행하는 이벤트 없음
- 소비하는 이벤트 (모두 다른 모듈의 공개 Event, `NotificationCreationListener`가 구독)
  - `workspace.domain.event.TaskCommentMentionedEvent`
  - `approval.domain.event.ApprovalLineActivatedEvent`
  - `approval.domain.event.ApprovalDocumentDecidedEvent` (`boolean approved` 그대로 소비 — 아래 참고)

## 변경 시 주의 사항

- 실시간 전송(`WorkspaceWebSocketNotifier`, `ApprovalWebSocketNotifier`)과 이 모듈의 저장 리스너는 같은 원본 이벤트를 독립적으로 구독하는 팬아웃 구조다. 한쪽을 고칠 때 다른 쪽 리스너 존재를 놓치지 않는다.
- `ApprovalDocumentDecidedEvent.approved()`가 `false`인 경우 반려(REJECTED)와 취소(CANCELLED)를 구분할 수 없어 "결재 문서 처리가 철회되었습니다."로 중립적으로 표시한다. approval 담당 팀원에게 상태 enum 필드 추가를 요청해뒀고, 반영되면 이 문구 분기를 정교화할 수 있다.
- 새 도메인의 이벤트를 알림 대상에 추가하려면 [NOTIFICATION_TYPES.md](NOTIFICATION_TYPES.md)에 타입을 등록하고 `NotificationCreationListener`에 구독 메서드를 추가한다. 기존 코드는 수정하지 않는다.

## 세부 문서

- [NOTIFICATION_TYPES.md](NOTIFICATION_TYPES.md) — 알림 타입 코드 목록
- 설계 배경: `docs/superpowers/specs/2026-08-13-notification-persistence-design.md`
