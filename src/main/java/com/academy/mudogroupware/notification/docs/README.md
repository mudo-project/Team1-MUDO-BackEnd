# notification 모듈

## 책임

WebSocket 연결이 끊긴 사용자에게 유실되던 알림을 저장하고, 목록/안읽은 개수 조회·읽음 처리·삭제 API를 제공한다.

## 소유하는 주요 데이터

- `notification` 테이블 (`Notification` 도메인 모델) — 수신자별 알림 1건: 타입, 대상 ID, 완성된 문구(최대 250자), 읽음 시각, 소프트 삭제 시각
- 목록 조회는 `created_at DESC, notification_id DESC`로 정렬한다. 같은 시각에 여러 알림이 생성돼도 `created_at` 단독으로는 순서가 보장되지 않아 `id`를 tiebreak로 추가했다.

## 외부에 공개하는 Application API

상세 요청/응답은 [API.md](API.md) 참고.

- `GET /api/notifications` — 목록 조회 (offset 페이지네이션, 20개, 최신순, 필터 없음)
- `GET /api/notifications/unread-count` — 안읽은 개수 조회
- `PATCH /api/notifications/{notificationId}/read` — 읽음 처리
- `DELETE /api/notifications/{notificationId}` — 개별 삭제(소프트 삭제)
- `DELETE /api/notifications?status=READ` — 읽은 알림 일괄 삭제(소프트 삭제)

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
- `type`은 의도적으로 `NotificationType` enum이 아니라 문자열로 저장한다. 도메인마다 자기 코드를 문자열로 넘기게 해서, 새 타입이 추가될 때마다 이 모듈의 enum을 고쳐야 하는 결합을 피하기 위함이다(코드 리뷰에서 반복 제기될 수 있는 지점).
- `markAsRead(null)`은 예외를 던진다(조용히 무시하지 않음). 이미 읽은 알림에 다시 호출하는 것(멱등)과 `null`을 넘기는 것(잘못된 호출)은 구분한다.
- `markAsRead`/`delete`는 조회 후 dirty checking에 기대는 방식이라 완전히 원자적이진 않다. 동시 요청 시 나중에 커밋한 쪽의 시각이 남을 수 있으나, 두 연산 모두 멱등이라 최종 상태(읽음/삭제됨)는 동일해서 의도적으로 락을 걸지 않았다.

## 세부 문서

- [API.md](API.md) — API 요청/응답 명세
- [NOTIFICATION_TYPES.md](NOTIFICATION_TYPES.md) — 알림 타입 코드 목록
- 설계 배경: `docs/superpowers/specs/2026-08-13-notification-persistence-design.md`
