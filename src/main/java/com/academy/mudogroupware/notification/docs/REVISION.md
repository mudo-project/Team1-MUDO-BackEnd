# Notification Revision

## 2026-08-17 · 알림 저장 신뢰성 강화 - 멱등키 + 재시도

### 배경

트랜잭션 분리(아래 8/15 항목)로 "저장이 실제로 커밋되는가"는 해결됐지만, 그 위에 두 가지가 남아있었다.

- 일시적 DB 오류로 저장이 실패하면 재시도 없이 그대로 유실됨
- 재시도를 붙이면 응답 유실 등으로 같은 알림이 중복 저장될 위험이 생김

### 변경 내용

- `notification` 테이블에 `idempotency_key` 컬럼 + 유니크 제약 추가(`V3.2.2`, be3 폴더에 배치 — 원본 테이블 생성(`V3.2.1`)과 같은 폴더에 이어서 두는 게 맞다고 판단해 be7에서 이동).
- 4개 이벤트(멘션·결재선·결재확정·매출리포트) 각각 이벤트 타입에 맞는 자연키로 멱등키 계산(`NotificationCreationListener`). 예: 멘션은 `commentId+recipientUserId` — `target_id`(taskId)를 그대로 쓰면 같은 업무의 다른 댓글 멘션과 겹칠 수 있어 별도 계산.
- `NotificationCommandService.create()`에 `@Retryable(retryFor = TransientDataAccessException.class, maxAttempts = 3, backoff = 200ms)` 적용. 일시적 DB 오류만 재시도한다.
- 멱등키 유니크 위반은 재시도 없이 idempotent 무시 처리하되, `DataIntegrityViolationException` 전체를 무시하면 `fk_notification_recipient` 등 다른 제약 위반까지 삼켜버리는 문제가 있어(코드래빗 리뷰 반영) 예외 메시지에 `idempotency_key`가 포함된 경우만 무시하고 나머지는 다시 던진다.
- `Notification.idempotencyKey`에 255자 길이 제한 검증 추가(DB 컬럼 제약과 일치, 코드래빗 리뷰 반영).
- 웹소켓 실시간 전송 쪽은 이번 범위에서 제외 — 연결 끊긴 사용자에겐 재시도가 의미 없고, 목록 조회로 복구되는 기존 fallback이 있음.
- 마이그레이션은 컬럼 추가 시점에 기존 행을 기본값(빈 문자열)으로 채운 뒤 즉시 backfill(타입+대상+수신자+PK 조합)하는 방식이라 기존 데이터에 NULL이 들어가지 않는다. 다만 무중단(롤링) 배포 중 구버전 인스턴스가 이 컬럼을 모른 채 INSERT를 시도하면 NOT NULL 제약에 걸릴 수 있는 리스크는 인지하고 있고, 아직 테스트 단계라 감수하기로 결정했다(추후 배포 빈도가 늘면 nullable 추가/backfill과 NOT NULL·유니크 제약 적용을 별도 릴리스로 나누는 방식 검토).

> 작성일: 2026-08-17
> 이슈: [#581](https://github.com/mudo-project/Team1-MUDO-BackEnd/issues/581) · PR: [#582](https://github.com/mudo-project/Team1-MUDO-BackEnd/pull/582)
> 상태: 구현 완료, 테스트 통과(도메인 단위 테스트, DB 통합 테스트, 재시도/멱등 처리 검증). 코드래빗 리뷰 4건 중 3건 반영, 1건(FK 위반까지 실제 DB로 검증)은 `NotificationEntity.recipientUserId`가 `@ManyToOne` 관계가 아니라 테스트 스키마(H2)에 FK 자체가 없어 재현 불가 확인.

## 2026-08-15 · 알림 이벤트 저장 트랜잭션 분리

### 배경

댓글 멘션·결재선 알림 등에서 실시간 WebSocket 전송은 정상 도착하는데, 알림 목록(`GET /api/notifications`)에는 해당 알림이 나타나지 않는 문제가 있었다. API 응답·로그 어디에도 에러가 없어 겉으로는 정상 동작처럼 보이는, 에러 없이 조용히 실패하는 유형의 결함이었다.

원인은 `@TransactionalEventListener(phase = AFTER_COMMIT)` 리스너에서 원본 트랜잭션이 커밋된 직후 실행되는 저장 로직(`NotificationCommandService.create()`)이 별도 트랜잭션 전파 설정 없이 기본 `@Transactional`(REQUIRED)만 걸려 있었기 때문이다. 원본 트랜잭션이 이미 종료된 시점이라 새 트랜잭션을 열어야 정상인데, Spring이 해당 시점의 트랜잭션 동기화 컨텍스트를 아직 다 정리하기 전이라 REQUIRED가 새 트랜잭션을 열지 않고 이미 종료된 트랜잭션에 잘못 편입되는 경우가 있었다. 그 결과 `save()`는 예외 없이 ID까지 정상 반환하지만, 실제 커밋은 일어나지 않아 DB에는 아무것도 남지 않았다.

### 변경 내용

- `NotificationCommandService.create()`에 `@Transactional(propagation = Propagation.REQUIRES_NEW)`를 명시해서, 원본 트랜잭션과 완전히 분리된 새 트랜잭션에서 알림 저장이 확실히 커밋되도록 수정.
- 트랜잭션 밖에서 발행되는 매출 리포트 이벤트는 `RevenueReportGeneratedEvent` 리스너에 `fallbackExecution = true`를 추가해 별도로 대응.

> 작성일: 2026-08-15
> PR: [#552](https://github.com/mudo-project/Team1-MUDO-BackEnd/pull/552)
> 상태: 구현 완료, 회귀 테스트(`NotificationPersistenceTransactionDataJpaTest`) 통과. 수정 전 커밋으로 재현 시 3건 전부 실패, 수정 후 3건 전부 통과로 확인.
