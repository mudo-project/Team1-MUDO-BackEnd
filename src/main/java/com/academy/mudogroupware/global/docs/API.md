# 🧩 global 공통 컴포넌트

> 업데이트: 2026-08-10 · `WebSocketEventPublisher`를 추가해 도메인 알림 전송이 공통 STOMP 발행기를 거치도록 정리했고, 도메인별 삭제·정리 배치를 공통 실행하는 `GlobalRetentionScheduler`를 추가했습니다.

`global`은 도메인 모듈이 아니므로 REST 엔드포인트를 공개하지 않습니다. 대신 도메인 모듈이 의존해서 쓰는 공통 Bean/추상 클래스를 "공개 계약"으로 취급합니다. 아래 표는 API.md 표준 형식(엔드포인트/Method/권한)을 이 모듈 성격에 맞게 컴포넌트/종류/사용 방법으로 대체한 것입니다.

## ⏰ 시간 정책

| 컴포넌트 | 종류 | 사용 방법 | 기능 요약 |
| --- | --- | --- | --- |
| `TimeConfig.KOREA_ZONE` | `public static final ZoneId` | `TimeConfig.KOREA_ZONE` 직접 참조 | `Asia/Seoul` 고정 상수. 시간대 문자열을 코드 곳곳에 흩어두지 않기 위한 단일 참조 지점 |
| `Clock` | Spring Bean | 생성자/필드 주입 (`private final Clock clock`) | `Clock.system(Asia/Seoul)`. 업무 시각이 필요한 도메인/애플리케이션 코드는 `LocalDateTime.now()` 대신 `LocalDateTime.now(clock)`을 사용합니다 |
| `DateTimeProvider`(`auditingDateTimeProvider`) | Spring Bean (내부용) | 직접 주입하지 않음 — `@EnableJpaAuditing`이 내부적으로 사용 | JPA Auditing(`@CreatedDate`/`@LastModifiedDate`)이 시스템 기본 시간대가 아니라 위 `Clock`(`Asia/Seoul`) 기준으로 시각을 채우게 합니다 |

세부 명세: [TimeConfig.java](../infrastructure/config/TimeConfig.java)

---

## 📄 공용 페이지네이션

| 컴포넌트 | 종류 | 사용 방법 | 기능 요약 |
| --- | --- | --- | --- |
| `PageResult<T>` | record (`global.domain.common.page`) | 도메인 Repository 인터페이스가 반환, Application 계층에서 `.map()`으로 View 타입 변환 | `content`/`page`/`size`/`hasNext`만 담는 프레임워크-비의존 페이지 결과. Spring Data `Pageable`/`Slice`를 domain 계층에 노출하지 않기 위한 래퍼 |
| `SliceResponse<T>` | record (`global.presentation.api.common`) | Controller에서 `SliceResponse.from(pageResult, ResponseDto::from)`로 생성, `GlobalApiResponse<SliceResponse<T>>`로 감싸 반환 | `docs/API_CONTRACT.md` 페이지네이션 규칙(`content`/`page`/`size`/`hasNext`)을 만족하는 응답 포맷 |

세부 명세: [PageResult.java](../domain/common/page/PageResult.java) · [SliceResponse.java](../presentation/api/common/SliceResponse.java)

---

## 🧱 공통 타임스탬프 Base Entity

| 컴포넌트 | 종류 | 사용 방법 | 기능 요약 |
| --- | --- | --- | --- |
| `CreatedAtEntity` | `@MappedSuperclass` 추상 클래스 | 도메인 JPA Entity가 상속 | `createdAt` 1개만 필요한 엔티티용. `@CreatedDate`로 생성 시점에 자동 채워지며, 이후 수정 불가(`updatable = false`) |
| `BaseTimeEntity` | `@MappedSuperclass` 추상 클래스 (`CreatedAtEntity` 상속) | 도메인 JPA Entity가 상속 | `createdAt` + `updatedAt`. `@LastModifiedDate`로 수정 시마다 자동 갱신 |
| `SoftDeleteTimeEntity` | `@MappedSuperclass` 추상 클래스 (`BaseTimeEntity` 상속) | 도메인 JPA Entity가 상속, 삭제 시 `markDeleted(LocalDateTime.now(clock))` 명시적 호출 | `createdAt` + `updatedAt` + `deletedAt`(소프트 삭제). `deletedAt`은 Auditing 대상이 아니므로 호출부에서 시각을 직접 넘겨야 함. `isDeleted()`로 삭제 여부 조회 |

세부 명세: [CreatedAtEntity.java](../infrastructure/persistence/CreatedAtEntity.java) · [BaseTimeEntity.java](../infrastructure/persistence/BaseTimeEntity.java) · [SoftDeleteTimeEntity.java](../infrastructure/persistence/SoftDeleteTimeEntity.java)

---

## WebSocket 공통 발행기

| 컴포넌트 | 종류 | 사용 방법 | 기능 요약 |
| --- | --- | --- | --- |
| `WebSocketConfig` | Spring WebSocket 설정 | 내부 설정. 프론트는 `/ws` STOMP endpoint에 연결 | STOMP endpoint, broker prefix(`/topic`, `/queue`), application prefix(`/app`), user prefix(`/user`)를 설정 |
| `WebSocketEventPublisher` | Spring Bean | 도메인 Notifier에서 주입 후 `publish(destination, payload)` 호출 | `SimpMessagingTemplate` 직접 사용을 `global`로 모은 공통 발행기. `/topic/`, `/queue/` 목적지만 허용하고 payload null을 차단 |

