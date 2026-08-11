# 🔄 Google 연동 도메인 변경 이력

## ✅ 2026-08-11 · 리프레시 토큰 폐기 타이밍 개선과 콜백 실패 로깅

### 변경 목적

Codex 코드 리뷰에서 지적된 나머지 두 항목을 반영한다(오류 분류는 앞선 라운드에서 완료). ① 기존 리프레시 토큰 폐기(revoke)가 DB 트랜잭션 커밋보다 먼저 실행되어, 이후 DB 쓰기가 실패해 롤백돼도 이미 폐기된 토큰은 되돌릴 수 없는 문제를 고친다. ② OAuth 콜백 실패 시 서버 로그에 원인이 전혀 남지 않던 문제를 고친다.

state 일회성(재사용 방지) 처리는 이번 범위에서 제외했다 — 프론트엔드가 `authorize-url` 호출을 Next.js Server Action으로 경유하고 있어 백엔드의 `Set-Cookie`가 브라우저에 도달하지 않고, 서버 측 저장 방식은 Google의 authorization code 자체가 이미 1회용이라 실효성이 낮다. 공유파일이 실사용 단계에 들어가면 재검토한다.

### 구현 변경

- `OldGoogleRefreshTokenRevocationRequestedEvent` 신규 추가. `CompleteGoogleAccountConnectionService`(계정 교체)와 `DisconnectGoogleAccountService`(연동 해제) 둘 다 DB 쓰기 직후 이 이벤트를 발행하고, `GoogleTokenRevocationListener`가 `@TransactionalEventListener(phase = AFTER_COMMIT)`로 받아 그제서야 `googleOAuthPort.revoke()`를 호출한다. 커밋이 실패해 롤백되면 이벤트 자체가 발행되지 않아 revoke 요청도 나가지 않는다.
- `GoogleAccountConnectionController.completeConnection()`에 실패 원인 로그 3갈래를 추가했다: Google이 `error` 파라미터를 보낸 경우, `code`/`state`가 누락된 경우, 연동 처리 중 예외가 발생한 경우(예외 타입+메시지). `code`·`state`(토큰·인가코드가 실린 값)는 로그에 남기지 않는다.

### 검증

- `CompleteGoogleAccountConnectionServiceTest`: 즉시 `revoke()` 호출 검증을 이벤트 발행 검증으로 교체.
- `DisconnectGoogleAccountServiceTest`: 동일하게 즉시 `revoke()` 호출 없음 + 이벤트 발행을 검증.
- `GoogleTokenRevocationListenerTest`(신규): 이벤트 수신 시 `revoke()` 호출 검증.
- 콜백 로깅은 기존 `GoogleAccountConnectionControllerTest`의 리다이렉트 동작 테스트가 그대로 통과하는 것으로 회귀 없음만 확인했다(로그 출력 자체를 캡처하는 테스트 관례가 이 프로젝트에 없음).
- 전체 `./gradlew test` 통과.

> 연동 해제·계정 교체 시 revoke 시점 변경은 [README.md](README.md), [GOOGLE_API.md](GOOGLE_API.md)에도 반영했다. 📚

## ✅ 2026-08-11 · Google 토큰 오류를 영구/일시로 분류

### 변경 목적

Codex 코드 리뷰에서 "영구적인 토큰 만료와 일시적인 Google 장애를 구분하지 못한다"는 문제가 지적됐다. `CheckGoogleAccountConnectionService`는 일시적 500/429/타임아웃도 무조건 `failed=true`로 확정했고, `GetGoogleAccessTokenService`는 반대로 실제 `invalid_grant`(토큰 폐기)가 발생해도 DB를 갱신하지 않아 자기 치유가 안 됐다. 이 문제는 공유파일 설계서의 `SHAREDFILE_409_2`/`SHAREDFILE_502_1` 구분을 구현하기 위한 전제이기도 했다.

### 구현 변경

