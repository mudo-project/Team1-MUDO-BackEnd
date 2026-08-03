# 🌐 global 모듈

> 업데이트: 2026-08-03 · 시간 정책(`Clock`, JPA Auditing)과 공통 타임스탬프 Base Entity 3종을 추가했습니다.

## 📦 책임과 범위

`global`은 도메인 모듈이 아닌 공유 플랫폼 영역입니다(저장소 루트 `docs/ARCHITECTURE.md` 참조). 인증/보안(JWT, CORS), 공통 예외 응답, WebSocket 인프라, 성능 로깅, TraceId, 시간 정책처럼 도메인에 종속되지 않는 기술 공통 기능만 제공합니다.

- 도메인 비즈니스 규칙, 도메인 데이터와 상태, 도메인 소유 JPA Entity, Repository, 도메인별 Application API는 소유하지 않습니다.
- 도메인 모듈을 참조하거나 의존하지 않습니다. 도메인 모듈이 필요할 때 `global`에 의존하는 방향만 허용됩니다.

## 🙋 담당자

(팀 확인 필요)

## 🗂️ 소유하는 주요 데이터와 상태

`global`은 도메인 데이터를 소유하지 않습니다. 대신 도메인 모듈이 공통으로 상속·주입해서 쓰는 기술 컴포넌트를 제공합니다.

- ⏰ **시간 정책**: `TimeConfig` — `Clock` 빈(`Asia/Seoul` 고정), JPA Auditing용 `DateTimeProvider`
- 🧱 **공통 타임스탬프 Base Entity**: `CreatedAtEntity`, `BaseTimeEntity`, `SoftDeleteTimeEntity`
- 🔐 그 외 보안(JWT/CORS), WebSocket, 공통 예외 응답, AOP 성능 로깅, TraceId 필터

세부 목록과 사용 방법은 [API.md](API.md)를 참고해주세요.

## 🔓 외부에 공개하는 Application API

`global`은 도메인 UseCase를 공개하지 않습니다. 대신 도메인 모듈이 의존할 수 있는 공통 컴포넌트(Bean, 추상 클래스)를 공개합니다. 목록은 [API.md](API.md)를 참고해주세요.

## 🔗 다른 모듈 또는 외부 시스템에 요청하는 의존성

- 없음. `global`은 도메인 모듈에 의존하지 않습니다(저장소 루트 `docs/ARCHITECTURE.md` 원칙).

## 📣 발행·소비하는 Event

- 현재 없습니다.

## ⚠️ 변경 시 주의 사항

- 시간대 정책 전체 맥락은 저장소 루트 `docs/DATABASE.md`를 참고해주세요. 요약: **JVM 기본 시간대는 UTC로 유지**하고, 업무적으로 필요한 시각은 시스템 기본값에 의존하지 않고 `TimeConfig`의 `Clock`(`Asia/Seoul` 고정)으로 명시적으로 생성합니다.
- `CreatedAtEntity`/`BaseTimeEntity`의 `createdAt`/`updatedAt`은 JPA Auditing(`AuditingEntityListener` + `Clock` 기반 `DateTimeProvider`)이 자동으로 채웁니다. 엔티티 안에서 `LocalDateTime.now()`를 직접 호출하지 않습니다.
- `SoftDeleteTimeEntity`의 `deletedAt`은 Auditing 대상이 아니라 자동으로 채워지지 않습니다. 호출하는 서비스 계층에서 `Clock`을 주입받아 `entity.markDeleted(LocalDateTime.now(clock))` 형태로 명시적으로 호출해야 합니다.
- `global`에 도메인 비즈니스 규칙이나 도메인 소유 JPA Entity를 추가하지 않습니다(저장소 루트 `docs/ARCHITECTURE.md` 위반).

## 📚 세부 문서

- [API.md](API.md) — `global`이 제공하는 공통 컴포넌트 목록과 사용 방법