세부 명세: [WebSocketConfig.java](../infrastructure/config/WebSocketConfig.java) · [WebSocketEventPublisher.java](../infrastructure/websocket/WebSocketEventPublisher.java)

---

## 🧹 삭제·정리 배치 공통 스케줄러

| 컴포넌트 | 종류 | 사용 방법 | 기능 요약 |
| --- | --- | --- | --- |
| `RetentionJob` | 인터페이스 | 도메인이 구현체를 만들어 `@Component`로 등록하면 자동 수집됨 | `name()`, `run(LocalDateTime now)` 두 메서드만 계약. Global Scheduler 수정 없이 새 도메인 Job 추가 가능 |
| `RetentionJobResult` | record | 도메인 Job이 반환 | `jobName`/`candidateCount`/`deletedChildCount`/`deletedParentCount`. 자식 테이블이 없으면 `deletedChildCount=0` |
| `GlobalRetentionScheduler` | Spring Bean, `@Scheduled` | 직접 호출하지 않음 — 매일 03:00(KST) 자동 실행 | 등록된 `List<RetentionJob>`을 순회 실행. 한 Job이 실패해도 나머지 Job은 계속 실행(`try/catch` 격리) |

Cron/시간대는 `app.scheduler.retention.cron`/`app.scheduler.retention.zone` 프로퍼티(환경변수 `APP_SCHEDULER_RETENTION_CRON`/`APP_SCHEDULER_RETENTION_ZONE`)로 재정의할 수 있습니다. 보관 기간·배치 크기는 비밀값이 아니므로 각 도메인의 `{Domain}RetentionProperties` 기본값으로 관리합니다. 설계 배경과 새 도메인 추가 절차는 [BOILER_PLATE.md](BOILER_PLATE.md), 실제 구현 예시는 `student/application/retention/*`를 참고해주세요.

세부 명세: [RetentionJob.java](../scheduler/RetentionJob.java) · [RetentionJobResult.java](../scheduler/RetentionJobResult.java) · [GlobalRetentionScheduler.java](../scheduler/GlobalRetentionScheduler.java)

---

## 💡 사용 시 주의 사항

- 세 클래스 모두 `@Getter`만 열어두고 필드에 직접 값을 대입하는 setter는 두지 않았습니다. `createdAt`/`updatedAt`은 Auditing이 채우고, `deletedAt`만 `markDeleted()`로 명시적으로 채웁니다.
- `nullable = false` 컬럼(`created_at`, `updated_at`)을 상속하는 도메인 엔티티는 대응하는 DB 컬럼도 `NOT NULL`로 마이그레이션해야 합니다.
- 도메인 엔티티에서 `LocalDateTime.now()`를 직접 호출하지 않습니다. 필요하면 이 문서의 `Clock` 빈을 주입받아 사용해주세요.
- `markDeleted(null)`을 호출하면 예외(`NullPointerException`)가 발생합니다. 이미 삭제된 엔티티에 다시 `markDeleted()`를 호출하면 `IllegalStateException`이 발생하며, 기존 `deletedAt`은 덮어써지지 않습니다. 삭제를 되돌려야 하면 `markDeleted()`를 재사용하지 말고 별도의 `restore()` 메서드를 도메인 엔티티에 명시적으로 추가해주세요.
- **소프트 삭제 조회 필터 정책**: `SoftDeleteTimeEntity`는 조회 쿼리를 자동으로 걸러주지 않습니다(`@Where`, `@SQLRestriction` 등을 적용하지 않음). `SoftDeleteTimeEntity`를 상속하는 도메인 엔티티의 Repository/QueryDSL 조회 조건에는 `deleted_at IS NULL`(또는 이에 대응하는 조건)을 **직접 추가**해야 합니다. 누락하면 삭제된 데이터가 목록/상세 조회에 그대로 노출됩니다.
- **페이지네이션**: 목록 API는 전체 개수(`totalElements`/`totalPages`)가 필요 없다면 `Page` 대신 `Slice`를 우선 고려하세요(추가 COUNT 쿼리를 생략). 도메인 Repository 인터페이스는 Spring Data의 `Pageable`/`Slice`를 직접 노출하지 말고, Infrastructure 계층에서 `PageResult`로 변환해 반환하세요.
- **WebSocket 알림**: 도메인 Notifier는 목적지 문자열과 payload만 정하고 실제 전송은 `WebSocketEventPublisher.publish(...)`에 위임합니다. 도메인 코드에서 `SimpMessagingTemplate`을 직접 주입하지 않습니다.
- **WebSocket 목적지**: 브로드캐스트는 `/topic/...`, 큐성 알림은 `/queue/...`로만 발행합니다. 클라이언트가 서버로 보내는 `/app/...` 목적지는 서버 발행 목적지로 사용하지 않습니다.

## 📝 문서 정보

- 업데이트일: `2026-08-10`
- 변경 사항(요약):
  - `WebSocketEventPublisher`를 추가해 결재/메신저 실시간 알림 전송 경로를 공통화했습니다.
  - `Clock` 빈과 JPA Auditing `DateTimeProvider`를 추가했습니다. ⏰
  - `CreatedAtEntity` / `BaseTimeEntity` / `SoftDeleteTimeEntity` 3종 Base Entity를 추가했습니다. 🧱
  - `markDeleted()`의 null 방어 및 중복 삭제 방지 로직을 추가했습니다. 🛡️
  - 소프트 삭제 조회 필터 정책(Repository/QueryDSL에서 `deleted_at IS NULL` 직접 처리)을 문서화했습니다. 🗄️
  - `PageResult<T>`/`SliceResponse<T>` 공용 페이지네이션 컴포넌트를 추가했습니다. 📄
