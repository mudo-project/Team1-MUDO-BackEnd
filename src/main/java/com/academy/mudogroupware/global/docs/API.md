# 🧩 global 공통 컴포넌트

> 업데이트: 2026-08-04 · `SoftDeleteTimeEntity.markDeleted()`의 null/중복 삭제 방어 로직과 소프트 삭제 조회 필터 정책을 추가했습니다.

`global`은 도메인 모듈이 아니므로 REST 엔드포인트를 공개하지 않습니다. 대신 도메인 모듈이 의존해서 쓰는 공통 Bean/추상 클래스를 "공개 계약"으로 취급합니다. 아래 표는 API.md 표준 형식(엔드포인트/Method/권한)을 이 모듈 성격에 맞게 컴포넌트/종류/사용 방법으로 대체한 것입니다.

## ⏰ 시간 정책

| 컴포넌트 | 종류 | 사용 방법 | 기능 요약 |
| --- | --- | --- | --- |
| `TimeConfig.KOREA_ZONE` | `public static final ZoneId` | `TimeConfig.KOREA_ZONE` 직접 참조 | `Asia/Seoul` 고정 상수. 시간대 문자열을 코드 곳곳에 흩어두지 않기 위한 단일 참조 지점 |
| `Clock` | Spring Bean | 생성자/필드 주입 (`private final Clock clock`) | `Clock.system(Asia/Seoul)`. 업무 시각이 필요한 도메인/애플리케이션 코드는 `LocalDateTime.now()` 대신 `LocalDateTime.now(clock)`을 사용합니다 |
| `DateTimeProvider`(`auditingDateTimeProvider`) | Spring Bean (내부용) | 직접 주입하지 않음 — `@EnableJpaAuditing`이 내부적으로 사용 | JPA Auditing(`@CreatedDate`/`@LastModifiedDate`)이 시스템 기본 시간대가 아니라 위 `Clock`(`Asia/Seoul`) 기준으로 시각을 채우게 합니다 |

세부 명세: [TimeConfig.java](../infrastructure/config/TimeConfig.java)

---

## 🧱 공통 타임스탬프 Base Entity

| 컴포넌트 | 종류 | 사용 방법 | 기능 요약 |
| --- | --- | --- | --- |
| `CreatedAtEntity` | `@MappedSuperclass` 추상 클래스 | 도메인 JPA Entity가 상속 | `createdAt` 1개만 필요한 엔티티용. `@CreatedDate`로 생성 시점에 자동 채워지며, 이후 수정 불가(`updatable = false`) |
| `BaseTimeEntity` | `@MappedSuperclass` 추상 클래스 (`CreatedAtEntity` 상속) | 도메인 JPA Entity가 상속 | `createdAt` + `updatedAt`. `@LastModifiedDate`로 수정 시마다 자동 갱신 |
| `SoftDeleteTimeEntity` | `@MappedSuperclass` 추상 클래스 (`BaseTimeEntity` 상속) | 도메인 JPA Entity가 상속, 삭제 시 `markDeleted(LocalDateTime.now(clock))` 명시적 호출 | `createdAt` + `updatedAt` + `deletedAt`(소프트 삭제). `deletedAt`은 Auditing 대상이 아니므로 호출부에서 시각을 직접 넘겨야 함. `isDeleted()`로 삭제 여부 조회 |

세부 명세: [CreatedAtEntity.java](../infrastructure/persistence/CreatedAtEntity.java) · [BaseTimeEntity.java](../infrastructure/persistence/BaseTimeEntity.java) · [SoftDeleteTimeEntity.java](../infrastructure/persistence/SoftDeleteTimeEntity.java)

---

## 💡 사용 시 주의 사항

- 세 클래스 모두 `@Getter`만 열어두고 필드에 직접 값을 대입하는 setter는 두지 않았습니다. `createdAt`/`updatedAt`은 Auditing이 채우고, `deletedAt`만 `markDeleted()`로 명시적으로 채웁니다.
- `nullable = false` 컬럼(`created_at`, `updated_at`)을 상속하는 도메인 엔티티는 대응하는 DB 컬럼도 `NOT NULL`로 마이그레이션해야 합니다.
- 도메인 엔티티에서 `LocalDateTime.now()`를 직접 호출하지 않습니다. 필요하면 이 문서의 `Clock` 빈을 주입받아 사용해주세요.
- `markDeleted(null)`을 호출하면 예외(`NullPointerException`)가 발생합니다. 이미 삭제된 엔티티에 다시 `markDeleted()`를 호출하면 `IllegalStateException`이 발생하며, 기존 `deletedAt`은 덮어써지지 않습니다. 삭제를 되돌려야 하면 `markDeleted()`를 재사용하지 말고 별도의 `restore()` 메서드를 도메인 엔티티에 명시적으로 추가해주세요.
- **소프트 삭제 조회 필터 정책**: `SoftDeleteTimeEntity`는 조회 쿼리를 자동으로 걸러주지 않습니다(`@Where`, `@SQLRestriction` 등을 적용하지 않음). `SoftDeleteTimeEntity`를 상속하는 도메인 엔티티의 Repository/QueryDSL 조회 조건에는 `deleted_at IS NULL`(또는 이에 대응하는 조건)을 **직접 추가**해야 합니다. 누락하면 삭제된 데이터가 목록/상세 조회에 그대로 노출됩니다.

## 📝 문서 정보

- 업데이트일: `2026-08-04`
- 변경 사항(요약):
  - `Clock` 빈과 JPA Auditing `DateTimeProvider`를 추가했습니다. ⏰
  - `CreatedAtEntity` / `BaseTimeEntity` / `SoftDeleteTimeEntity` 3종 Base Entity를 추가했습니다. 🧱
  - `markDeleted()`의 null 방어 및 중복 삭제 방지 로직을 추가했습니다. 🛡️
  - 소프트 삭제 조회 필터 정책(Repository/QueryDSL에서 `deleted_at IS NULL` 직접 처리)을 문서화했습니다. 🗄️