- `GoogleTokenRevokedException`(신규, `GoogleOAuthCallException` 하위) — HTTP 400 + body `error=invalid_grant`일 때만 던져진다(영구 오류). 그 외(다른 400, 401/403/429/5xx, 타임아웃, 응답 파싱 실패)는 기존 `GoogleOAuthCallException`(일시 오류)으로 유지한다. `invalid_client` 같은 실제로는 영구적인 설정 오류도 이 기준상 일시적으로 취급되는 트레이드오프를 승인했다.
- `CheckGoogleAccountConnectionService`: 일시 오류는 `failed` 값을 건드리지 않고 `lastCheckedAt`만 갱신한다(`GoogleAccountConnection.markCheckAttempted` 신규).
- `GetGoogleAccessTokenService`: 실사용 중 영구 오류를 감지하면 그 자리에서 `failed=true`를 기록한다(자기 치유). `@Transactional(readOnly = true)`를 제거했다 — 영구 오류 시 쓰기가 발생하기 때문이다. 이 쓰기가 `GoogleAccountConnectionInvalidException`(런타임 예외) 전파로 인해 함께 롤백되는 걸 막기 위해 `@Transactional(noRollbackFor = GoogleAccountConnectionInvalidException.class)`도 적용했다(CodeRabbit 리뷰에서 발견).

### 코드 리뷰 반영 (CodeRabbit)

- **반영**: `save()`가 `publishEvent()`보다 먼저 호출되는지 `InOrder`로 고정하고, `save()` 실패 시 이벤트가 발행되지 않는 테스트를 추가했다.
- **반영**: `GoogleAccountConnectionInvalidException`이 `RuntimeException` 계열이라 기본 롤백 규칙상 영구 오류 감지 시의 `save()`가 함께 롤백되던 회귀를 발견해 `noRollbackFor`로 수정했다(이 프로젝트에 이미 있던 `SummarizeApprovalAttachmentService`의 동일 패턴 참고).
- **보류(중복)**: `complete()`/`check()`/`disconnect()` 공통 동시성 제어 제안은 이번 PR의 diff와 무관한 기존 코드 문제라 별도 이슈(#337)로 뺐다.

### 검증

- `GoogleOAuthAdapterTest`(신규): `HttpClientErrorException.create(...)`로 실제 HTTP 호출 없이 400(`invalid_grant`)/400(그 외)/429/파싱 실패 4케이스로 분류 로직만 단위 테스트.
- `GoogleAccountConnectionTest`, `CheckGoogleAccountConnectionServiceTest`, `GetGoogleAccessTokenServiceTest`에 영구/일시 분기별 케이스를 추가했다.
- 전체 `./gradlew test` 통과.

## ✅ 2026-08-11 · 연결 성공 이벤트(GoogleAccountConnectedEvent) 발행

### 변경 목적

공유파일 설계서·계획서는 Google 계정 연결 성공 시 `GoogleAccountConnectedEvent`를 발행해 공유파일 모듈이 AFTER_COMMIT으로 수신, 시스템 루트를 자동 생성하는 흐름을 전제로 했다. 하지만 이 이벤트와 발행 코드가 실제로는 존재하지 않았다.

### 구현 변경

- `GoogleAccountConnectedEvent(boolean accountChanged)` 신규 정의. 구독자가 아직 없어 최소 필드만 담았다(YAGNI).
- `CompleteGoogleAccountConnectionService`가 연결 저장(최초 연결/같은 계정 재연결/다른 계정 교체) 성공 직후 이 이벤트를 발행한다. `accountChanged`는 기존 연결이 있었고 그 이메일이 새 이메일과 다를 때만 `true`다.
- 이 라운드의 범위는 이벤트 **발행**까지다. 구독(공유파일 시스템 루트 자동 생성)은 공유파일 도메인 본 구현 시점의 별도 작업이다. 연동 **해제**는 이벤트를 발행하지 않는다.

### 검증

- `CompleteGoogleAccountConnectionServiceTest`에 최초 연결/동일 계정 재연결/계정 교체 3케이스를 추가해 `accountChanged` 값을 검증했다.
- 전체 `./gradlew test` 통과.
